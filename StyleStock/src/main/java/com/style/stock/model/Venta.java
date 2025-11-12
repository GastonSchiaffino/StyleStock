package com.style.stock.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// ============================================
// VENTA
// ============================================
public class Venta {
    private Integer id;
    private String numeroComprobante;
    private Integer clienteId;
    private Cliente cliente;
    private Integer tipoClienteId;
    private LocalDate fecha;
    private LocalTime hora;
    private Double subtotal;
    private Double descuento;
    private Double total;
    private TipoComprobante tipoComprobante;
    private TipoVenta tipoVenta;
    private EstadoVenta estado;
    private String notas;
    private String vendedor;
    private List<DetalleVenta> detalles;
    private List<PagoVenta> pagos;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TipoComprobante {
        TICKET("TICKET"),
        FACTURA_A("FACTURA_A"),
        FACTURA_B("FACTURA_B"),
        FACTURA_C("FACTURA_C");

        private final String valor;
        TipoComprobante(String valor) { this.valor = valor; }
        public String getValor() { return valor; }
    }

    public enum TipoVenta {
        MINORISTA("MINORISTA"),
        MAYORISTA("MAYORISTA");

        private final String valor;
        TipoVenta(String valor) { this.valor = valor; }
        public String getValor() { return valor; }
    }

    public enum EstadoVenta {
        COMPLETADA("COMPLETADA"),
        ANULADA("ANULADA"),
        PENDIENTE("PENDIENTE");

        private final String valor;
        EstadoVenta(String valor) { this.valor = valor; }
        public String getValor() { return valor; }
    }

    public Venta() {
        this.fecha = LocalDate.now();
        this.hora = LocalTime.now();
        this.tipoComprobante = TipoComprobante.TICKET;
        this.tipoVenta = TipoVenta.MINORISTA;
        this.estado = EstadoVenta.COMPLETADA;
        this.subtotal = 0.0;
        this.descuento = 0.0;
        this.total = 0.0;
        this.detalles = new ArrayList<>();
        this.pagos = new ArrayList<>();
    }

    public void validate() throws IllegalArgumentException {
        if (clienteId == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La venta debe tener al menos un producto");
        }
        if (total == null || total < 0) {
            throw new IllegalArgumentException("El total no puede ser negativo");
        }
        
        // Validar que requiera CUIT si es factura A o B
        if ((tipoComprobante == TipoComprobante.FACTURA_A || tipoComprobante == TipoComprobante.FACTURA_B) 
            && cliente != null && (cliente.getCuit() == null || cliente.getCuit().isEmpty())) {
            throw new IllegalArgumentException("Las facturas A y B requieren CUIT del cliente");
        }
    }

    public void calcularTotales() {
        this.subtotal = detalles.stream().mapToDouble(DetalleVenta::getSubtotal).sum();
        this.total = this.subtotal - (this.descuento != null ? this.descuento : 0.0);
    }

    public void agregarDetalle(DetalleVenta detalle) {
        if (this.detalles == null) this.detalles = new ArrayList<>();
        this.detalles.add(detalle);
        calcularTotales();
    }

    public void agregarPago(PagoVenta pago) {
        if (this.pagos == null) this.pagos = new ArrayList<>();
        this.pagos.add(pago);
    }

    public Double getTotalPagado() {
        if (pagos == null) return 0.0;
        return pagos.stream().mapToDouble(PagoVenta::getMonto).sum();
    }

    public boolean estaTotalmentePagada() {
        return getTotalPagado() >= total;
    }

    public Double getSaldoPendiente() {
        return total - getTotalPagado();
    }

    public boolean tieneSaldo() {
        return getSaldoPendiente() > 0.01; // Tolerancia de 1 centavo
    }
    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { 
        this.cliente = cliente;
        if (cliente != null) {
            this.clienteId = cliente.getId();
            this.tipoClienteId = cliente.getTipoClienteId();
        }
    }
    public Integer getTipoClienteId() { return tipoClienteId; }
    public void setTipoClienteId(Integer tipoClienteId) { this.tipoClienteId = tipoClienteId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public Double getDescuento() { return descuento; }
    public void setDescuento(Double descuento) { this.descuento = descuento; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public TipoComprobante getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(TipoComprobante tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public TipoVenta getTipoVenta() { return tipoVenta; }
    public void setTipoVenta(TipoVenta tipoVenta) { this.tipoVenta = tipoVenta; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getVendedor() { return vendedor; }
    public void setVendedor(String vendedor) { this.vendedor = vendedor; }
    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
    public List<PagoVenta> getPagos() { return pagos; }
    public void setPagos(List<PagoVenta> pagos) { this.pagos = pagos; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Venta venta = (Venta) o;
        return Objects.equals(id, venta.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}