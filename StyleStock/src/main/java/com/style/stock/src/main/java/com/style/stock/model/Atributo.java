package com.style.stock.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modelo de Atributo (Talle, Color, Sabor, etc.)
 */
public class Atributo {
    private Integer id;
    private String nombre;
    private TipoAtributo tipo;
    private String descripcion;
    private Integer orden;
    private Boolean activo;
    private List<ValorAtributo> valoresPosibles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TipoAtributo {
        TEXTO("TEXTO"),
        LISTA("LISTA"),
        NUMERO("NUMERO"),
        COLOR("COLOR");

        private final String valor;
        TipoAtributo(String valor) { this.valor = valor; }
        public String getValor() { return valor; }
    }

    public Atributo() {
        this.activo = true;
        this.tipo = TipoAtributo.LISTA;
        this.valoresPosibles = new ArrayList<>();
        this.orden = 0;
    }

    public Atributo(String nombre, TipoAtributo tipo) {
        this();
        this.nombre = nombre;
        this.tipo = tipo;
    }

    // Validaciones
    public void validate() throws IllegalArgumentException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del atributo es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de atributo es obligatorio");
        }
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public TipoAtributo getTipo() { return tipo; }
    public void setTipo(TipoAtributo tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public List<ValorAtributo> getValoresPosibles() { return valoresPosibles; }
    public void setValoresPosibles(List<ValorAtributo> valoresPosibles) { 
        this.valoresPosibles = valoresPosibles; 
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Atributo atributo = (Atributo) o;
        return Objects.equals(id, atributo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}