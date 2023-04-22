package shpp.level2;

import org.apache.activemq.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.MessageStream;
import shpp.level2.util.ConnectionMQ;

import javax.jms.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);

    private final BlockingQueue<TextMessage> messageQueue;

    private final ConnectionMQ connectionMQ;


    private final StopWatch timer = new StopWatch();

    private final AtomicInteger sendMessageCounter = new AtomicInteger(0);

    private final MessageStream messageStream;

    private boolean isInit = false;


    private final int threads;

    private final CountDownLatch latch;


    public Producer(ConnectionMQ connectionMQ, BlockingQueue<TextMessage> queue, MessageStream messageStream, int threads)  {
        this.connectionMQ = connectionMQ;
        this.messageQueue = queue;
        this.messageStream = messageStream;
        this.threads = threads;
        this.latch = new CountDownLatch(threads);

    }

    public void start() throws JMSException {

        isInit = true;
        timer.restart();
        logger.debug("Starting MessageStream thread.");
        new Thread(messageStream).start();
        logger.debug("Starting Producer threads.");
        for (int i = 0; i < threads; i++){
            new Thread(this).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        connectionMQ.closeConnection();

        logger.debug("Done! Send messages number = {}", sendMessageCounter);
        logger.debug("Time execution = {} ms", timer.taken());
        logger.info("Send messages rps={}", (sendMessageCounter.doubleValue() / timer.taken()) * 1000);
    }

    @Override
    public void run() {
        if(!isInit){
            init();
        }else{
            logger.debug("Producer thread starting.");
            Session session = connectionMQ.createSession();

            logger.debug("Creating MessageProducer.");
            if(session != null){
                Queue queue = connectionMQ.createQueue(session);
                if(queue != null){
                    try {
                        MessageProducer  producer = session.createProducer(queue);
                        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                        logger.debug("MessageProducer created");

                        sendMessage(producer);
                        producer.send( session.createTextMessage("END"));
                        logger.debug("MessageProducer send Poison Pill");
                        producer.close();
                        logger.debug("MessageProducer closed.");
                        session.close();
                        logger.debug("MessageProducer session closed.");

                    } catch (JMSException e) {
                        logger.error("Can't create Producer or send message.", e);
                    } catch (InterruptedException e) {
                        logger.error("Producer send process interrupted");
                        Thread.currentThread().interrupt();
                    }

                    latch.countDown();
                }
            }

        }
    }

    private void init() {
        try {
            logger.debug("Init Producer.");
            start();
        } catch (JMSException e) {
            logger.error("Can't init Producer threads.");
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage(MessageProducer producer) throws InterruptedException, JMSException {
        int localCounter = 0;
        logger.debug("Start reading and sending messages.");
        while(messageStream.isRunning()) {
            while (!messageQueue.isEmpty()){
                    producer.send(messageQueue.take());
                    sendMessageCounter.incrementAndGet();
                    localCounter++;
            }
        }
        logger.debug("This thread done! Send messages number = {}", localCounter);
    }
}
