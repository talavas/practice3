package shpp.level2.util;


import org.apache.activemq.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.MessageValidator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class CSVWriter<T> implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(CSVWriter.class);

    protected final String fileName;
    protected final BlockingQueue<T> queue;
    protected final MessageValidator messageValidator;

    protected final StopWatch timer = new StopWatch();

    protected AtomicInteger printedMessageCounter = new AtomicInteger(0);

    protected CSVWriter(MessageValidator validator, String fileName, BlockingQueue<T> queue) {
        this.fileName = fileName;
        this.queue = queue;
        this.messageValidator = validator;
        logger.debug("Created CSWWriter, fileName={}", fileName);

    }

    public void run() {

    }
}