package shpp.level2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.InvalidMessageDTO;
import shpp.level2.message.MessagePojo;
import shpp.level2.message.MessageStream;
import shpp.level2.message.MessageValidator;
import shpp.level2.util.*;

import javax.jms.TextMessage;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String POISSON_PILL = "END";
    private static final long DEFAULT_MESSAGE_NUMBERS = 1000;
    private static final String VALID_CSV = "valid.csv";

    private static final String INVALID_CSV = "invalid.csv";
    public static void main(String[] args) {
        long counter = DEFAULT_MESSAGE_NUMBERS;
        int threads = 1;
        if(args.length == 2){
            counter = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        }else if(args.length == 1) {
            counter = Integer.parseInt(args[0]);
        }
        Config config = new Config();


        String type = System.getProperty("type", "all");
        if(type.equals("producer")){
            runProducer(config, threads, counter);

        } else if (type.equals("consumer")) {
           runConsumer(config, threads);

        }else {
            runProducer(config, threads, counter);
            runConsumer(config, threads);
        }
    }

    private static void runConsumer(Config config, int threads) {
        BlockingQueue<MessagePojo> consumerQueue = new LinkedBlockingQueue<>();
        try {
            ConnectionMQ connectionConsumer = new ConnectionMQ(config);
            Consumer consumer = new Consumer(connectionConsumer, consumerQueue, threads);

            consumer.setPoisonPill(POISSON_PILL);

            BlockingQueue<MessagePojo> validMessages = new LinkedBlockingQueue<>();
            BlockingQueue<InvalidMessageDTO> invalidMessages = new LinkedBlockingQueue<>();
            MessageValidator validator = new MessageValidator(consumer, validMessages, invalidMessages, threads);


            CSVWriter<MessagePojo> validFileWriter = new ValidMessageCSVWriterImp(validator, VALID_CSV, validMessages);
            CSVWriter<InvalidMessageDTO> invalidFileWriter = new InvalidMessageCSVWriterImp(validator, INVALID_CSV, invalidMessages);
            new Thread(consumer).start();
            new Thread(validator).start();
            new Thread(validFileWriter).start();
            new Thread(invalidFileWriter).start();
        } catch (IOException e) {
            logger.error("Can't create file.", e);
        }

    }

    private static void runProducer(Config config, int threads, long counter) {
        BlockingQueue<TextMessage> producerQueue = new LinkedBlockingQueue<>();
        ConnectionMQ connectionProducer = new ConnectionMQ(config);
        MessageStream messageStream = new MessageStream(connectionProducer, config, producerQueue, counter);
        Producer producer = new Producer(connectionProducer, producerQueue, messageStream, threads);
        producer.setPoisonPill(POISSON_PILL);
        producer.setBatchSize(100);
        new Thread(producer).start();

    }
}