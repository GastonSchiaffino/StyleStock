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

    public List<Categoria> listarTodas() throws DataAccessException {
        return categoriaDAO.findAll(true);
    }

    public List<Atributo> obtenerAtributosDeCategoria(Integer categoriaId) throws DataAccessException {
        return categoriaDAO.findAtributosByCategoria(categoriaId);
    }

    private void validar(Categoria categoria) throws ValidationException {
        try {
            categoria.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("categoria", e.getMessage());
        }
    }
}
