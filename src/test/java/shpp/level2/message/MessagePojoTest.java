package shpp.level2.message;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MessagePojoTest {

    private static Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testNotValidNameNoCharA() {
        MessagePojo message = new MessagePojo("Not vlid nme", 1, LocalDateTime.now());

        Set<ConstraintViolation<MessagePojo>> constraintViolations =
                validator.validate(message);

        assertEquals(2, constraintViolations.size());


    }

    @Test
    void testNotValidNameLength() {
        MessagePojo message = new MessagePojo("Alex", 1, LocalDateTime.now());

        Set<ConstraintViolation<MessagePojo>> constraintViolations =
                validator.validate(message);

        assertEquals(2, constraintViolations.size());

    }

    @Test
    void testValidName() {
        MessagePojo message = new MessagePojo("Alex Talavas", 1, LocalDateTime.now());

        Set<ConstraintViolation<MessagePojo>> constraintViolations =
                validator.validate(message);

                assertEquals(1, constraintViolations.size());

    }
}