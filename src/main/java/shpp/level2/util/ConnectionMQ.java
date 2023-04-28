package shpp.level2.util;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;


public class ConnectionMQ {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionMQ.class);
    private final Connection connection;

    public String getQueueName() {
        return queueName;
    }

    private final String queueName;



    public ConnectionMQ(Config config)  {
        this.queueName = config.getProperty("activemq.queue.name");
        this.connection = ConnectionMQ.createConnection(config);
        logger.info("New connection to ActiveMQ created");
    }

    public Session createSession() {
        try {
            return this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
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
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        factory.setUserName(userName);
        factory.setPassword(password);

        factory.setUseAsyncSend(true);
       factory.setDispatchAsync(true);
        return factory;
    }

    public static Connection createConnection(Config config)  {
        ConnectionFactory connectionFactory = getConnectionFactory(config);
        Connection connection;
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
