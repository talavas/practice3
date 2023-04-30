package shpp.level2.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import shpp.level2.message.InvalidMessageDTO;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvalidMessageCSVWriterImpTest {
    @Mock
    private MessageValidator validator;

    private BlockingQueue<InvalidMessageDTO> queue;
    private InvalidMessageCSVWriterImp writer;

    @BeforeEach
    public void setUp()  {
        MockitoAnnotations.openMocks(this);
        queue = new LinkedBlockingQueue<>();
        writer = new InvalidMessageCSVWriterImp(validator, "testin.csv", queue);
    }

    @Test
    void run_writesInValidMessagesToCSV() {
        MessagePojo message = new MessagePojo("test nme", 6, LocalDateTime.now());
        InvalidMessageDTO notValidMessage = new InvalidMessageDTO(message, "errors");
        queue.add(notValidMessage);

        when(validator.isRunning()).thenReturn(true, false);

        writer.run();

        verify(validator, times(2)).isRunning();
        Path path = Paths.get("testin.csv");

        try {
            long linesCount = Files.lines(path).count();
            assertEquals(2, linesCount, "Expected 2 records");

            String[] headers = Files.lines(path).findFirst().get().split(",");
            assertEquals(3, headers.length, "Expected 3 number of headers in the file.");
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}