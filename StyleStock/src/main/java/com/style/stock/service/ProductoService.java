package com.style.stock.service;

import com.style.stock.dao.*;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// ============================================
// PRODUCTO SERVICE
// ============================================
public class ProductoService {
    private static final Logger logger = LoggerFactory.getLogger(ProductoService.class);
    private final ProductoDAO productoDAO;

    public ProductoService() {
        this.productoDAO = new ProductoDAO();
    }

    public Producto crear(Producto producto) throws BusinessException, ValidationException, DataAccessException {
        logger.debug("Creando producto: {}", producto.getCodigo());
        validar(producto);

        if (productoDAO.findByCodigo(producto.getCodigo()).isPresent()) {
            throw new ValidationException("codigo", "Ya existe un producto con el código: " + producto.getCodigo());
        }

        Producto guardado = productoDAO.save(producto);
        logger.info("Producto creado: {} - {}", guardado.getId(), guardado.getCodigo());
        return guardado;
    }

    public Producto actualizar(Producto producto) throws BusinessException, ValidationException, DataAccessException, NotFoundException {
        logger.debug("Actualizando producto: {}", producto.getId());

        if (producto.getId() == null) {
            throw new ValidationException("id", "El ID del producto es obligatorio");
        }

        validar(producto);

        Producto existente = productoDAO.findById(producto.getId())
            .orElseThrow(() -> new NotFoundException("Producto", producto.getId()));

        if (!existente.getCodigo().equals(producto.getCodigo())) {
            if (productoDAO.findByCodigo(producto.getCodigo()).isPresent()) {
                throw new ValidationException("codigo", "Ya existe otro producto con el código: " + producto.getCodigo());
            }
        }

        Producto actualizado = productoDAO.update(producto);
        logger.info("Producto actualizado: {}", actualizado.getId());
        return actualizado;
    }

    public Producto buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return productoDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Producto", id));
    }

    public List<Producto> listarTodos() throws DataAccessException {
        return productoDAO.findAll(true);
    }

    public List<Producto> listarPorCategoria(Integer categoriaId) throws DataAccessException {
        return productoDAO.findByCategoria(categoriaId);
    }

    public void eliminar(Integer id) throws BusinessException, DataAccessException, NotFoundException {
        buscarPorId(id);
        productoDAO.delete(id);
        logger.info("Producto eliminado: {}", id);
    }

    private void validar(Producto producto) throws ValidationException {
        try {
            producto.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("producto", e.getMessage());
        }
    }
}