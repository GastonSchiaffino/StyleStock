// ============================================
// VarianteAtributo.java
// ============================================
package com.style.stock.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo que relaciona un atributo específico con un valor en una variante
 */
public class VarianteAtributo {
    private Integer id;
    private Integer varianteId;
    private Integer atributoId;
    private String valor;
    private Atributo atributo; // Relación opcional para mostrar nombre del atributo
    private LocalDateTime createdAt;

    public VarianteAtributo() {}

    public VarianteAtributo(Integer atributoId, String valor) {
        this.atributoId = atributoId;
        this.valor = valor;
    }

    public VarianteAtributo(Integer varianteId, Integer atributoId, String valor) {
        this.varianteId = varianteId;
        this.atributoId = atributoId;
        this.valor = valor;
    }

    public void validate() throws IllegalArgumentException {
        if (varianteId == null) {
            throw new IllegalArgumentException("El ID de la variante es obligatorio");
        }
        if (atributoId == null) {
            throw new IllegalArgumentException("El ID del atributo es obligatorio");
        }
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El valor del atributo es obligatorio");
        }
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getVarianteId() { return varianteId; }
    public void setVarianteId(Integer varianteId) { this.varianteId = varianteId; }

    public Integer getAtributoId() { return atributoId; }
    public void setAtributoId(Integer atributoId) { this.atributoId = atributoId; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public Atributo getAtributo() { return atributo; }
    public void setAtributo(Atributo atributo) { this.atributo = atributo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        if (atributo != null) {
            return atributo.getNombre() + ": " + valor;
        }
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarianteAtributo that = (VarianteAtributo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
