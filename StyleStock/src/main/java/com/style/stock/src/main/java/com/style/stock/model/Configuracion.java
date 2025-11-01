// ============================================
// Configuracion.java
// ============================================
package com.style.stock.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modelo de configuración general de la aplicación
 * Almacena parámetros personalizables de la empresa
 */
public class Configuracion {
    private String clave;
    private String valor;
    private String descripcion;
    private TipoConfiguracion tipo;
    private LocalDateTime updatedAt;

    public enum TipoConfiguracion {
        TEXTO("TEXTO"),
        NUMERO("NUMERO"),
        BOOLEAN("BOOLEAN");

        private final String valor;
        TipoConfiguracion(String valor) { this.valor = valor; }
        public String getValor() { return valor; }
    }

    public Configuracion() {
        this.tipo = TipoConfiguracion.TEXTO;
    }

    public Configuracion(String clave, String valor) {
        this();
        this.clave = clave;
        this.valor = valor;
    }

    public Configuracion(String clave, String valor, String descripcion, TipoConfiguracion tipo) {
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
        this.tipo = tipo;
    }

    public void validate() throws IllegalArgumentException {
        if (clave == null || clave.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave de configuración es obligatoria");
        }
        if (clave.length() > 100) {
            throw new IllegalArgumentException("La clave no puede superar 100 caracteres");
        }
        if (valor == null) {
            throw new IllegalArgumentException("El valor es obligatorio");
        }
    }

    /**
     * Obtiene el valor como número
     */
    public Integer getValorAsInteger() {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Obtiene el valor como booleano
     */
    public Boolean getValorAsBoolean() {
        return "true".equalsIgnoreCase(valor) || "1".equals(valor);
    }

    /**
     * Obtiene el valor como double
     */
    public Double getValorAsDouble() {
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Getters y Setters
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public TipoConfiguracion getTipo() { return tipo; }
    public void setTipo(TipoConfiguracion tipo) { this.tipo = tipo; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return clave + " = " + valor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Configuracion that = (Configuracion) o;
        return Objects.equals(clave, that.clave);
    }

    @Override
    public int hashCode() { return Objects.hash(clave); }
}
