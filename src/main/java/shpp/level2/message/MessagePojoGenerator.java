package shpp.level2.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Random;


public class MessagePojoGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Random random = new Random();

    private static final int MAX_LENGTH = 10;

    private static final int MAX_COUNT = 30;

    private MessagePojoGenerator(){
        throw new IllegalStateException("Utility class");
    }


    public static MessagePojo generateMessage(){
        return new MessagePojo(
              generateRandomString(MAX_LENGTH),
                random.nextInt(MAX_COUNT)
        );
    }
    private  static String generateRandomString(int length) {
        return random.ints('a', 'z' + 1)
                .limit(length)
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
