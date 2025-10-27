package com.style.stock.exception;

/**
 * Excepción base para errores de la aplicación
 */
public class StockException extends Exception {
    public StockException(String message) {
        super(message);
    }

    public StockException(String message, Throwable cause) {
        super(message, cause);
    }
}
