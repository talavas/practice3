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
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class MessageStream implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(MessageStream.class);

    private long maxMessageCounter;

    BlockingQueue<TextMessage> messageQueue;

    private int maxDuration;

    public boolean isRunning() {
        return isRunning;
    }

    private boolean isRunning = true;

    StopWatch timer = new StopWatch();

    ConnectionMQ connectionMQ;

    AtomicInteger counter = new AtomicInteger(-1);

    public MessageStream(ConnectionMQ connectionMQ, Config config, BlockingQueue<TextMessage> queue, long maxMessageCounter)  {
        this.messageQueue =queue;
        this.maxMessageCounter = maxMessageCounter;
        this.maxDuration = Integer.parseInt(config.getProperty("stop.time"));
        this.connectionMQ = connectionMQ;

    }

    @Override
    public void run() {
        logger.info("Starting generate messages.");
        Optional<Session> session = Optional.of(connectionMQ.createSession());
        session.ifPresent(sessionInstance -> Stream.generate(MessagePojoGenerator::generateMessage)
                .takeWhile(message ->
                        timer.taken() / 1000 < maxDuration &&
                                counter.incrementAndGet() < maxMessageCounter
                )
                .forEach(message -> {
                    try {
                        TextMessage textMessage = sessionInstance.createTextMessage(MessagePojoGenerator.toJson(message));
                        messageQueue.add(textMessage);
                    } catch (JsonProcessingException e) {
                        logger.error("Can't proceed jsom parsing", e);
                    } catch (JMSException e) {
                        logger.error("Can't create JMS message");
                    }
                })
        );

        isRunning = false;
        try {
            session.get().close();
            logger.debug("MessageStream session close.");
        } catch (JMSException e) {
            logger.error("JMS service issue:", e);
        }
        logger.debug("Generated messages number = {}", counter);
        logger.debug("Time execution = {} ms", timer.taken());
        logger.debug("Generate messages rps={}", (counter.doubleValue() / timer.taken()) * 1000);
    }
}
