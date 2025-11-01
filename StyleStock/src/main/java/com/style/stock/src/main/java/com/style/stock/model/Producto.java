// ============================================
// Producto.java - ACTUALIZADO
// ============================================
package com.style.stock.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo de Producto actualizado para sincronizar con BD v2.0
 */
public class Producto {
    private Integer id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private Integer categoriaId;
    private Categoria categoria;
    private String marca;
    private Double precioCosto;
    private Double precioMinorista;
    private Double precioMayorista;
    private String imagenUrl;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Producto() {
        this.activo = true;
        this.precioCosto = 0.0;
        this.precioMinorista = 0.0;
        this.precioMayorista = 0.0;
    }

    public Producto(String codigo, String nombre, Double precioMinorista, Double precioMayorista) {
        this();
        this.codigo = codigo;
        this.nombre = nombre;
        this.precioMinorista = precioMinorista;
        this.precioMayorista = precioMayorista;
    }

    public void validate() throws IllegalArgumentException {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (categoriaId == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        if (precioMinorista == null || precioMinorista < 0) {
            throw new IllegalArgumentException("El precio minorista debe ser mayor o igual a 0");
        }
        if (precioMayorista == null || precioMayorista < 0) {
            throw new IllegalArgumentException("El precio mayorista debe ser mayor o igual a 0");
        }
        if (precioMayorista > precioMinorista) {
            throw new IllegalArgumentException("El precio mayorista no puede ser mayor al minorista");
        }
        if (precioCosto != null && precioCosto < 0) {
            throw new IllegalArgumentException("El precio de costo no puede ser negativo");
        }
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { 
        this.categoria = categoria;
        if (categoria != null) {
            this.categoriaId = categoria.getId();
        }
    }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public Double getPrecioCosto() { return precioCosto; }
    public void setPrecioCosto(Double precioCosto) { this.precioCosto = precioCosto; }

    public Double getPrecioMinorista() { return precioMinorista; }
    public void setPrecioMinorista(Double precioMinorista) { this.precioMinorista = precioMinorista; }

    public Double getPrecioMayorista() { return precioMayorista; }
    public void setPrecioMayorista(Double precioMayorista) { this.precioMayorista = precioMayorista; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return nombre != null ? nombre : codigo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return Objects.equals(id, producto.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
