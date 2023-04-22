package shpp.level2;

import shpp.level2.message.MessagePojo;
import shpp.level2.message.MessageStream;
import shpp.level2.util.CSVFileWriter;
import shpp.level2.util.Config;
import shpp.level2.util.ConnectionMQ;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    private static final long DEFAULT_MESSAGE_NUMBERS = 10;
    public static void main(String[] args) throws JMSException {
        long counter = DEFAULT_MESSAGE_NUMBERS;
        int theads = 1;
        if(args.length == 2){
            counter = Integer.parseInt(args[0]);
            theads = Integer.parseInt(args[1]);
        }else if(args.length == 1) {
            counter = Integer.parseInt(args[0]);
        }
        Config config = new Config();

        ConnectionMQ connectionProducer = new ConnectionMQ(config);
        ConnectionMQ connectionConsumer = new ConnectionMQ(config);

        BlockingQueue<TextMessage> producerQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> consumerQueue = new LinkedBlockingQueue<>();

        MessageStream messageStream = new MessageStream(connectionProducer, config, producerQueue, counter);
        Producer producer = new Producer(connectionProducer, producerQueue, messageStream, theads);


        Consumer consumer = new Consumer(connectionConsumer, consumerQueue, theads);
        CSVFileWriter fileWriter = new CSVFileWriter(consumerQueue, consumer, theads);
        new Thread(producer).start();
        new Thread(consumer).start();

    }
}