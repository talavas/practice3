package shpp.level2.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.FIELD;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidNameValidator.class)
@Documented
public @interface ValidName {
    String message() default "Name is invalid";

    String nameLengthMessage() default "Name has less than {nameLength} symbols";

    String characterMessage() default "Name doesn't contain '{containChar}' character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int nameLength() default 5;
    char containChar() default 'a';

}
