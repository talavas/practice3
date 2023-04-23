package shpp.level2.message;

import jakarta.validation.constraints.Min;

public class InvalidMessageDTO {

    public String getName() {
        return name;
    }

    private String name;

    public int getCount() {
        return count;
    }

    private int count;

    public String getErrors() {
        return errors;
    }

    private String errors;

    public InvalidMessageDTO(MessagePojo message, String errors) {
        this.name = message.getName();
        this.count = message.getCount();
        this.errors = errors;
    }
}
