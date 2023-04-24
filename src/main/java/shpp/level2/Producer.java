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

    private final AtomicInteger threadCounter = new AtomicInteger(0);

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
        this.executorService = new ThreadPoolExecutor(10, 100,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public void start() throws JMSException {
        isInit = true;
        timer.restart();

        logger.debug("Starting Producer Pool.");
        executorService.execute(this);

        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        sendPoissonPill();
        logger.info("Done! Send messages number = {}", sendMessageCounter);
        logger.info("Time execution = {} ms", timer.taken());
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
            }

    }

    private void sendMessage() throws InterruptedException, JMSException {
        logger.debug("Start reading and sending messages.");
        List<TextMessage> batch = null;

        while(messageStream.isRunning() || !messageQueue.isEmpty()) {
            batch = new ArrayList<>();
            int count = messageQueue.drainTo(batch, 100);
            if (count == 0) {
                Thread.sleep(1); // Затримуємо потік на 1 мілісекунду, щоб не займати зайвої процесорної потужності.
                continue;
            }
                logger.info("Prepared batch = {}", batch.size());
                sendBatch(new ArrayList<>(batch));
                batch.clear();
        }
        if (batch != null && !batch.isEmpty()) {
            logger.debug("Send last batch = {}", batch.size());
            sendBatch(new ArrayList<>(batch));
            batch.clear();
        }

        while (threadCounter.get() > 0){
            Thread.sleep(1);
        }
        logger.info("Executor Service shutDown.");
        executorService.shutdown();

    }
    private void sendBatch(List<TextMessage> batch) throws JMSException {
        threadCounter.incrementAndGet();
        executorService.submit(() -> {
            logger.info("New thread created {}", threadCounter.get());
            try {
                ConnectionMQ connectionMQ = new ConnectionMQ(config);
                Session session = connectionMQ.createSession();
                Queue queue = connectionMQ.createQueue(session);
                MessageProducer producer = session.createProducer(queue);
                batch.forEach(message -> {
                    try {
                        producer.send(message);
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                });
                sendMessageCounter.addAndGet(batch.size());
                threadCounter.decrementAndGet();
                logger.info("Thread deleted");
                logger.info("Batch sent {} messages", batch.size());
                logger.debug("Closing producer.");
                producer.close();
                logger.debug("Closing session.");
                session.close();
                logger.debug("Closing connection.");
                connectionMQ.closeConnection();

            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sendPoissonPill() throws JMSException {
        ConnectionMQ connectionMQ = new ConnectionMQ(config);
        Session session = connectionMQ.createSession();
        Queue queue = connectionMQ.createQueue(session);
        MessageProducer  producer = session.createProducer(queue);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        logger.debug("MessageProducer created");
        producer.send( session.createTextMessage("END"));
        logger.info("MessageProducer send Poison Pill");
        logger.debug("Closing producer.");
        producer.close();
        logger.debug("Closing session.");
        session.close();
        logger.debug("Closing connection.");
        connectionMQ.closeConnection();
    }
}
