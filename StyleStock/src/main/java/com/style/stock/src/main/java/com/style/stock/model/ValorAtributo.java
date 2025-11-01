// ============================================
// ValorAtributo.java
// ============================================
package com.style.stock.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo de valor predefinido para atributos tipo LISTA o COLOR
 */
public class ValorAtributo {
    private Integer id;
    private Integer atributoId;
    private String valor;
    private String codigoHex; // Para colores (ej: #FF0000)
    private Integer orden;
    private Boolean activo;
    private LocalDateTime createdAt;

    public ValorAtributo() {
        this.activo = true;
        this.orden = 0;
    }

    public ValorAtributo(String valor) {
        this();
        this.valor = valor;
    }

    public ValorAtributo(Integer atributoId, String valor) {
        this();
        this.atributoId = atributoId;
        this.valor = valor;
    }

    public ValorAtributo(Integer atributoId, String valor, String codigoHex) {
        this();
        this.atributoId = atributoId;
        this.valor = valor;
        this.codigoHex = codigoHex;
    }

    public void validate() throws IllegalArgumentException {
        if (atributoId == null) {
            throw new IllegalArgumentException("El ID del atributo es obligatorio");
        }
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El valor es obligatorio");
        }
        if (valor.length() > 100) {
            throw new IllegalArgumentException("El valor no puede superar 100 caracteres");
        }
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getAtributoId() { return atributoId; }
    public void setAtributoId(Integer atributoId) { this.atributoId = atributoId; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public String getCodigoHex() { return codigoHex; }
    public void setCodigoHex(String codigoHex) { this.codigoHex = codigoHex; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return valor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValorAtributo that = (ValorAtributo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
