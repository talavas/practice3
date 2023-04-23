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

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageValidator implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(MessageValidator.class);

    private final Consumer consumer;
    BlockingQueue<MessagePojo> receivedMessages;

    BlockingQueue<MessagePojo> validMessages;
    BlockingQueue<InvalidMessageDTO> invalidMessages;
    private final StopWatch timer = new StopWatch();

    private AtomicInteger validatedMessageCounter = new AtomicInteger(0);
    private AtomicInteger validMessageCounter = new AtomicInteger(0);

    private AtomicInteger invalidMessageCounter = new AtomicInteger(0);

    public boolean isRunning = true;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    public MessageValidator(Consumer consumer, BlockingQueue<MessagePojo> validMessages, BlockingQueue<InvalidMessageDTO> invalidMessages) {
        this.consumer = consumer;
        this.receivedMessages = consumer.getMessageQueue();
        this.validMessages = validMessages;
        this.invalidMessages = invalidMessages;
        logger.debug("Created new MessageValidator object");
    }

    @Override
    public void run() {
        timer.restart();
        logger.debug("Starting validate messages");
        MessagePojo message;
        InvalidMessageDTO invalidMessage;
        while (consumer.isRunning) {
            while (!receivedMessages.isEmpty()){
                try {
                   message = receivedMessages.take();
                    Set<ConstraintViolation<MessagePojo>> violations = validator.validate(message);
                    if (violations.isEmpty()) {
                        validMessages.put(message);
                        validMessageCounter.incrementAndGet();
                    } else {
                        invalidMessage = new InvalidMessageDTO(message, generateErrors(violations));
                        invalidMessages.put(invalidMessage);
                        invalidMessageCounter.incrementAndGet();
                    }
                    validatedMessageCounter.incrementAndGet();
                } catch (InterruptedException e) {
                    logger.error("Issue in thread.", e);
                }
            }
        }
        logger.debug("Validator stop validation, validate = {} messages", validatedMessageCounter.get());
        logger.debug("Valid messages = {}, invalid messages = {}", validMessageCounter.get(), invalidMessageCounter.get());
        logger.debug("Time execution = {} ms", timer.taken());
        logger.info("Validating messages rps={}", (validatedMessageCounter.doubleValue() / timer.taken()) * 1000);
        isRunning = false;
    }
    private String generateErrors(Set<ConstraintViolation<MessagePojo>> violations) {
        ObjectNode errorsNode = JsonNodeFactory.instance.objectNode();
        ArrayNode errorsArray = errorsNode.putArray("errors");
        for (ConstraintViolation<MessagePojo> violation : violations) {
            errorsArray.add(violation.getPropertyPath() + " " + violation.getMessage());
        }
        return errorsNode.toString();
    }
}

