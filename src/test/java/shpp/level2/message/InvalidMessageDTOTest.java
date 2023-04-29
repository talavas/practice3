package shpp.level2.message;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InvalidMessageDTOTest {
    @Test
    void testGetName() {
        MessagePojo message = new MessagePojo("test", 10, LocalDateTime.now());
        InvalidMessageDTO invalidMessage = new InvalidMessageDTO(message, "error");
        assertEquals(message.getName(), invalidMessage.getName());
    }

    @Test
    void testGetCount() {
        MessagePojo message = new MessagePojo("test", 10, LocalDateTime.now());
        InvalidMessageDTO invalidMessage = new InvalidMessageDTO(message, "error");
        assertEquals(message.getCount(), invalidMessage.getCount());
    }

    @Test
    void testGetErrors() {
        MessagePojo message = new MessagePojo("test", 10, LocalDateTime.now());
        InvalidMessageDTO invalidMessage = new InvalidMessageDTO(message, "error");
        assertEquals("error", invalidMessage.getErrors());
    }

    @Test
    void testConstructor() {
        MessagePojo message = new MessagePojo("test", 10, LocalDateTime.now());
        String errors = "error";
        InvalidMessageDTO invalidMessage = new InvalidMessageDTO(message, errors);
        assertNotNull(invalidMessage);
        assertEquals(message.getName(), invalidMessage.getName());
        assertEquals(message.getCount(), invalidMessage.getCount());
        assertEquals(errors, invalidMessage.getErrors());
    }
}