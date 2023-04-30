package shpp.level2.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.activemq.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.util.Config;
import shpp.level2.util.ConnectionMQ;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class MessageStream implements Runnable{

    private final Logger logger = LoggerFactory.getLogger(MessageStream.class);

    private final long maxMessageCounter;

    BlockingQueue<TextMessage> messageQueue;

    private final int maxDuration;

    public boolean isRunning() {
        return isRunning;
    }

    private boolean isRunning = true;

    StopWatch timer = new StopWatch();

    ConnectionMQ connectionMQ;

    AtomicInteger counter = new AtomicInteger(0);

    public MessageStream(ConnectionMQ connectionMQ, Config config, BlockingQueue<TextMessage> queue, long maxMessageCounter)  {
        this.messageQueue =queue;
        this.maxMessageCounter = maxMessageCounter;
        this.maxDuration = Integer.parseInt(config.getProperty("stop.time"));
        this.connectionMQ = connectionMQ;

    }

    @Override
    public void run() {
        logger.info("Starting generate messages.");
        Session session = connectionMQ.createSession();
        if(session != null){
            generateStream(session);
            isRunning = false;
            try {
                session.close();
                logger.debug("MessageStream session close.");
            } catch (JMSException e) {
                logger.error("JMS service issue:", e);
            }
            logger.info("Generated messages number = {}", counter);
            logger.info("Time execution = {} ms", timer.taken());
            logger.info("Generate messages rps={}", (counter.doubleValue() / timer.taken()) * 1000);
        }else{
            logger.error("Can't create session.");
        }

    }

    void generateStream(Session session) {
        Stream.generate(MessagePojoGenerator::generateMessage)
                .takeWhile(message ->
                        timer.taken() / 1000 < maxDuration &&
                                counter.get() < maxMessageCounter
                )
                .forEach(message -> {
                    try {
                        TextMessage textMessage = session.createTextMessage(MessagePojoGenerator.toJson(message));
                        messageQueue.add(textMessage);
                        counter.incrementAndGet();
                    } catch (JsonProcessingException e) {
                        logger.error("Can't proceed json parsing", e);
                        Thread.currentThread().interrupt();
                    } catch (JMSException e) {
                        logger.error("Can't create JMS message");
                        Thread.currentThread().interrupt();
                    }
                });
    }
}
