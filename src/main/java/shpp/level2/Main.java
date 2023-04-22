package shpp.level2;

import shpp.level2.message.MessageStream;
import shpp.level2.util.Config;

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
        BlockingQueue<TextMessage> producerQueue = new LinkedBlockingQueue<>();
        MessageStream messageStream = new MessageStream(config, producerQueue, counter);
        Producer producer = new Producer(config, producerQueue, messageStream, theads);
        producer.start();
    }
}