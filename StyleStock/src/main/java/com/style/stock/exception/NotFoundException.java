package com.style.stock.exception;

/**
 * Excepción cuando no se encuentra un registro
 */
public class NotFoundException extends StockException {
    public NotFoundException(String entity, Object id) {
        super(String.format("%s con ID %s no encontrado", entity, id));
    }
}