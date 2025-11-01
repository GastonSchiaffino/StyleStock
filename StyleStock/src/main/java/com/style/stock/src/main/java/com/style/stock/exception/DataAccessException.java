package com.style.stock.exception;

/**
 * Excepción para errores de acceso a datos
 */
public class DataAccessException extends StockException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}