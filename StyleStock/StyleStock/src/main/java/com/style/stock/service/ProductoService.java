package com.style.stock.service;

import com.style.stock.dao.ProductoDAO;
import com.style.stock.exception.*;
import com.style.stock.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Servicio de negocio para Productos
 * Contiene validaciones y lógica de negocio
 */
public class ProductoService {
    private static final Logger logger = LoggerFactory.getLogger(ProductoService.class);
    private final ProductoDAO productoDAO;

    public ProductoService() {
        this.productoDAO = new ProductoDAO();
    }

    /**
     * Crea un nuevo producto con validaciones
     */
    public Producto crear(Producto producto) throws BusinessException, ValidationException, DataAccessException {
        logger.debug("Creando producto: {}", producto.getCodigo());

        // Validar datos
        validar(producto);

        // Verificar que el código no exista
        if (productoDAO.findByCodigo(producto.getCodigo()).isPresent()) {
            throw new ValidationException("codigo", "Ya existe un producto con el código: " + producto.getCodigo());
        }

        // Guardar
        Producto guardado = productoDAO.save(producto);
        logger.info("Producto creado exitosamente: {} - {}", guardado.getId(), guardado.getCodigo());
        
        return guardado;
    }

    /**
     * Actualiza un producto existente
     */
    public Producto actualizar(Producto producto) throws BusinessException, ValidationException, DataAccessException, NotFoundException {
        logger.debug("Actualizando producto: {}", producto.getId());

        if (producto.getId() == null) {
            throw new ValidationException("id", "El ID del producto es obligatorio para actualizar");
        }

        // Validar datos
        validar(producto);

        // Verificar que exista
        Producto existente = productoDAO.findById(producto.getId())
            .orElseThrow(() -> new NotFoundException("Producto", producto.getId()));

        // Verificar código único (si cambió)
        if (!existente.getCodigo().equals(producto.getCodigo())) {
            if (productoDAO.findByCodigo(producto.getCodigo()).isPresent()) {
                throw new ValidationException("codigo", "Ya existe otro producto con el código: " + producto.getCodigo());
            }
        }

        // Actualizar
        Producto actualizado = productoDAO.update(producto);
        logger.info("Producto actualizado: {}", actualizado.getId());
        
        return actualizado;
    }

    /**
     * Busca un producto por ID
     */
    public Producto buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return productoDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Producto", id));
    }

    /**
     * Obtiene todos los productos activos
     */
    public List<Producto> listarTodos() throws DataAccessException {
        return productoDAO.findAll(true);
    }

    /**
     * Busca productos por descripción
     */
    public List<Producto> buscarPorDescripcion(String descripcion) throws DataAccessException {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return listarTodos();
        }
        return productoDAO.findByDescripcion(descripcion);
    }

    /**
     * Obtiene productos con stock bajo
     */
    public List<Producto> listarStockBajo() throws DataAccessException {
        return productoDAO.findStockBajo();
    }

    /**
     * Elimina (desactiva) un producto
     */
    public void eliminar(Integer id) throws BusinessException, DataAccessException, NotFoundException {
        // Verificar que exista
        Producto producto = buscarPorId(id);

        // Aquí podrías agregar más validaciones de negocio
        // Por ejemplo: verificar que no tenga movimientos recientes, etc.

        productoDAO.delete(id);
        logger.info("Producto eliminado: {}", id);
    }

    /**
     * Ajusta el stock de un producto (ingreso/egreso manual)
     */
    public void ajustarStock(Integer id, Integer cantidad, String motivo)
            throws BusinessException, DataAccessException, NotFoundException {
        
        Producto producto = buscarPorId(id);
        int nuevoStock = producto.getStock() + cantidad;

        if (nuevoStock < 0) {
            throw new BusinessException("El ajuste resultaría en stock negativo");
        }

        productoDAO.updateStock(id, nuevoStock);
        logger.info("Stock ajustado para producto {}: {} -> {}", id, producto.getStock(), nuevoStock);
    }

    /**
     * Verifica si hay stock suficiente
     */
    public boolean verificarStock(Integer productoId, Integer cantidad) throws DataAccessException, NotFoundException {
        Producto producto = buscarPorId(productoId);
        return producto.getStock() >= cantidad;
    }

    /**
     * Valida los datos del producto
     */
    private void validar(Producto producto) throws ValidationException {
        try {
            producto.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("producto", e.getMessage());
        }

        // Validaciones adicionales de negocio
        if (producto.getPrecio() > 1000000) {
            throw new ValidationException("precio", "El precio no puede superar $1.000.000");
        }

        if (producto.getStock() > 100000) {
            throw new ValidationException("stock", "El stock no puede superar 100.000 unidades");
        }
    }

    /**
     * Obtiene el total de productos activos
     */
    public long contarProductos() throws DataAccessException {
        return productoDAO.count();
    }
}

