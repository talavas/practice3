package shpp.level2.util;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.lang.IllegalStateException;

public class ConnectionMQ {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionMQ.class);

   // public Queue getQueue() {
    //    return queue;
   // }

    //private final Queue queue;


    private final ConnectionFactory connectionFactory;

    public Connection getConnection() {
        return connection;
    }

    private final Connection connection;

    private  Config config;

    public String getQueueName() {
        return queueName;
    }

    private String queueName;



    public ConnectionMQ(Config config) throws JMSException {
        this.queueName = config.getProperty("activemq.queue.name");
        this.config = config;
        this.connectionFactory = ConnectionMQ.getConnectionFactory(config);
        this.connection = ConnectionMQ.createConnection(config);
        logger.info("New connection to ActiveMQ created");
    }

    public Session createSession() {
        try {
            return this.connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        } catch (JMSException e) {
            return null;
        }
    }

    public Queue createQueue(Session session){
        try {
            return session.createQueue(getQueueName());
        } catch (JMSException e) {
           logger.error("Can't create queue within session");
        }
        return null;
    }

    public static ConnectionFactory getConnectionFactory(Config config) {
        String brokerUrl = config.getProperty("activemq.brokerUrl");
        String userName = config.getProperty("activemq.userName");
        String password = config.getProperty("activemq.password");
        return new ActiveMQConnectionFactory(userName, password, brokerUrl);
    }

    public static Connection createConnection(Config config)  {
        ConnectionFactory connectionFactory = getConnectionFactory(config);
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
        } catch (JMSException e) {
            logger.error("Can't create connection.", e);

            return null;
        }

        return connection;
    }

    public void closeConnection() throws JMSException {
        this.connection.close();
    }

}
