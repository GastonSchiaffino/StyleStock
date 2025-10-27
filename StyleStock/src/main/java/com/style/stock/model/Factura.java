package com.style.stock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modelo de Factura con validaciones
 */
public class Factura {
    private Integer id;
    private String numeroFactura;
    private Integer clienteId;
    private Cliente cliente;
    private LocalDate fecha;
    private Double subtotal;
    private Double descuento;
    private Double total;
    private TipoFactura tipo;
    private EstadoFactura estado;
    private String notas;
    private List<DetalleFactura> detalles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TipoFactura {
        A("A"), B("B"), C("C");
        private final String valor;
        TipoFactura(String valor) { this.valor = valor; }
        public String getValor() { return valor; }
    }

    public enum EstadoFactura {
        EMITIDA("EMITIDA"), ANULADA("ANULADA"), PAGADA("PAGADA");
        private final String valor;
        EstadoFactura(String valor) { this.valor = valor; }
        public String getValor() { return valor; }
    }

    public Factura() {
        this.fecha = LocalDate.now();
        this.tipo = TipoFactura.A;
        this.estado = EstadoFactura.EMITIDA;
        this.detalles = new ArrayList<>();
        this.subtotal = 0.0;
        this.descuento = 0.0;
        this.total = 0.0;
    }

    // Validaciones
    public void validate() throws IllegalArgumentException {
        if (clienteId == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La factura debe tener al menos un ítem");
        }
        if (total == null || total < 0) {
            throw new IllegalArgumentException("El total no puede ser negativo");
        }
    }

    public void calcularTotales() {
        this.subtotal = detalles.stream()
            .mapToDouble(DetalleFactura::getSubtotal)
            .sum();
        this.total = this.subtotal - (this.descuento != null ? this.descuento : 0.0);
    }

    public void agregarDetalle(DetalleFactura detalle) {
        if (this.detalles == null) {
            this.detalles = new ArrayList<>();
        }
        this.detalles.add(detalle);
        calcularTotales();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { 
        this.cliente = cliente;
        if (cliente != null) {
            this.clienteId = cliente.getId();
        }
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getDescuento() { return descuento; }
    public void setDescuento(Double descuento) { this.descuento = descuento; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public TipoFactura getTipo() { return tipo; }
    public void setTipo(TipoFactura tipo) { this.tipo = tipo; }

    public EstadoFactura getEstado() { return estado; }
    public void setEstado(EstadoFactura estado) { this.estado = estado; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public List<DetalleFactura> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleFactura> detalles) { this.detalles = detalles; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Factura factura = (Factura) o;
        return Objects.equals(id, factura.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

