package shpp.level2.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import shpp.level2.util.Config;
import shpp.level2.util.ConnectionMQ;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MessageStreamTest {
    @Mock
    ConnectionMQ connectionMQ;

    @Mock
    Config config;

    @Mock
    Session session;

    @Mock
    TextMessage textMessage;
    @Mock
    BlockingQueue<TextMessage> messageQueue;

    long maxMessageCounter = 10L;

    MessageStream messageStream;

    @BeforeEach
    void setUp() throws JMSException {
        MockitoAnnotations.openMocks(this);
        when(connectionMQ.createSession()).thenReturn(session);
        when(session.createTextMessage(anyString())).thenReturn(textMessage);
        when(config.getProperty("stop.time")).thenReturn("2");
        when(messageQueue.add(any(TextMessage.class))).thenReturn(true);

        messageStream = new MessageStream(connectionMQ, config, messageQueue, maxMessageCounter);

    }

    @Test
    void run_createsSessionAndGeneratesMessages() throws JMSException {
        messageStream.run();

        verify(connectionMQ).createSession();
        verify(session).close();

        assertEquals(maxMessageCounter, messageStream.counter.get());
        assertFalse(messageStream.isRunning());

    }

    @Test
    void generateStream_generatesMessagesUntilMaxDurationOrMaxMessageCounterIsReached() throws JMSException {
        messageStream.generateStream(session);

        verify(session, times((int) maxMessageCounter)).createTextMessage(anyString());
        assertEquals(maxMessageCounter, messageStream.counter.get());

        messageStream.generateStream(session);
        assertTrue(messageStream.counter.get() > 0);

    }

    @Test
    void generateStream_addsGeneratedMessagesToMessageQueue()  {
        messageStream = new MessageStream(connectionMQ, config, messageQueue, maxMessageCounter);
        messageStream.generateStream(session);
        verify(messageQueue, times((int) maxMessageCounter)).add(any());
    }

    @Test
    void generateStreamWithThrowsException() throws JMSException {
        reset(session);
        when(session.createTextMessage(anyString())).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> {
            messageStream.generateStream(session);
        });

        verify(messageQueue, never()).add(any());
    }

}