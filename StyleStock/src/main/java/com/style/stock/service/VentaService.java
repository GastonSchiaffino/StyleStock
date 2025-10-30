package com.style.stock.service;

import com.style.stock.dao.*;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// ============================================
// VENTA SERVICE
// ============================================
class VentaService {
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
    public Venta crear(Venta venta) throws BusinessException, ValidationException, DataAccessException, NotFoundException {
        logger.debug("Creando venta para cliente: {}", venta.getClienteId());

        validar(venta);

        // Verificar que el cliente exista
        clienteDAO.findById(venta.getClienteId())
            .orElseThrow(() -> new NotFoundException("Cliente", venta.getClienteId()));

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
        }

        // Validar que los pagos cubran el total
        if (!venta.getPagos().isEmpty()) {
            double totalPagado = venta.getTotalPagado();
            if (totalPagado < venta.getTotal()) {
                throw new BusinessException("El total pagado es menor al total de la venta");
            }
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
                detalle.validate();
            } catch (IllegalArgumentException e) {
                throw new ValidationException("detalle", e.getMessage());
            }
        }

        // Validar pagos
        if (venta.getPagos() != null) {
            for (PagoVenta pago : venta.getPagos()) {
                try {
                    pago.validate();
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("pago", e.getMessage());
                }
            }
        }
    }
}
