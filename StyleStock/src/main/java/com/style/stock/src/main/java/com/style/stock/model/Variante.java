// ============================================
// Variante.java - ACTUALIZADO
// ============================================
package com.style.stock.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modelo de Variante con atributos dinámicos
 */
public class Variante {
    private Integer id;
    private Integer productoId;
    private String sku;
    private String codigoBarras;
    private Double precioCosto;
    private Double precioMinorista;
    private Double precioMayorista;
    private Integer stock;
    private Integer stockMinimo;
    private Boolean activo;
    private Producto producto; // Relación opcional
    private List<VarianteAtributo> atributos; // Atributos dinámicos (Talle, Color, etc)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Variante() {
        this.activo = true;
        this.stock = 0;
        this.stockMinimo = 5;
        this.precioCosto = 0.0;
        this.precioMinorista = 0.0;
        this.precioMayorista = 0.0;
        this.atributos = new ArrayList<>();
    }

    public Variante(String sku, Double precioMinorista, Double precioMayorista, Integer stock) {
        this();
        this.sku = sku;
        this.precioMinorista = precioMinorista;
        this.precioMayorista = precioMayorista;
        this.stock = stock;
    }

    public void validate() throws IllegalArgumentException {
        if (productoId == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("El SKU es obligatorio");
        }
        if (precioMinorista == null || precioMinorista < 0) {
            throw new IllegalArgumentException("El precio minorista no puede ser negativo");
        }
        if (precioMayorista == null || precioMayorista < 0) {
            throw new IllegalArgumentException("El precio mayorista no puede ser negativo");
        }
        if (precioMayorista > precioMinorista) {
            throw new IllegalArgumentException("El precio mayorista no puede ser mayor al minorista");
        }
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        if (stockMinimo != null && stockMinimo < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
    }

    /**
     * Retorna descripción completa de la variante con sus atributos
     * Ej: "Remera - Talle: M, Color: Negro"
     */
    public String getDescripcionCompleta() {
        StringBuilder sb = new StringBuilder();
        
        if (producto != null) {
            sb.append(producto.getNombre());
        } else {
            sb.append("Producto desconocido");
        }
        
        if (!atributos.isEmpty()) {
            sb.append(" - ");
            for (int i = 0; i < atributos.size(); i++) {
                VarianteAtributo attr = atributos.get(i);
                if (attr.getAtributo() != null) {
                    sb.append(attr.getAtributo().getNombre()).append(": ").append(attr.getValor());
                } else {
                    sb.append(attr.getValor());
                }
                if (i < atributos.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        
        return sb.toString();
    }

    public boolean isStockBajo() {
        return stock < stockMinimo;
    }

    public void agregarAtributo(VarianteAtributo atributo) {
        if (this.atributos == null) {
            this.atributos = new ArrayList<>();
        }
        this.atributos.add(atributo);
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getProductoId() { return productoId; }
    public void setProductoId(Integer productoId) { this.productoId = productoId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }

    public Double getPrecioCosto() { return precioCosto; }
    public void setPrecioCosto(Double precioCosto) { this.precioCosto = precioCosto; }

    public Double getPrecioMinorista() { return precioMinorista; }
    public void setPrecioMinorista(Double precioMinorista) { this.precioMinorista = precioMinorista; }

    public Double getPrecioMayorista() { return precioMayorista; }
    public void setPrecioMayorista(Double precioMayorista) { this.precioMayorista = precioMayorista; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { 
        this.producto = producto;
        if (producto != null) {
            this.productoId = producto.getId();
        }
    }

    public List<VarianteAtributo> getAtributos() { return atributos; }
    public void setAtributos(List<VarianteAtributo> atributos) { this.atributos = atributos; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return sku; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variante variante = (Variante) o;
        return Objects.equals(id, variante.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
