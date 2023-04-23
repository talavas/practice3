package shpp.level2;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.activemq.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.MessagePojo;
import shpp.level2.message.MessagePojoGenerator;
import shpp.level2.util.ConnectionMQ;

import javax.jms.*;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    private ConnectionMQ connectionMQ;
    private final StopWatch timer = new StopWatch();
    private int threads;

    private AtomicInteger receivedMessageCounter = new AtomicInteger(0);

    public BlockingQueue<MessagePojo> getMessageQueue() {
        return messageQueue;
    }

    private BlockingQueue<MessagePojo> messageQueue;

    public boolean isRunning = true;


private boolean isInit = false;
    CountDownLatch latch;
    public Consumer(ConnectionMQ connectionMQ, BlockingQueue<MessagePojo> queue, int threads) {
        this.connectionMQ = connectionMQ;
        this.messageQueue = queue;
        this.latch = new CountDownLatch(threads);
        this.threads = threads;
    }

    public void start() throws JMSException {
        isInit = true;
        timer.restart();

        for (int i = 0; i < threads; i++) {
            new Thread(this).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        isRunning = false;
        logger.debug("Consumer stop receiving messages");

        connectionMQ.closeConnection();
        logger.debug("ActiveMQ connection closed.");

        logger.debug("Done! Received messages number = {}", receivedMessageCounter.get());
        logger.debug("Time execution = {} ms", timer.taken());
        logger.info("Received messages rps={}", (receivedMessageCounter.doubleValue() / timer.taken()) * 1000);
    }

    @Override
    public void run() {
        if(!isInit){
           init();
        }else{
            try {
                logger.debug("Consumer thread starting.");
                Session session = connectionMQ.createSession();

                if(session != null){

                    logger.debug("Creating MessageConsumer.");
                    Queue queue =connectionMQ.createQueue(session);

                    if(queue != null){

                        MessageConsumer consumer = session.createConsumer(queue);
                        logger.debug("MessageConsumer created.");

                        receiveMessages(consumer);

                        consumer.close();
                        logger.debug("MessageConsumer close");

                        session.close();
                        logger.debug("MessageConsumer session close");
                    }
                }

            } catch (JMSException e) {
                logger.error("Can't create MessageConsumer or receive message.", e);
            }
            latch.countDown();
        }
    }

    private void receiveMessages(MessageConsumer consumer) throws JMSException{
        Message message;
        String text;

        while (true) {
            message = consumer.receive();
            MessagePojo messagePojo;
            if (message != null) {
                if (message instanceof TextMessage) {
                    text = ((TextMessage) message).getText();
                    if(text != null && text.equals("END")){
                        logger.debug("Receive poisson pill");
                        break;
                    }
                    try {
                        messagePojo = MessagePojoGenerator.toMessagePojo(text);
                        messageQueue.add(messagePojo);
                        receivedMessageCounter.incrementAndGet();
                    } catch (JsonProcessingException e) {
                        logger.error("Received message {} can't convert to MessagePojo class", text, e);
                    }
                }
            }
        }
    }

    private void init() {
        try {
            logger.debug("Init Consumer.");
            start();
        } catch (JMSException e) {
            logger.error("Can't init Consumer threads.");
            Thread.currentThread().interrupt();
        }
    }
}
