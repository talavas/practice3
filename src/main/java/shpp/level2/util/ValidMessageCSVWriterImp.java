package shpp.level2.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.MessagePojo;
import shpp.level2.message.MessageValidator;


import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;


public class ValidMessageCSVWriterImp extends CSVWriter<MessagePojo>{
    private static final Logger logger = LoggerFactory.getLogger(ValidMessageCSVWriterImp.class);

    public ValidMessageCSVWriterImp(MessageValidator validator, String filename, BlockingQueue<MessagePojo> queue) throws IOException {
        super(validator, filename, queue);

    }

    @Override
    public void run() {
        timer.restart();
        MessagePojo message;
        try (
                FileWriter fileWriter = new FileWriter(fileName);
                CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);

        ){
            logger.debug("Created CSVPrinter instance");
            while (messageValidator.isRunning) {
                while (!queue.isEmpty()){
                        message = queue.take();
                        csvPrinter.printRecord(message.getName(), message.getCount());
                        printedMessageCounter.incrementAndGet();
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("CSWWriter write {} valid messages", printedMessageCounter.get());
        logger.debug("Time execution = {} ms", timer.taken());
        logger.info("Writing valid messages rps={}", (printedMessageCounter.doubleValue() / timer.taken()) * 1000);

    }

}
