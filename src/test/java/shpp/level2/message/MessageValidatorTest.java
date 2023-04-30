package shpp.level2.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import shpp.level2.Consumer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageValidatorTest {
    @Mock
    private Consumer consumer;

    private BlockingQueue<MessagePojo> validMessages;
    private BlockingQueue<InvalidMessageDTO> invalidMessages;
    private MessageValidator messageValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validMessages = new LinkedBlockingQueue<>();
        invalidMessages = new LinkedBlockingQueue<>();
        List<MessagePojo> messages = new ArrayList<>();
        messages.add(new MessagePojo("test1", 10, LocalDateTime.now()));
        messages.add(new MessagePojo("test2", 2, LocalDateTime.now()));
        messages.add(new MessagePojo("test1aaaaa", 10, LocalDateTime.now()));
        messages.add(new MessagePojo("test2aaaaa", 20, LocalDateTime.now()));

        when(consumer.getMessageQueue()).thenReturn(new LinkedBlockingQueue<>(messages));
        when(consumer.isRunning()).thenReturn(true, false);
        messageValidator = new MessageValidator(consumer, validMessages, invalidMessages, 2);
    }

    @Test
    void testRun() {

        messageValidator.run();

        assertEquals(2, validMessages.size());
        assertEquals(2, invalidMessages.size());
    }

}