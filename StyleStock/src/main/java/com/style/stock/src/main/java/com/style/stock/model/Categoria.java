package com.style.stock.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modelo de Categoría de productos
 * Define agrupaciones de productos y sus atributos asociados
 */
public class Categoria {
    private Integer id;
    private String nombre;
    private String descripcion;
    private Boolean requiereVariantes; // Si necesita combinaciones de atributos
    private Boolean activo;
    private List<Atributo> atributos; // Atributos asociados a esta categoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Categoria() {
        this.activo = true;
        this.requiereVariantes = true;
        this.atributos = new ArrayList<>();
    }

    public Categoria(String nombre) {
        this();
        this.nombre = nombre;
    }

    public Categoria(String nombre, String descripcion) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Categoria(String nombre, String descripcion, Boolean requiereVariantes) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.requiereVariantes = requiereVariantes;
    }

    public void validate() throws IllegalArgumentException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        if (nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar 100 caracteres");
        }
    }

    /**
     * Agrega un atributo a la categoría
     */
    public void agregarAtributo(Atributo atributo) {
        if (this.atributos == null) {
            this.atributos = new ArrayList<>();
        }
        if (!this.atributos.contains(atributo)) {
            this.atributos.add(atributo);
        }
    }

    /**
     * Elimina un atributo de la categoría
     */
    public void eliminarAtributo(Atributo atributo) {
        if (this.atributos != null) {
            this.atributos.remove(atributo);
        }
    }

    /**
     * Verifica si la categoría tiene un atributo específico
     */
    public boolean tieneAtributo(Integer atributoId) {
        if (this.atributos == null) {
            return false;
        }
        return this.atributos.stream()
                .anyMatch(a -> a.getId().equals(atributoId));
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getRequiereVariantes() {
        return requiereVariantes;
    }

    public void setRequiereVariantes(Boolean requiereVariantes) {
        this.requiereVariantes = requiereVariantes;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public List<Atributo> getAtributos() {
        return atributos;
    }

    public void setAtributos(List<Atributo> atributos) {
        this.atributos = atributos;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categoria categoria = (Categoria) o;
        return Objects.equals(id, categoria.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}