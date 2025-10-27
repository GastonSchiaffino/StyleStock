package com.style.stock.exception;

/**
 * Excepción para errores de validación
 */
public class ValidationException extends StockException {
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}