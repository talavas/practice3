package shpp.level2.util;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

public class ConnectionMQ {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionMQ.class);

    public Queue getQueue() {
        return queue;
    }

    private final Queue queue;


    private final ConnectionFactory connectionFactory;

    public Connection getConnection() {
        return connection;
    }

    private final Connection connection;

    public Session getSession() {
        return session;
    }

    private final Session session;

    public ConnectionMQ(Config config) throws JMSException {
        this.connectionFactory = ConnectionMQ.getConnectionFactory(config);
        this.connection = ConnectionMQ.createConnection(config);
        this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.queue = session.createQueue(config.getProperty("activemq.queue.name"));
    }

    public static ConnectionFactory getConnectionFactory(Config config) {
        String brokerUrl = config.getProperty("activemq.brokerUrl");
        String userName = config.getProperty("activemq.userName");
        String password = config.getProperty("activemq.password");
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(userName, password, brokerUrl);
        return connectionFactory;
    }

    public static ActiveMQQueue getQueue(Config config) throws JMSException {
        String queueName = config.getProperty("activemq.queueName");
        return new ActiveMQQueue(queueName);
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
        this.session.close();
        this.connection.close();
    }

}
