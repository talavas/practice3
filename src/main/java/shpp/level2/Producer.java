package shpp.level2;

import org.apache.activemq.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.MessageStream;
import shpp.level2.util.ConnectionMQ;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private final BlockingQueue<TextMessage> messageQueue;
    private final ConnectionMQ connectionMQ;
    private final StopWatch timer = new StopWatch();

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    private int batchSize = 100;
    private final AtomicInteger sendMessageCounter = new AtomicInteger(0);
    private final MessageStream messageStream;
    private boolean isInit = false;
    private final int threads;
    private final CountDownLatch latch;
    private final ExecutorService executorService;

    public void setPoisonPill(String poisonPill) {
        this.poisonPill = poisonPill;
    }

    private String poisonPill;


    public Producer(ConnectionMQ connectionMQ, BlockingQueue<TextMessage> queue, MessageStream messageStream, int threads)  {
        this.connectionMQ = connectionMQ;
        this.messageQueue = queue;
        this.messageStream = messageStream;
        this.threads = threads;
        this.latch = new CountDownLatch(threads);
        this.executorService = new ThreadPoolExecutor(threads, threads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void start() throws JMSException, InterruptedException {
        isInit = true;
        timer.restart();
        logger.debug("Starting MessageStream thread.");
        executorService.execute(messageStream);
        Thread.sleep(1000);
        logger.debug("Starting Producer threads.");

        for (int i = 0; i < threads; i++) {
            executorService.execute(this);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }finally {
            executorService.shutdown();
        }

        connectionMQ.closeConnection();
        logger.debug("Producer ActiveMQ connection closed.");

        logger.info("Done! Send messages number = {}", sendMessageCounter);
        logger.info("Time execution = {} ms", timer.taken());
        logger.info("Send messages rps={}", (sendMessageCounter.doubleValue() / timer.taken()) * 1000);
    }

    @Override
    public void run() {
        if(!isInit){
            init();
        }else{
            logger.debug("Thread {}: Creating session.", Thread.currentThread().getName());
            Session session = connectionMQ.createSession();

            if(session != null){
                Queue queue = connectionMQ.createQueue(session);
                if(queue != null){
                    try {
                        logger.debug("Thread {}: Creating MessageProducer.", Thread.currentThread().getName());
                        MessageProducer  producer = session.createProducer(queue);
                        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                        sendMessage(producer, session);

                    } catch (JMSException e) {
                        logger.error("JMS exception.", e);
                    }
                    latch.countDown();
                }
            }
        }
    }

    private void init() {
        try {
            logger.debug("Init Producer's Thread Pool.");
            start();
        } catch (JMSException e) {
            logger.error("Can't init Producer Thread Pool.", e);
            Thread.currentThread().interrupt();
        } catch (InterruptedException e) {
            logger.error("Producer Thread Pool interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage(MessageProducer producer, Session session) throws JMSException {
        int localCounter = 0;

        while(messageStream.isRunning() || !messageQueue.isEmpty()) {
            List<TextMessage> batch = new ArrayList<>();
            int count = messageQueue.drainTo(batch, batchSize);
            if (count == 0) {
                continue;
            }
                logger.debug("Sending messages batch = {}", batch.size());
                sendBatch(producer, batch);
                localCounter += batch.size();
        }
        sendPoissonPill();
        logger.debug("MessageProducer send Poison Pill");

        logger.debug("Thread {}: Closing MessageProducer and Session.", Thread.currentThread().getName());
        producer.close();
        session.close();

        logger.debug("Thread {} done! Send messages number = {}", Thread.currentThread().getName(), localCounter);
    }

    private void sendPoissonPill()  {
        Session session = connectionMQ.createSession();
        if(session != null) {
            Queue queue = connectionMQ.createQueue(session);
            if (queue != null) {
                try {
                    MessageProducer  producer = session.createProducer(queue);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    producer.setPriority(0);
                    producer.send(session.createTextMessage(poisonPill));
                    session.close();
                    producer.close();
                } catch (JMSException e) {
                    logger.error("Can't send poisson pill");
                }
            }
        }

    }

    private void sendBatch(MessageProducer producer, List<TextMessage> batch) {
        batch.stream().forEach(message -> {
            try {
                producer.send(message);
                sendMessageCounter.incrementAndGet();
            } catch (JMSException e) {
                logger.error("Can't send message = {}", message);
            }
        });
    }
}
