package shpp.level2;

import shpp.level2.message.InvalidMessageDTO;
import shpp.level2.message.MessagePojo;
import shpp.level2.message.MessageStream;
import shpp.level2.message.MessageValidator;
import shpp.level2.util.*;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    private static final long DEFAULT_MESSAGE_NUMBERS = 10;
    public static void main(String[] args) throws JMSException, IOException {
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
        BlockingQueue<MessagePojo> consumerQueue = new LinkedBlockingQueue<>();

        MessageStream messageStream = new MessageStream(connectionProducer, config, producerQueue, counter);
        Producer producer = new Producer(connectionProducer, producerQueue, messageStream, theads);


        Consumer consumer = new Consumer(connectionConsumer, consumerQueue, theads);
        BlockingQueue<MessagePojo> validMessages = new LinkedBlockingQueue<>();
        BlockingQueue<InvalidMessageDTO> invalidMessages = new LinkedBlockingQueue<>();
        MessageValidator validator = new MessageValidator(consumer, validMessages, invalidMessages);


        CSVWriter<MessagePojo> validFileWriter = new ValidMessageCSVWriterImp(validator, "valid.csv", validMessages);
        CSVWriter<InvalidMessageDTO> invalidFileWriter = new InvalidMessageCSVWriterImp(validator, "invalid.csv", invalidMessages);
        String type = System.getProperty("type", "all");
        if(type.equals("producer")){
            connectionConsumer.closeConnection();
            new Thread(producer).start();
        } else if (type.equals("consumer")) {
            connectionProducer.closeConnection();
            new Thread(consumer).start();
            new Thread(validator).start();
            new Thread(validFileWriter).start();
            new Thread(invalidFileWriter).start();
        }else {
            new Thread(producer).start();
            new Thread(consumer).start();
            new Thread(validator).start();
            new Thread(validFileWriter).start();
            new Thread(invalidFileWriter).start();
        }



    }
}