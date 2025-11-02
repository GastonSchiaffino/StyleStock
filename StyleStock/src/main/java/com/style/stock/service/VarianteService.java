package com.style.stock.service;

import com.style.stock.dao.*;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VarianteService {
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

        productoDAO.findById(variante.getProductoId())
                .orElseThrow(() -> new BusinessException("El producto no existe"));

        if (varianteDAO.findBySku(variante.getSku()).isPresent()) {
            throw new ValidationException("sku", "Ya existe una variante con el SKU: " + variante.getSku());
        }

        Variante guardada = varianteDAO.save(variante);
        logger.info("Variante creada: {}", guardada.getSku());
        return guardada;
    }

    public Variante actualizar(Variante variante) throws BusinessException, ValidationException, DataAccessException, NotFoundException {
        logger.debug("Actualizando variante: {}", variante.getId());

        if (variante.getId() == null) {
            throw new ValidationException("id", "El ID de la variante es obligatorio");
        }

        validar(variante);

        // Verificar que existe
        varianteDAO.findById(variante.getId())
                .orElseThrow(() -> new NotFoundException("Variante", variante.getId()));

        Variante actualizada = varianteDAO.update(variante);
        logger.info("Variante actualizada: {}", actualizada.getSku());
        return actualizada;
    }

    public void eliminar(Integer id) throws BusinessException, DataAccessException, NotFoundException {
        logger.debug("Eliminando variante: {}", id);

        Variante variante = buscarPorId(id);

        // Verificar si tiene ventas asociadas (opcional - puedes comentar esto si no importa)
        // En ese caso, solo la desactiva en lugar de error

        varianteDAO.delete(id);
        logger.info("Variante eliminada: {}", id);
    }

    public Variante buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return varianteDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Variante", id));
    }

    public Variante buscarPorSku(String sku) throws NotFoundException, DataAccessException {
        return varianteDAO.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Variante con SKU", sku));
    }

    /**
     * Búsqueda de variantes por texto - IMPLEMENTACIÓN COMPLETA
     */
    public List<Variante> buscarPorTexto(String termino) throws DataAccessException {
        if (termino == null || termino.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String busqueda = termino.trim().toLowerCase();
        List<Variante> resultados = new ArrayList<>();

        try {
            // 1. Búsqueda exacta por SKU
            try {
                Optional<Variante> porSku = varianteDAO.findBySku(busqueda.toUpperCase());
                if (porSku.isPresent()) {
                    resultados.add(porSku.get());
                    return resultados;
                }
            } catch (DataAccessException e) {
                logger.debug("No se encontró variante con SKU exacto: {}", busqueda);
            }

            // 2. Búsqueda parcial: obtener todos los productos activos y filtrar
            List<Producto> productos = productoDAO.findAll(true);

            for (Producto producto : productos) {
                // Filtrar productos que coincidan con el término
                boolean coincide = producto.getCodigo().toLowerCase().contains(busqueda) ||
                        producto.getNombre().toLowerCase().contains(busqueda) ||
                        (producto.getMarca() != null && producto.getMarca().toLowerCase().contains(busqueda));

                if (coincide) {
                    // Obtener todas las variantes de este producto
                    List<Variante> variantes = varianteDAO.findByProducto(producto.getId());
                    for (Variante v : variantes) {
                        v.setProducto(producto); // Setear el producto para descripción completa
                        resultados.add(v);
                    }
                }
            }

            // 3. Si no hay resultados, buscar por SKU parcial en todas las variantes
            if (resultados.isEmpty()) {
                List<Producto> todosProductos = productoDAO.findAll(true);
                for (Producto p : todosProductos) {
                    List<Variante> variantes = varianteDAO.findByProducto(p.getId());
                    for (Variante v : variantes) {
                        if (v.getSku().toLowerCase().contains(busqueda) ||
                                (v.getCodigoBarras() != null && v.getCodigoBarras().contains(busqueda))) {
                            v.setProducto(p);
                            resultados.add(v);
                        }
                    }
                }
            }

            logger.debug("Búsqueda '{}' retornó {} resultados", termino, resultados.size());
            return resultados;

        } catch (DataAccessException e) {
            logger.error("Error en búsqueda de variantes", e);
            throw e;
        }
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

        if (variante.getPrecioMayorista() > variante.getPrecioMinorista()) {
            throw new ValidationException("precio", "El precio mayorista no puede ser mayor al minorista");
        }
    }
}