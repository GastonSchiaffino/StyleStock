package com.style.stock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// ============================================
// METODO PAGO
// ============================================
public class MetodoPago {
    private Integer id;
    private String nombre;
    private Boolean requiereCuotas;
    private Double comisionPorcentaje;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MetodoPago() {
        this.activo = true;
        this.requiereCuotas = false;
        this.comisionPorcentaje = 0.0;
    }

    public void validate() throws IllegalArgumentException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del método de pago es obligatorio");
        }
    }

    public Double calcularComision(Double monto) {
        if (comisionPorcentaje == null || comisionPorcentaje == 0) return 0.0;
        return (monto * comisionPorcentaje) / 100.0;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getRequiereCuotas() { return requiereCuotas; }
    public void setRequiereCuotas(Boolean requiereCuotas) { this.requiereCuotas = requiereCuotas; }
    public Double getComisionPorcentaje() { return comisionPorcentaje; }
    public void setComisionPorcentaje(Double comisionPorcentaje) { this.comisionPorcentaje = comisionPorcentaje; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return nombre; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetodoPago that = (MetodoPago) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
