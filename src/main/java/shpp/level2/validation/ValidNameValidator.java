package shpp.level2.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class ValidNameValidator implements ConstraintValidator<ValidName, String> {

    private int nameLength;

    private char containChar;

    private String notValidNameLengthMessage;

    private String notContainCharMessage;

    @Override
    public void initialize(ValidName constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        nameLength = constraintAnnotation.nameLength();
        containChar = constraintAnnotation.containChar();
        notContainCharMessage = constraintAnnotation.characterMessage();
        notValidNameLengthMessage = constraintAnnotation.nameLengthMessage();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        boolean valid = true;

        if (value.length() < nameLength) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(notValidNameLengthMessage)
                    .addConstraintViolation();
            valid = false;
        }
        if (!value.toLowerCase().contains(String.valueOf(containChar))) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(notContainCharMessage)
                    .addConstraintViolation();
            valid = false;
        }
        return valid;
    }
}
