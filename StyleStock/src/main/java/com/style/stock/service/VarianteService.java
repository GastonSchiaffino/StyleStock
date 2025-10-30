package com.style.stock.service;

import com.style.stock.dao.*;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// ============================================
// VARIANTE SERVICE
// ============================================
class VarianteService {
    private static final Logger logger = LoggerFactory.getLogger(VarianteService.class);
    private final VarianteDAO varianteDAO;
    private final ProductoDAO productoDAO;

    public VarianteService() {
        this.varianteDAO = new VarianteDAO();
        this.productoDAO = new ProductoDAO();
    }

    public Variante crear(Variante variante) throws BusinessException, ValidationException, DataAccessException {
        logger.debug("Creando variante: {}", variante.getSku());
        validar(variante);

        // Verificar que el producto exista
        productoDAO.findById(variante.getProductoId())
            .orElseThrow(() -> new BusinessException("El producto no existe"));

        // Verificar SKU único
        if (varianteDAO.findBySku(variante.getSku()).isPresent()) {
            throw new ValidationException("sku", "Ya existe una variante con el SKU: " + variante.getSku());
        }

        Variante guardada = varianteDAO.save(variante);
        logger.info("Variante creada: {}", guardada.getSku());
        return guardada;
    }

    public Variante buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return varianteDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Variante", id));
    }

    public Variante buscarPorSku(String sku) throws NotFoundException, DataAccessException {
        return varianteDAO.findBySku(sku)
            .orElseThrow(() -> new NotFoundException("Variante con SKU", sku));
    }

    public List<Variante> listarPorProducto(Integer productoId) throws DataAccessException {
        return varianteDAO.findByProducto(productoId);
    }

    public List<Variante> listarStockBajo() throws DataAccessException {
        return varianteDAO.findStockBajo();
    }

    public void ajustarStock(Integer id, Integer cantidad, String motivo) 
            throws BusinessException, DataAccessException, NotFoundException {
        Variante variante = buscarPorId(id);
        int nuevoStock = variante.getStock() + cantidad;

        if (nuevoStock < 0) {
            throw new BusinessException("El ajuste resultaría en stock negativo");
        }

        varianteDAO.updateStock(id, nuevoStock);
        logger.info("Stock ajustado para variante {}: {} -> {}", id, variante.getStock(), nuevoStock);
    }

    public boolean verificarStock(Integer varianteId, Integer cantidad) 
            throws DataAccessException, NotFoundException {
        Variante variante = buscarPorId(varianteId);
        return variante.getStock() >= cantidad;
    }

    private void validar(Variante variante) throws ValidationException {
        try {
            variante.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("variante", e.getMessage());
        }

        // Validar que si hereda precios del producto, estos sean válidos
        if (variante.getPrecioMayorista() > variante.getPrecioMinorista()) {
            throw new ValidationException("precio", "El precio mayorista no puede ser mayor al minorista");
        }
    }
}
