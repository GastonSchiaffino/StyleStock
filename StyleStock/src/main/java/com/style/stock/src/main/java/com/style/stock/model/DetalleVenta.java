package com.style.stock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


// ============================================
// DETALLE VENTA
// ============================================
public class DetalleVenta {
    private Integer id;
    private Integer ventaId;
    private Integer varianteId;
    private Variante variante;
    private Integer cantidad;
    private Double precioUnitario;
    private String precioTipo; // "MINORISTA" o "MAYORISTA"
    private Double descuento;
    private Double subtotal;
    private LocalDateTime createdAt;

    public DetalleVenta() {
        this.descuento = 0.0;
        this.precioTipo = "MINORISTA";
    }

    public DetalleVenta(Variante variante, Integer cantidad, Double precioUnitario, String precioTipo) {
        this();
        this.variante = variante;
        this.varianteId = variante.getId();
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.precioTipo = precioTipo;
        calcularSubtotal();
    }

    public void validate() throws IllegalArgumentException {
        if (ventaId == null) {
            throw new IllegalArgumentException("El ID de la venta es obligatorio");
        }
        validateBasicFields();
    }

    public void validateBasicFields() throws IllegalArgumentException {
        if (varianteId == null) {
            throw new IllegalArgumentException("La variante es obligatoria");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (precioUnitario == null || precioUnitario < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo");
        }
    }

    public void calcularSubtotal() {
        if (cantidad != null && precioUnitario != null) {
            double sub = cantidad * precioUnitario;
            this.subtotal = sub - (descuento != null ? descuento : 0.0);
        }
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVentaId() { return ventaId; }
    public void setVentaId(Integer ventaId) { this.ventaId = ventaId; }
    public Integer getVarianteId() { return varianteId; }
    public void setVarianteId(Integer varianteId) { this.varianteId = varianteId; }
    public Variante getVariante() { return variante; }
    public void setVariante(Variante variante) { 
        this.variante = variante;
        if (variante != null) this.varianteId = variante.getId();
    }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { 
        this.cantidad = cantidad;
        calcularSubtotal();
    }
    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { 
        this.precioUnitario = precioUnitario;
        calcularSubtotal();
    }
    public String getPrecioTipo() { return precioTipo; }
    public void setPrecioTipo(String precioTipo) { this.precioTipo = precioTipo; }
    public Double getDescuento() { return descuento; }
    public void setDescuento(Double descuento) { 
        this.descuento = descuento;
        calcularSubtotal();
    }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetalleVenta that = (DetalleVenta) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
