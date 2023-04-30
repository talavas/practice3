package shpp.level2.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import shpp.level2.Consumer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageValidatorTest {
    @Mock
    private Consumer consumer;
@Mock
    private BlockingQueue<MessagePojo> validMessages;
@Mock
private BlockingQueue<InvalidMessageDTO> invalidMessages;
    private MessageValidator messageValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(validMessages.offer(any(MessagePojo.class))).thenReturn(true);
        when(invalidMessages.offer(any(InvalidMessageDTO.class))).thenReturn(true);
        messageValidator = new MessageValidator(consumer, validMessages, invalidMessages, 2);
    }

    @Test
    void testRun() throws InterruptedException {
        List<MessagePojo> messages = new ArrayList<>();
        messages.add(new MessagePojo("test1", 10, LocalDateTime.now()));
        messages.add(new MessagePojo("test2", 2, LocalDateTime.now()));
        messages.add(new MessagePojo("test1aaaaa", 10, LocalDateTime.now()));
        messages.add(new MessagePojo("test2aaaaa", 20, LocalDateTime.now()));
        messageValidator.validateBatch(messages);

       assertEquals(2, messageValidator.validMessageCounter.get());
       assertEquals(2, messageValidator.invalidMessageCounter.get());
       assertEquals(4, messageValidator.validatedMessageCounter.get());
    }

}