package com.style.stock.service;

import com.style.stock.dao.*;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// ============================================
// VENTA SERVICE
// ============================================
public class VentaService {
    private static final Logger logger = LoggerFactory.getLogger(VentaService.class);
    private final VentaDAO ventaDAO;
    private final VarianteService varianteService;
    private final ClienteDAO clienteDAO;

    public VentaService() {
        this.ventaDAO = new VentaDAO();
        this.varianteService = new VarianteService();
        this.clienteDAO = new ClienteDAO();
    }

    /**
     * Crea una nueva venta con validaciones de stock
     */
    // Reemplazar el método crear() en VentaService.java

    public Venta crear(Venta venta) throws BusinessException, ValidationException, DataAccessException, NotFoundException {
        logger.debug("Creando venta para cliente: {}", venta.getClienteId());

        // IMPORTANTE: Asegurar que clienteId esté presente
        if (venta.getCliente() != null && venta.getClienteId() == null) {
            venta.setClienteId(venta.getCliente().getId());
        }

        // IMPORTANTE: Asegurar que tipoClienteId esté presente
        if (venta.getTipoClienteId() == null && venta.getCliente() != null) {
            venta.setTipoClienteId(venta.getCliente().getTipoClienteId());
        }

        validar(venta);

        // Verificar que el cliente exista
        Cliente cliente = clienteDAO.findById(venta.getClienteId())
                .orElseThrow(() -> new NotFoundException("Cliente", venta.getClienteId()));

        // Setear el cliente completo en la venta
        venta.setCliente(cliente);

        // Verificar stock de cada variante
        for (DetalleVenta detalle : venta.getDetalles()) {
            Variante variante = varianteService.buscarPorId(detalle.getVarianteId());

            if (variante.getStock() < detalle.getCantidad()) {
                throw new InsufficientStockException(
                        variante.getDescripcionCompleta(),
                        variante.getStock(),
                        detalle.getCantidad()
                );
            }

            // Asegurar que el detalle tenga la variante completa
            detalle.setVariante(variante);
        }

        // Validar pagos y determinar estado
        if (!venta.getPagos().isEmpty()) {
            double totalPagado = venta.getTotalPagado();

            if (totalPagado < venta.getTotal()) {
                // Pago parcial - marcar como PENDIENTE
                venta.setEstado(Venta.EstadoVenta.PENDIENTE);
                logger.info("Venta con pago parcial. Total: ${}, Pagado: ${}, Saldo: ${}",
                        venta.getTotal(), totalPagado, venta.getSaldoPendiente());
            } else {
                // Pago completo
                venta.setEstado(Venta.EstadoVenta.COMPLETADA);
            }
        } else {
            // Sin pagos - pendiente
            venta.setEstado(Venta.EstadoVenta.PENDIENTE);
        }

        // Calcular totales
        venta.calcularTotales();

        // Guardar (la transacción se maneja en el DAO)
        Venta guardada = ventaDAO.save(venta);
        logger.info("Venta creada: {} - Total: ${}", guardada.getNumeroComprobante(), guardada.getTotal());

        return guardada;
    }

    public Venta buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return ventaDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Venta", id));
    }

    public List<Venta> listarRecientes(int limite) throws DataAccessException {
        return ventaDAO.findAll(limite);
    }

    public List<Venta> buscarPorRangoFechas(LocalDate desde, LocalDate hasta) throws DataAccessException {
        logger.debug("Buscando ventas desde {} hasta {}", desde, hasta);
        return ventaDAO.findByRangoFechas(desde, hasta);
    }

    public List<Venta> buscarPorCliente(Integer clienteId, int limite) throws DataAccessException {
        logger.debug("Buscando ventas del cliente: {}", clienteId);
        return ventaDAO.findByCliente(clienteId, limite);
    }

    public Venta buscarPorNumeroComprobante(String numero) throws NotFoundException, DataAccessException {
        logger.debug("Buscando venta por número: {}", numero);
        return ventaDAO.findByNumeroComprobante(numero)
                .orElseThrow(() -> new NotFoundException("Venta con número", numero));
    }

    public Map<String, Object> obtenerEstadisticasPeriodo(LocalDate desde, LocalDate hasta) throws DataAccessException {
        logger.debug("Obteniendo estadísticas del período {} - {}", desde, hasta);
        return ventaDAO.getEstadisticasPeriodo(desde, hasta);
    }

    public void anular(Integer id) throws BusinessException, DataAccessException, NotFoundException {
        logger.debug("Anulando venta: {}", id);

        Venta venta = buscarPorId(id);

        if (venta.getEstado() == Venta.EstadoVenta.ANULADA) {
            throw new BusinessException("La venta ya está anulada");
        }

        ventaDAO.anularVenta(id);
        logger.info("Venta anulada: {}", id);
    }

    private void validar(Venta venta) throws ValidationException {
        try {
            venta.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("venta", e.getMessage());
        }

        // Validar detalles
        for (DetalleVenta detalle : venta.getDetalles()) {
            try {
                detalle.validateBasicFields();
            } catch (IllegalArgumentException e) {
                throw new ValidationException("detalle", e.getMessage());
            }
        }

        // ✅ Validar solo campos básicos de pagos (sin ventaId)
        if (venta.getPagos() != null) {
            for (PagoVenta pago : venta.getPagos()) {
                try {
                    pago.validateBasicFields();
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("pago", e.getMessage());
                }
            }
        }
    }

    public List<Map<String, Object>> obtenerProductosMasVendidos(
            LocalDate desde, LocalDate hasta, int limite, Integer categoriaId)
            throws DataAccessException {
        return ventaDAO.findProductosMasVendidos(desde, hasta, limite, categoriaId);
    }

    /**
     * Registra un pago adicional a una venta pendiente
     */
    public Venta registrarPago(Integer ventaId, PagoVenta pago)
            throws BusinessException, DataAccessException, NotFoundException {

        logger.debug("Registrando pago para venta: {}", ventaId);

        Venta venta = buscarPorId(ventaId);

        if (venta.getEstado() == Venta.EstadoVenta.ANULADA) {
            throw new BusinessException("No se pueden registrar pagos en ventas anuladas");
        }

        if (venta.getEstado() == Venta.EstadoVenta.COMPLETADA) {
            throw new BusinessException("Esta venta ya está completamente pagada");
        }

        // Validar que el pago no exceda el saldo
        double saldoPendiente = venta.getSaldoPendiente();
        if (pago.getMonto() > saldoPendiente + 0.01) { // Tolerancia de 1 centavo
            throw new BusinessException(
                    String.format("El monto ($%.2f) excede el saldo pendiente ($%.2f)",
                            pago.getMonto(), saldoPendiente)
            );
        }

        // Registrar el pago
        pago.setVentaId(ventaId);
        ventaDAO.registrarPago(pago);

        // Recargar la venta
        venta = buscarPorId(ventaId);

        // Actualizar estado si se completó el pago
        if (!venta.tieneSaldo()) {
            ventaDAO.actualizarEstado(ventaId, Venta.EstadoVenta.COMPLETADA);
            venta.setEstado(Venta.EstadoVenta.COMPLETADA);
            logger.info("Venta {} completada. Pago final registrado", venta.getNumeroComprobante());
        } else {
            logger.info("Pago registrado. Saldo restante: ${}", venta.getSaldoPendiente());
        }

        return venta;
    }
}
