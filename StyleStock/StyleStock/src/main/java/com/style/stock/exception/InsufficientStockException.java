package com.style.stock.exception;

/**
 * Excepción para stock insuficiente
 */
public class InsufficientStockException extends BusinessException {
    private final int available;
    private final int requested;

    public InsufficientStockException(String producto, int available, int requested) {
        super(String.format("Stock insuficiente para %s. Disponible: %d, Solicitado: %d", 
              producto, available, requested));
        this.available = available;
        this.requested = requested;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}