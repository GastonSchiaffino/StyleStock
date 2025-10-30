package com.style.stock.model;

import java.util.Objects;

/**
 * Modelo de Tipo de Cliente (Minorista/Mayorista)
 */
public class TipoCliente {
    private Integer id;
    private String nombre;
    private Boolean usaPrecioMayorista;
    private String descripcion;
    private Boolean activo;

    public TipoCliente() {
        this.activo = true;
        this.usaPrecioMayorista = false;
    }

    public TipoCliente(String nombre, Boolean usaPrecioMayorista) {
        this();
        this.nombre = nombre;
        this.usaPrecioMayorista = usaPrecioMayorista;
    }

    // Validaciones
    public void validate() throws IllegalArgumentException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del tipo de cliente es obligatorio");
        }
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getUsaPrecioMayorista() { return usaPrecioMayorista; }
    public void setUsaPrecioMayorista(Boolean usaPrecioMayorista) { 
        this.usaPrecioMayorista = usaPrecioMayorista; 
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TipoCliente that = (TipoCliente) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
