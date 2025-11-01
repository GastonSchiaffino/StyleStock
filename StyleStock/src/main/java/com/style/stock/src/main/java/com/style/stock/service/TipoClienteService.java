// ============================================
// TipoClienteService.java
// ============================================
package com.style.stock.service;

import com.style.stock.dao.TipoClienteDAO;
import com.style.stock.exception.*;
import com.style.stock.model.TipoCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TipoClienteService {
    private static final Logger logger = LoggerFactory.getLogger(TipoClienteService.class);
    private final TipoClienteDAO tipoClienteDAO;

    public TipoClienteService() {
        this.tipoClienteDAO = new TipoClienteDAO();
    }

    public TipoCliente crear(TipoCliente tipo) throws ValidationException, DataAccessException {
        logger.debug("Creando tipo de cliente: {}", tipo.getNombre());
        validar(tipo);

        TipoCliente guardado = tipoClienteDAO.save(tipo);
        logger.info("Tipo de cliente creado: {}", guardado.getNombre());
        return guardado;
    }

    public TipoCliente actualizar(TipoCliente tipo) throws ValidationException, DataAccessException, NotFoundException {
        logger.debug("Actualizando tipo de cliente: {}", tipo.getId());

        if (tipo.getId() == null) {
            throw new ValidationException("id", "El ID del tipo de cliente es obligatorio");
        }

        validar(tipo);

        tipoClienteDAO.findById(tipo.getId())
            .orElseThrow(() -> new NotFoundException("Tipo de Cliente", tipo.getId()));

        TipoCliente actualizado = tipoClienteDAO.update(tipo);
        logger.info("Tipo de cliente actualizado: {}", actualizado.getId());
        return actualizado;
    }

    public TipoCliente buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return tipoClienteDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Tipo de Cliente", id));
    }

    public List<TipoCliente> listarTodos() throws DataAccessException {
        return tipoClienteDAO.findAll(true);
    }

    public void eliminar(Integer id) throws DataAccessException, NotFoundException {
        buscarPorId(id);
        tipoClienteDAO.delete(id);
        logger.info("Tipo de cliente eliminado: {}", id);
    }

    private void validar(TipoCliente tipo) throws ValidationException {
        try {
            tipo.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("tipo_cliente", e.getMessage());
        }
    }
}