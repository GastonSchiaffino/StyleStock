// ============================================
// AtributoService.java
// ============================================
package com.style.stock.service;

import com.style.stock.dao.*;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AtributoService {
    private static final Logger logger = LoggerFactory.getLogger(AtributoService.class);
    private final AtributoDAO atributoDAO;
    private final ValorAtributoDAO valorAtributoDAO;

    public AtributoService() {
        this.atributoDAO = new AtributoDAO();
        this.valorAtributoDAO = new ValorAtributoDAO();
    }

    public Atributo crear(Atributo atributo) throws ValidationException, DataAccessException {
        logger.debug("Creando atributo: {}", atributo.getNombre());
        validar(atributo);

        Atributo guardado = atributoDAO.save(atributo);
        logger.info("Atributo creado: {}", guardado.getNombre());
        return guardado;
    }

    public Atributo actualizar(Atributo atributo) throws ValidationException, DataAccessException, NotFoundException {
        logger.debug("Actualizando atributo: {}", atributo.getId());

        if (atributo.getId() == null) {
            throw new ValidationException("id", "El ID del atributo es obligatorio");
        }

        validar(atributo);

        atributoDAO.findById(atributo.getId())
            .orElseThrow(() -> new NotFoundException("Atributo", atributo.getId()));

        Atributo actualizado = atributoDAO.update(atributo);
        logger.info("Atributo actualizado: {}", actualizado.getId());
        return actualizado;
    }

    public Atributo buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return atributoDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Atributo", id));
    }

    public List<Atributo> listarTodos() throws DataAccessException {
        return atributoDAO.findAll(true);
    }

    public List<Atributo> listarTodosIncluyendoInactivos() throws DataAccessException {
        return atributoDAO.findAll(false);
    }

    public void eliminar(Integer id) throws DataAccessException, NotFoundException {
        buscarPorId(id);
        atributoDAO.delete(id);
        logger.info("Atributo eliminado: {}", id);
    }

    public List<ValorAtributo> obtenerValoresPosibles(Integer atributoId) throws DataAccessException {
        return valorAtributoDAO.findByAtributo(atributoId);
    }

    private void validar(Atributo atributo) throws ValidationException {
        try {
            atributo.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("atributo", e.getMessage());
        }
    }
}
