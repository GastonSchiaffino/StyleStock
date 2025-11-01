// ============================================
// MetodoPagoService.java
// ============================================
package com.style.stock.service;

import com.style.stock.dao.MetodoPagoDAO;
import com.style.stock.exception.*;
import com.style.stock.model.MetodoPago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MetodoPagoService {
    private static final Logger logger = LoggerFactory.getLogger(MetodoPagoService.class);
    private final MetodoPagoDAO metodoPagoDAO;

    public MetodoPagoService() {
        this.metodoPagoDAO = new MetodoPagoDAO();
    }

    public MetodoPago crear(MetodoPago metodo) throws ValidationException, DataAccessException {
        logger.debug("Creando método de pago: {}", metodo.getNombre());
        validar(metodo);

        MetodoPago guardado = metodoPagoDAO.save(metodo);
        logger.info("Método de pago creado: {}", guardado.getNombre());
        return guardado;
    }

    public MetodoPago actualizar(MetodoPago metodo) throws ValidationException, DataAccessException, NotFoundException {
        logger.debug("Actualizando método de pago: {}", metodo.getId());

        if (metodo.getId() == null) {
            throw new ValidationException("id", "El ID del método de pago es obligatorio");
        }

        validar(metodo);

        metodoPagoDAO.findById(metodo.getId())
            .orElseThrow(() -> new NotFoundException("Método de Pago", metodo.getId()));

        MetodoPago actualizado = metodoPagoDAO.update(metodo);
        logger.info("Método de pago actualizado: {}", actualizado.getId());
        return actualizado;
    }

    public MetodoPago buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return metodoPagoDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Método de Pago", id));
    }

    public List<MetodoPago> listarTodos() throws DataAccessException {
        return metodoPagoDAO.findAll(true);
    }

    public void eliminar(Integer id) throws DataAccessException, NotFoundException {
        buscarPorId(id);
        metodoPagoDAO.delete(id);
        logger.info("Método de pago eliminado: {}", id);
    }

    private void validar(MetodoPago metodo) throws ValidationException {
        try {
            metodo.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("metodo_pago", e.getMessage());
        }
    }
}