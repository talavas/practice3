package shpp.level2.message;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.activemq.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageValidator implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(MessageValidator.class);

    private final Consumer consumer;
    BlockingQueue<MessagePojo> receivedMessages;

    BlockingQueue<MessagePojo> validMessages;
    BlockingQueue<InvalidMessageDTO> invalidMessages;
    private final StopWatch timer = new StopWatch();

    protected final AtomicInteger validatedMessageCounter = new AtomicInteger(0);
    private final AtomicInteger validMessageCounter = new AtomicInteger(0);

    private final AtomicInteger invalidMessageCounter = new AtomicInteger(0);
    public synchronized boolean isRunning() {
        return isRunning;
    }

    protected boolean isRunning = true;
    private final Validator validator;
    private boolean isInit = false;

    private final int threads;

    private final CountDownLatch latch;

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    private int batchSize = 100;

    private final ExecutorService executorService;
    public MessageValidator(Consumer consumer, BlockingQueue<MessagePojo> validMessages, BlockingQueue<InvalidMessageDTO> invalidMessages, int threads) {
        this.consumer = consumer;
        this.receivedMessages = consumer.getMessageQueue();
        this.validMessages = validMessages;
        this.invalidMessages = invalidMessages;
        this.latch = new CountDownLatch(threads);
        this.threads = threads;
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.executorService = new ThreadPoolExecutor(threads * 2, threads * 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        logger.debug("Created new MessageValidator pool");
    }
    private void start() {
        isInit = true;
        timer.restart();
        logger.debug("Starting MessageValidator threads.");
        for (int i = 0; i < threads * 2; i++) {
            executorService.execute(this);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        finally {
            executorService.shutdown();
        }

        isRunning = false;
        logger.info("Validator stop validation, validate = {} messages", validatedMessageCounter.get());
        logger.info("Valid messages = {}, invalid messages = {}", validMessageCounter.get(), invalidMessageCounter.get());
        logger.info("Time execution = {} ms", timer.taken());
        logger.info("Validating messages rps={}", (validatedMessageCounter.doubleValue() / timer.taken()) * 1000);

    }

    @Override
    public void run() {
        if(!isInit){
            init();
        }else{
            logger.debug("MessageValidator thread {} started", Thread.currentThread().getName());
            while (consumer.isRunning() || !receivedMessages.isEmpty()) {
                List<MessagePojo> batch = new ArrayList<>();
                int count = receivedMessages.drainTo(batch, batchSize);
                if (count == 0) {
                    continue;
                }
                logger.debug("Validating messages batch = {}", batch.size());
                try {
                    validateBatch(batch);
                } catch (InterruptedException e) {
                    logger.error("Can't validate messages");
                    Thread.currentThread().interrupt();
                }

            }
            latch.countDown();
            logger.debug("MessageValidator thread {} stopped", Thread.currentThread().getName());
        }

    }

    private void validateBatch(List<MessagePojo> batch) throws InterruptedException {
        batch.forEach(messagePojo -> {
            Set<ConstraintViolation<MessagePojo>> violations = validator.validate(messagePojo);
            if (violations.isEmpty()) {
                if(validMessages.offer(messagePojo)){
                    validMessageCounter.incrementAndGet();
                }
            } else {
                if(invalidMessages.offer(new InvalidMessageDTO(messagePojo, generateErrors(violations)))){
                    invalidMessageCounter.incrementAndGet();
                }

            }
            validatedMessageCounter.incrementAndGet();
        });
        Thread.sleep(200);
    }

    private String generateErrors(Set<ConstraintViolation<MessagePojo>> violations) {
        ObjectNode errorsNode = JsonNodeFactory.instance.objectNode();
        ArrayNode errorsArray = errorsNode.putArray("errors");
        for (ConstraintViolation<MessagePojo> violation : violations) {
            errorsArray.add(violation.getMessage());
        }
        return errorsNode.toString();
    }

    protected void init() {
        logger.debug("Init MessageValidator.");
        start();
    }


}

