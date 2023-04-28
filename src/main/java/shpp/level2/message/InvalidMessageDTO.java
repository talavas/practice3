package shpp.level2.message;


public class InvalidMessageDTO {

    public String getName() {
        return name;
    }

    private final String name;

    public int getCount() {
        return count;
    }

    private final int count;

    public String getErrors() {
        return errors;
    }

    private final String errors;

    public InvalidMessageDTO(MessagePojo message, String errors) {
        this.name = message.getName();
        this.count = message.getCount();
        this.errors = errors;
    }
}
