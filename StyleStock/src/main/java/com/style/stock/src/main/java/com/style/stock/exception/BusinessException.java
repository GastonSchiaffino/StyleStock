package com.style.stock.exception;

/**
 * Excepción para errores de negocio
 */
public class BusinessException extends StockException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}