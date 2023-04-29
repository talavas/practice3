package shpp.level2.validation;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

class ValidNameValidatorTest {
    private ValidNameValidator validator = new ValidNameValidator();

    @BeforeEach
    void setUp(){
        validator.initialize(new ValidName() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String message() {
                return null;
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public int nameLength() {
                return 5;
            }

            @Override
            public char containChar() {
                return 'a';
            }

            @Override
            public String characterMessage() {
                return "Name must contain the letter 'a'.";
            }

            @Override
            public String nameLengthMessage() {
                return "Name must be at least 5 characters long.";
            }
        });
    }

    @Test
    void testValidName() {
        assertTrue(validator.isValid("John Abort", mockContext()));
    }

    @Test
    void testNameTooShort() {
        assertFalse(validator.isValid("Jo", mockContext()));
    }

    @Test
    void testNameNotContainChar() {
        assertFalse(validator.isValid("Emily", mockContext()));
    }

    @Test
    void testNameNull() {
        assertFalse(validator.isValid(null, mockContext()));
    }

    private ConstraintValidatorContext mockContext() {
        return new ConstraintValidatorContext() {
            @Override
            public void disableDefaultConstraintViolation() {}

            @Override
            public String getDefaultConstraintMessageTemplate() {
                return null;
            }

            @Override
            public ClockProvider getClockProvider() {
                return null;
            }

            @Override
            public ConstraintViolationBuilder buildConstraintViolationWithTemplate(String messageTemplate) {
                return new ConstraintViolationBuilder() {
                    @Override
                    public NodeBuilderDefinedContext addNode(String name) {
                        return null;
                    }

                    @Override
                    public NodeBuilderCustomizableContext addPropertyNode(String name) {
                        return null;
                    }

                    @Override
                    public LeafNodeBuilderCustomizableContext addBeanNode() {
                        return null;
                    }

                    @Override
                    public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(String s, Class<?> aClass, Integer integer) {
                        return null;
                    }

                    @Override
                    public NodeBuilderDefinedContext addParameterNode(int i) {
                        return null;
                    }

                    @Override
                    public ConstraintValidatorContext addConstraintViolation() {
                        return null;
                    }
                };
            }

            @Override
            public <T> T unwrap(Class<T> aClass) {
                return null;
            }
        };
    }
}