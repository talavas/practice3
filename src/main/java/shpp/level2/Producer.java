package shpp.level2;

import org.apache.activemq.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.MessageStream;
import shpp.level2.util.Config;
import shpp.level2.util.ConnectionMQ;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);

    private final BlockingQueue<TextMessage> messageQueue;

    private final Config config;


    private final StopWatch timer = new StopWatch();

    private final AtomicInteger sendMessageCounter = new AtomicInteger(0);

    private final MessageStream messageStream;

    private boolean isInit = false;


    private final int threads;

    private final CountDownLatch latch;

    ExecutorService executorService;


    public Producer(Config config, BlockingQueue<TextMessage> queue, MessageStream messageStream, int threads)  {
        this.config = config;
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

    public void start() throws JMSException {
        isInit = true;
        timer.restart();

        logger.debug("Starting Producer Pool.");

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

        logger.debug("Done! Send messages number = {}", sendMessageCounter);
        logger.debug("Time execution = {} ms", timer.taken());
        logger.info("Send messages rps={}", (sendMessageCounter.doubleValue() / timer.taken()) * 1000);
    }

    @Override
    public void run() {
            logger.debug("Producer thread starting.");
            try {
                sendMessage();
            } catch (InterruptedException e) {
                logger.error("Producer send process interrupted");
                Thread.currentThread().interrupt();
            } catch (JMSException e) {
                logger.error("Can't send messages.", e);
            }finally {
                try {
                    sendPoissonPill();
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
                logger.debug("Latch countDown {}", latch.getCount());
                latch.countDown();
                }

    }

    private void sendMessage() throws InterruptedException, JMSException {
        int localCounter = 0;
        logger.debug("Start reading and sending messages.");
        List<TextMessage> batch = new ArrayList<>();

        while(messageStream.isRunning() || !messageQueue.isEmpty()) {

            int count = messageQueue.drainTo(batch, 1000);
            if (count == 0) {
                Thread.sleep(1); // Затримуємо потік на 1 мілісекунду, щоб не займати зайвої процесорної потужності.
                continue;
            }
                logger.debug("Send batch = {}", batch.size());
                sendBatch(new ArrayList<>(batch));
                localCounter += batch.size();
                batch.clear();
        }
        if (!batch.isEmpty()) {
            logger.debug("Send last batch = {}", batch.size());
            sendBatch(new ArrayList<>(batch));
            sendMessageCounter.addAndGet(batch.size());
            localCounter += batch.size();
            batch.clear();
        }

        logger.debug("This thread done! Send messages number = {}", localCounter);
    }
    private void sendBatch(List<TextMessage> batch) throws JMSException {
        ConnectionMQ connectionMQ = new ConnectionMQ(config);
        Session session = connectionMQ.createSession();
        Queue queue = connectionMQ.createQueue(session);

        try {
            MessageProducer producer = session.createProducer(queue);
            batch.forEach(message -> {
                try {
                    producer.send(message);
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            });
            sendMessageCounter.addAndGet(batch.size());
            logger.debug("Batch sent {} messages", batch.size());
            logger.debug("Closing producer.");
            producer.close();
            logger.debug("Closing session.");
            session.close();
            logger.debug("Closing connection.");
            connectionMQ.closeConnection();

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendPoissonPill() throws JMSException {
        ConnectionMQ connectionMQ = new ConnectionMQ(config);
        Session session = connectionMQ.createSession();
        Queue queue = connectionMQ.createQueue(session);
        MessageProducer  producer = session.createProducer(queue);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        logger.debug("MessageProducer created");
        producer.send( session.createTextMessage("END"));
        logger.debug("MessageProducer send Poison Pill");
        logger.debug("Closing producer.");
        producer.close();
        logger.debug("Closing session.");
        session.close();
        logger.debug("Closing connection.");
        connectionMQ.closeConnection();
    }
}
