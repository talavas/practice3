package shpp.level2.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import shpp.level2.message.MessagePojo;
import shpp.level2.message.MessageValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ValidMessageCSVWriterImpTest {
    @Mock
    private MessageValidator validator;

    private BlockingQueue<MessagePojo> queue;
    private ValidMessageCSVWriterImp writer;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        queue = new LinkedBlockingQueue<>();
        writer = new ValidMessageCSVWriterImp(validator, "test.csv", queue);
    }

    @Test
    void run_writesValidMessagesToCSV() {
        MessagePojo message = new MessagePojo("test name", 12, LocalDateTime.now());
        queue.add(message);

        when(validator.isRunning()).thenReturn(true, false);

        writer.run();

        verify(validator, times(2)).isRunning();
        Path path = Paths.get("test.csv");
        try {
            long linesCount = Files.lines(path).count();
            assertEquals(2, linesCount, "Expected 2 records");

            String[] headers = Files.lines(path).findFirst().get().split(",");
            assertEquals(2, headers.length, "Expected 2 number of headers in the file.");
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}