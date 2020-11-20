package br.gustavobilert.validation;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationResponse {

    private final List<FieldViolation> errors;

    ValidationResponse(ConstraintViolationException exception) {
        errors = mapErrorsToFields(exception);
    }

    private List<FieldViolation> mapErrorsToFields(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(this::getFieldViolation).collect(Collectors.toList());
    }

    private FieldViolation getFieldViolation(ConstraintViolation<?> constraintViolation) {
        String property = extractLastPart(constraintViolation.getPropertyPath());
        return new FieldViolation(property, constraintViolation.getMessage());
    }

    private String extractLastPart(Path propertyPath) {
        return StringUtils.substringAfterLast(propertyPath.toString(), ".");
    }

    public List<FieldViolation> getErrors() {
        return errors;
    }

}
