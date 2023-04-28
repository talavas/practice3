package shpp.level2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import shpp.level2.message.MessageStream;
import shpp.level2.util.ConnectionMQ;

import javax.jms.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProducerTest {

    private static final int THREADS = 2;

    private static final int MESSAGE_COUNT = 200;

    @Mock
    private static ConnectionMQ connectionMQ;

    @Mock
    Session session;

    @Mock
    MessageProducer messageProducer;

    @Mock
    Queue queue;

    @Mock
    MessageStream messageStream;


    @BeforeEach
    void setUp() throws JMSException {
        MockitoAnnotations.openMocks(this);
        when(connectionMQ.createSession()).thenReturn(session);
        when(connectionMQ.createQueue(session)).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(messageProducer);
        doAnswer(invocationOnMock -> null).when(messageProducer).send(any(TextMessage.class));
        when(messageStream.isRunning()).thenReturn(false);



    }

    @Test
    void testProducerSendMessage() throws Exception {
        BlockingQueue<TextMessage> messageQueue = new LinkedBlockingQueue<>();
        for (int i = 0; i < MESSAGE_COUNT; i++){
            messageQueue.add(mock(TextMessage.class));
        }

        Producer producer = new Producer(connectionMQ, messageQueue, messageStream, THREADS);
        producer.setPoisonPill("END");
        assertEquals(MESSAGE_COUNT, messageQueue.size(), "Unexpected message count");
        producer.start();


        verify(session, times(THREADS)).close();
        verify(messageProducer, times(THREADS)).close();
        verify(messageProducer, times(MESSAGE_COUNT)).send(any(TextMessage.class));
    }

}