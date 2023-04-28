package shpp.level2.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shpp.level2.message.InvalidMessageDTO;
import shpp.level2.message.MessageValidator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class InvalidMessageCSVWriterImp extends CSVWriter<InvalidMessageDTO>{
    private static final Logger logger = LoggerFactory.getLogger(InvalidMessageCSVWriterImp.class);
    public InvalidMessageCSVWriterImp(MessageValidator validator, String filename, BlockingQueue<InvalidMessageDTO> queue) {
        super(validator, filename, queue);
    }

    private final String[] headers = {"Name", "Count", "Errors"};

    @Override
    public void run() {
        timer.restart();
        InvalidMessageDTO message;
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();
        try (
                FileWriter fileWriter = new FileWriter(fileName);
                CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)

        ){
            logger.debug("Created CSVPrinter instance for invalid message");
            while (messageValidator.isRunning()){
                while(!queue.isEmpty()) {
                    message = queue.take();
                    csvPrinter.printRecord(message.getName(), message.getCount(), message.getErrors());
                    printedMessageCounter.incrementAndGet();
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error stack with FileWriter, CSVWriter, ThreadInterrupt.", e);
            Thread.currentThread().interrupt();
        }
        logger.info("CSWWriter write {} invalid messages", printedMessageCounter.get());
        logger.info("Time execution = {} ms", timer.taken());
        logger.info("Writing invalid messages rps={}", (printedMessageCounter.doubleValue() / timer.taken()) * 1000);

    }
}
