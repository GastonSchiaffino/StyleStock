package com.style.stock.service;

import com.style.stock.dao.FacturaDAO;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de negocio para Facturas
 */
public class FacturaService {
    private static final Logger logger = LoggerFactory.getLogger(FacturaService.class);
    private final FacturaDAO facturaDAO;
    private final ProductoService productoService;
    private final ClienteService clienteService;

    public FacturaService() {
        this.facturaDAO = new FacturaDAO();
        this.productoService = new ProductoService();
        this.clienteService = new ClienteService();
    }

    /**
     * Crea una nueva factura con validaciones de stock
     */
    public Factura crear(Factura factura) throws BusinessException, ValidationException, DataAccessException, NotFoundException {
        logger.debug("Creando factura para cliente: {}", factura.getClienteId());

        // Validar
        validar(factura);

        // Verificar que el cliente exista
        clienteService.buscarPorId(factura.getClienteId());

        // Verificar stock de cada producto
        for (DetalleFactura detalle : factura.getDetalles()) {
            Producto producto = productoService.buscarPorId(detalle.getProductoId());
            
            if (producto.getStock() < detalle.getCantidad()) {
                throw new InsufficientStockException(
                    producto.getDescripcion(),
                    producto.getStock(),
                    detalle.getCantidad()
                );
            }
        }

        // Calcular totales
        factura.calcularTotales();

        // Guardar (la transacción se maneja en el DAO)
        Factura guardada = facturaDAO.save(factura);
        logger.info("Factura creada: {} - Total: ${}", guardada.getNumeroFactura(), guardada.getTotal());
        
        return guardada;
    }

    public Factura buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return facturaDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Factura", id));
    }

    public List<Factura> listarTodas() throws DataAccessException {
        return facturaDAO.findAll();
    }

    public List<Factura> listarPorCliente(Integer clienteId) throws DataAccessException {
        return facturaDAO.findByCliente(clienteId);
    }

    public List<Factura> listarPorPeriodo(LocalDate desde, LocalDate hasta) throws DataAccessException {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        return facturaDAO.findByFechaRange(desde, hasta);
    }

    public void anular(Integer id) throws BusinessException, DataAccessException, NotFoundException {
        logger.debug("Anulando factura: {}", id);

        Factura factura = buscarPorId(id);

        if (factura.getEstado() == Factura.EstadoFactura.ANULADA) {
            throw new BusinessException("La factura ya está anulada");
        }

        facturaDAO.anularFactura(id);
        logger.info("Factura anulada: {}", id);
    }

    private void validar(Factura factura) throws ValidationException {
        try {
            factura.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("factura", e.getMessage());
        }

        // Validar detalles
        for (DetalleFactura detalle : factura.getDetalles()) {
            try {
                detalle.validate();
            } catch (IllegalArgumentException e) {
                throw new ValidationException("detalle", e.getMessage());
            }
        }
    }

    /**
     * Calcula el total de ventas en un período
     */
    public double calcularVentasPeriodo(LocalDate desde, LocalDate hasta) throws DataAccessException {
        List<Factura> facturas = listarPorPeriodo(desde, hasta);
        return facturas.stream()
            .filter(f -> f.getEstado() != Factura.EstadoFactura.ANULADA)
            .mapToDouble(Factura::getTotal)
            .sum();
    }
}