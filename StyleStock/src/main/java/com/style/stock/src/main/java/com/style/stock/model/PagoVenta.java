package com.style.stock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// ============================================
// PAGO VENTA
// ============================================
public class PagoVenta {
    private Integer id;
    private Integer ventaId;
    private Integer metodoPagoId;
    private MetodoPago metodoPago;
    private Double monto;
    private Integer cuotas;
    private Double comision;
    private String observaciones;
    private LocalDateTime createdAt;

    public PagoVenta() {
        this.cuotas = 1;
        this.comision = 0.0;
    }

    public PagoVenta(Integer ventaId, MetodoPago metodoPago, Double monto) {
        this();
        this.ventaId = ventaId;
        this.metodoPago = metodoPago;
        this.metodoPagoId = metodoPago.getId();
        this.monto = monto;
        this.comision = metodoPago.calcularComision(monto);
    }

    /**
     * Validación completa (requiere ventaId)
     * Usar DESPUÉS de persistir
     */
    public void validate() throws IllegalArgumentException {
        if (ventaId == null) {
            throw new IllegalArgumentException("El ID de la venta es obligatorio");
        }
        validateBasicFields();
    }

    /**
     * Validación básica (NO requiere ventaId)
     * Usar ANTES de persistir
     */
    public void validateBasicFields() throws IllegalArgumentException {
        if (metodoPagoId == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
        if (cuotas == null || cuotas < 1) {
            throw new IllegalArgumentException("Las cuotas deben ser al menos 1");
        }
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVentaId() { return ventaId; }
    public void setVentaId(Integer ventaId) { this.ventaId = ventaId; }
    public Integer getMetodoPagoId() { return metodoPagoId; }
    public void setMetodoPagoId(Integer metodoPagoId) { this.metodoPagoId = metodoPagoId; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { 
        this.metodoPago = metodoPago;
        if (metodoPago != null) this.metodoPagoId = metodoPago.getId();
    }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public Integer getCuotas() { return cuotas; }
    public void setCuotas(Integer cuotas) { this.cuotas = cuotas; }
    public Double getComision() { return comision; }
    public void setComision(Double comision) { this.comision = comision; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PagoVenta pagoVenta = (PagoVenta) o;
        return Objects.equals(id, pagoVenta.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
