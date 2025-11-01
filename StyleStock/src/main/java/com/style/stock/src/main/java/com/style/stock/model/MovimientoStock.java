package com.style.stock.model;

import java.time.LocalDateTime;

/**
 * Modelo de MovimientoStock para auditoría
 */
public class MovimientoStock {
    private Integer id;
    private Integer productoId;
    private TipoMovimiento tipo;
    private Integer cantidad;
    private Integer stockAnterior;
    private Integer stockNuevo;
    private String referencia;
    private String observaciones;
    private LocalDateTime createdAt;

    public enum TipoMovimiento {
        INGRESO("INGRESO"),
        EGRESO("EGRESO"),
        AJUSTE("AJUSTE"),
        VENTA("VENTA");

        private final String valor;
        TipoMovimiento(String valor) { this.valor = valor; }
        public String getValor() { return valor; }
    }

    public MovimientoStock() {}

    public MovimientoStock(Integer productoId, TipoMovimiento tipo, Integer cantidad, 
                          Integer stockAnterior, Integer stockNuevo, String referencia) {
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.referencia = referencia;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getProductoId() { return productoId; }
    public void setProductoId(Integer productoId) { this.productoId = productoId; }

    public TipoMovimiento getTipo() { return tipo; }
    public void setTipo(TipoMovimiento tipo) { this.tipo = tipo; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Integer getStockAnterior() { return stockAnterior; }
    public void setStockAnterior(Integer stockAnterior) { this.stockAnterior = stockAnterior; }

    public Integer getStockNuevo() { return stockNuevo; }
    public void setStockNuevo(Integer stockNuevo) { this.stockNuevo = stockNuevo; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}