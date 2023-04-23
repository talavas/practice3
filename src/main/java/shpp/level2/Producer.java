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

    private final AtomicInteger sendMessageCounter = new AtomicInteger(0);

    private final MessageStream messageStream;

    private boolean isInit = false;


    private final int threads;

    private final CountDownLatch latch;

    ExecutorService executorService;


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

    public void start() throws JMSException {

        isInit = true;
        timer.restart();
        logger.debug("Starting MessageStream thread.");
        new Thread(messageStream).start();
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

                        sendMessage(producer, session);


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

    private void sendMessage(MessageProducer producer, Session session) throws InterruptedException, JMSException {
        int localCounter = 0;
        logger.debug("Start reading and sending messages.");
        List<TextMessage> batch = new ArrayList<>();

        while(messageStream.isRunning() || !messageQueue.isEmpty()) {
            int count = messageQueue.drainTo(batch, 100);
            if (count == 0) {
                Thread.sleep(1); // Затримуємо потік на 1 мілісекунду, щоб не займати зайвої процесорної потужності.
                continue;
            }

                logger.debug("Send batch = {}", batch.size());
                sendBatch(producer, batch);
                sendMessageCounter.addAndGet(batch.size());
                localCounter += batch.size();
                batch.clear();

        }
        if (!batch.isEmpty()) {
            logger.debug("Send batch = {}", batch.size());
            sendBatch(producer, batch);
            sendMessageCounter.addAndGet(batch.size());
            localCounter += batch.size();
            batch.clear();
        }
        producer.send( session.createTextMessage("END"));
        logger.debug("MessageProducer send Poison Pill");

        logger.debug("Closing producer.");
        producer.close();
        session.close();

        logger.debug("This thread done! Send messages number = {}", localCounter);
    }
    private void sendBatch(MessageProducer producer, List<TextMessage> batch) {
        batch.stream().forEach(message -> {
            try {
                producer.send(message);
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
