package com.style.stock.service;

import com.style.stock.dao.*;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// ============================================
// CATEGORIA SERVICE
// ============================================
public class CategoriaService {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaService.class);
    private final CategoriaDAO categoriaDAO;

    public CategoriaService() {
        this.categoriaDAO = new CategoriaDAO();
    }

    public Categoria crear(Categoria categoria) throws ValidationException, DataAccessException {
        logger.debug("Creando categoría: {}", categoria.getNombre());
        validar(categoria);

        Categoria guardada = categoriaDAO.save(categoria);
        logger.info("Categoría creada: {}", guardada.getNombre());
        return guardada;
    }

    public Categoria actualizar(Categoria categoria) throws ValidationException, DataAccessException, NotFoundException {
        logger.debug("Actualizando categoría: {}", categoria.getId());

        if (categoria.getId() == null) {
            throw new ValidationException("id", "El ID de la categoría es obligatorio");
        }

        validar(categoria);

        categoriaDAO.findById(categoria.getId())
                .orElseThrow(() -> new NotFoundException("Categoría", categoria.getId()));

        Categoria actualizada = categoriaDAO.update(categoria);
        logger.info("Categoría actualizada: {}", actualizada.getId());
        return actualizada;
    }

    public void eliminar(Integer id) throws BusinessException, DataAccessException, NotFoundException {
        logger.debug("Eliminando categoría: {}", id);

        categoriaDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría", id));

        categoriaDAO.delete(id);
        logger.info("Categoría eliminada: {}", id);
    }

    public List<Categoria> listarTodas() throws DataAccessException {
        return categoriaDAO.findAll(true);
    }

    public List<Atributo> obtenerAtributosDeCategoria(Integer categoriaId) throws DataAccessException {
        return categoriaDAO.findAtributosByCategoria(categoriaId);
    }

    public void asociarAtributo(Integer categoriaId, Integer atributoId, boolean requerido, int orden)
            throws DataAccessException {
        logger.debug("Asociando atributo {} a categoría {}", atributoId, categoriaId);
        categoriaDAO.asociarAtributo(categoriaId, atributoId, requerido, orden);
        logger.info("Atributo asociado exitosamente");
    }

    public void desasociarAtributo(Integer categoriaId, Integer atributoId) throws DataAccessException {
        logger.debug("Desasociando atributo {} de categoría {}", atributoId, categoriaId);
        categoriaDAO.desasociarAtributo(categoriaId, atributoId);
        logger.info("Atributo desasociado exitosamente");
    }

    private void validar(Categoria categoria) throws ValidationException {
        try {
            categoria.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("categoria", e.getMessage());
        }
    }
}
