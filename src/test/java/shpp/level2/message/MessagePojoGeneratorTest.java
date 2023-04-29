package shpp.level2.message;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagePojoGeneratorTest {
    @Test
    void testGenerateMessage() {
        MessagePojo message = MessagePojoGenerator.generateMessage();
        assertNotNull(message);
        assertNotNull(message.getName());
        assertTrue(message.getCount() > 0);
        assertNotNull(message.getCreatedAt());
        assertTrue(message.getName().length() <= MessagePojoGenerator.MAX_LENGTH);
        assertTrue(message.getCount() <= MessagePojoGenerator.MAX_COUNT);
    }

    @Test
    void testToJson() throws JsonProcessingException {
        MessagePojo message = mock(MessagePojo.class);
        when(message.getName()).thenReturn("test message");
        when(message.getCount()).thenReturn(10);
        when(message.getCreatedAt()).thenReturn(LocalDateTime.now());
        String jsonMessage = MessagePojoGenerator.toJson(message);
        assertNotNull(jsonMessage);
    }
    @Test
    void testToMessagePojo() throws JsonProcessingException {
        MessagePojo message = mock(MessagePojo.class);
        when(message.getName()).thenReturn("test message");
        when(message.getCount()).thenReturn(10);
        when(message.getCreatedAt()).thenReturn(LocalDateTime.now());
        String jsonMessage = MessagePojoGenerator.toJson(message);
        MessagePojo parsedMessage = MessagePojoGenerator.toMessagePojo(jsonMessage);
        assertNotNull(parsedMessage);
        assertEquals(message.getName(), parsedMessage.getName());
        assertEquals(message.getCount(), parsedMessage.getCount());
        assertEquals(message.getCreatedAt(), parsedMessage.getCreatedAt());
    }


    }
