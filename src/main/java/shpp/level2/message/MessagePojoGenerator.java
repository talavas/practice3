package shpp.level2.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.Random;


public class MessagePojoGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    private static final Random random = new Random();

    private static final int MAX_LENGTH = 15;

    private static final int MAX_COUNT = 30;

    private MessagePojoGenerator(){
        throw new IllegalStateException("Utility class");
    }


    public static MessagePojo generateMessage(){
        return new MessagePojo(
              generateRandomString(),
                random.nextInt(MAX_COUNT),
                LocalDateTime.now()
        );
    }
    private  static String generateRandomString() {
        int length  = random.nextInt(MAX_LENGTH);
        return random.ints('a', 'z' + 1)
                .limit(length + 1)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static String toJson(MessagePojo message) throws JsonProcessingException {
        return objectMapper.writeValueAsString(message);
    }

    public static MessagePojo toMessagePojo(String jsonMessage) throws JsonProcessingException {
        return objectMapper.readValue(jsonMessage, MessagePojo.class);
    }

}
