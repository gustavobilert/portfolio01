package br.gustavobilert.validation;

public class FieldViolation {
    private String field;
    private String message;

    public FieldViolation(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}
