package shpp.level2.message;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import shpp.level2.validation.ValidName;

import java.time.LocalDateTime;

public class MessagePojo {

    @NotNull
    @ValidName(nameLength = 7)
    private String name;
    @Min(value = 10, message = " Count should be equal or higher than {value}")
    private int count;
    private LocalDateTime createdAt;

    public MessagePojo(String name, int count, LocalDateTime createdAt){
        this.name = name;
        this.count = count;
        this.createdAt = createdAt;
    }

    public MessagePojo(){};
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

   public LocalDateTime getCreatedAt() {
        return createdAt;
   }

    public void setCreatedAt(LocalDateTime createdAt) {
       this.createdAt = createdAt;
    }
}
