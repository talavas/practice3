package shpp.level2;

import org.apache.activemq.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.MessageStream;
import shpp.level2.util.Config;
import shpp.level2.util.ConnectionMQ;

import javax.jms.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);

    BlockingQueue<TextMessage> messageQueue;


    volatile StopWatch time = new StopWatch();

    AtomicInteger sendMessageCounter = new AtomicInteger(0);

    MessageStream messageStream;


    int threads;

    CountDownLatch latch;

    Config config;

    public Producer( Config config, BlockingQueue<TextMessage> queue, MessageStream messageStream, int threads) throws JMSException {
        this.config = config;
        this.messageQueue = queue;
        this.messageStream = messageStream;
        this.threads = threads;
        this.latch = new CountDownLatch(threads);

    }


    public void start() throws JMSException {
        time.restart();
        new Thread(messageStream).start();
        for (int i = 0; i < threads; i++){
            new Thread(this).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.debug("Done! Send messages number = {}", sendMessageCounter);
        logger.debug("Time execution = {} ms", time.taken());
        logger.info("Send messages rps={}", (sendMessageCounter.doubleValue() / time.taken()) * 1000);
    }

    @Override
    public void run() {
        ConnectionMQ connectionMQ;
        try {
            connectionMQ = new ConnectionMQ(config);
            logger.debug("Create connection to ActiveMQ.");
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        if(connectionMQ.getSession() != null){
            logger.debug("Create Producer.");
            try {
                MessageProducer  producer = connectionMQ.getSession().createProducer(connectionMQ.getQueue());
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                sendMessage(producer, connectionMQ);
                logger.debug("Producer created");
            } catch (JMSException e) {
                logger.error("Can't create Producer.", e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            connectionMQ.closeConnection();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
        latch.countDown();
    }

    private void sendMessage(MessageProducer producer, ConnectionMQ connectionMQ) throws InterruptedException, JMSException {
        int localCounter = 0;
        logger.debug("Start reading and sending messages.");
        while(messageStream.isRunning()) {
            while (!messageQueue.isEmpty()){
                    producer.send(messageQueue.take());
                    sendMessageCounter.incrementAndGet();
                    localCounter++;
            }
        }
        connectionMQ.getSession().createTextMessage("END");
        logger.debug("This thread done! Send messages number = {}", localCounter);
    }
}
