package com.style.stock.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo de DetalleFactura con validaciones
 */
public class DetalleFactura {
    private Integer id;
    private Integer facturaId;
    private Integer productoId;
    private Producto producto;
    private Integer cantidad;
    private Double precioUnitario;
    private Double descuento;
    private Double subtotal;
    private LocalDateTime createdAt;

    public DetalleFactura() {
        this.descuento = 0.0;
    }

    public DetalleFactura(Producto producto, Integer cantidad, Double precioUnitario) {
        this();
        this.producto = producto;
        this.productoId = producto.getId();
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        calcularSubtotal();
    }

    // Validaciones
    public void validate() throws IllegalArgumentException {
        if (productoId == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
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

    public Integer getFacturaId() { return facturaId; }
    public void setFacturaId(Integer facturaId) { this.facturaId = facturaId; }

    public Integer getProductoId() { return productoId; }
    public void setProductoId(Integer productoId) { this.productoId = productoId; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { 
        this.producto = producto;
        if (producto != null) {
            this.productoId = producto.getId();
        }
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
        DetalleFactura that = (DetalleFactura) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
