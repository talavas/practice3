package shpp.level2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import shpp.level2.message.MessagePojo;
import shpp.level2.util.ConnectionMQ;

import javax.jms.*;

import java.util.concurrent.BlockingQueue;

import static org.mockito.Mockito.*;

class ConsumerTest {
    private Consumer consumer;

    @Mock
    private ConnectionMQ connectionMQ;

    @Mock
    private BlockingQueue<MessagePojo> messageQueue;

    @Mock
    private Session session;

    @Mock
    private Queue queue;

    @Mock
    private MessageConsumer messageConsumer;

    @Mock
    private TextMessage textMessage;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new Consumer(connectionMQ, messageQueue, 1);
        consumer.setPoisonPill("poison");
    }

    @Test
    void testRun() throws JMSException {
        when(connectionMQ.createSession()).thenReturn(session);
        when(connectionMQ.createQueue(session)).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(messageConsumer);
        when(messageConsumer.receiveNoWait()).thenReturn(textMessage);
        when(textMessage.getText()).thenReturn("{\"name\":\"hlrwtfuzncsxlf\",\"count\":27,\"createdAt\":\"2023-04-30T14:40:23.0389838\"}", "poison");

        consumer.run();

        verify(connectionMQ, times(1)).createSession();
        verify(connectionMQ, times(1)).createQueue(session);
        verify(session, times(1)).createConsumer(queue);
        verify(messageConsumer, times(2)).receiveNoWait();
        verify(textMessage, times(2)).getText();
        verify(messageQueue, times(1)).offer(any(MessagePojo.class));
    }

}