package com.style.stock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// ============================================
// CLIENTE
// ============================================
class Cliente {
    private Integer id;
    private String nombre;
    private String apellido;
    private String direccion;
    private String telefono;
    private String email;
    private String cuit;
    private Integer tipoClienteId;
    private TipoCliente tipoCliente;
    private String notas;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Cliente() {
        this.activo = true;
        this.tipoClienteId = 1; // Minorista por defecto
    }

    public void validate() throws IllegalArgumentException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (cuit != null && !cuit.trim().isEmpty()) {
            String cuitLimpio = cuit.replaceAll("[^0-9]", "");
            if (cuitLimpio.length() != 11) {
                throw new IllegalArgumentException("El CUIT debe tener 11 dígitos");
            }
        }
    }

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder(nombre);
        if (apellido != null && !apellido.isEmpty()) {
            sb.append(" ").append(apellido);
        }
        return sb.toString();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }
    public Integer getTipoClienteId() { return tipoClienteId; }
    public void setTipoClienteId(Integer tipoClienteId) { this.tipoClienteId = tipoClienteId; }
    public TipoCliente getTipoCliente() { return tipoCliente; }
    public void setTipoCliente(TipoCliente tipoCliente) { 
        this.tipoCliente = tipoCliente;
        if (tipoCliente != null) this.tipoClienteId = tipoCliente.getId();
    }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return getNombreCompleto(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return Objects.equals(id, cliente.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
