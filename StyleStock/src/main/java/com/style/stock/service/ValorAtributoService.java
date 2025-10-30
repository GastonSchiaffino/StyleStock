// ============================================
// ValorAtributoService.java
// ============================================
package com.style.stock.service;

import com.style.stock.dao.ValorAtributoDAO;
import com.style.stock.exception.*;
import com.style.stock.model.ValorAtributo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ValorAtributoService {
    private static final Logger logger = LoggerFactory.getLogger(ValorAtributoService.class);
    private final ValorAtributoDAO valorAtributoDAO;

    public ValorAtributoService() {
        this.valorAtributoDAO = new ValorAtributoDAO();
    }

    public ValorAtributo crear(ValorAtributo valor) throws ValidationException, DataAccessException {
        logger.debug("Creando valor de atributo: {}", valor.getValor());
        validar(valor);

        ValorAtributo guardado = valorAtributoDAO.save(valor);
        logger.info("Valor de atributo creado: {}", guardado.getValor());
        return guardado;
    }

    public ValorAtributo buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return valorAtributoDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Valor de Atributo", id));
    }

    public List<ValorAtributo> listarPorAtributo(Integer atributoId) throws DataAccessException {
        return valorAtributoDAO.findByAtributo(atributoId);
    }

    public List<ValorAtributo> listarTodosPorAtributo(Integer atributoId) throws DataAccessException {
        return valorAtributoDAO.findAll(atributoId);
    }

    public void eliminar(Integer id) throws DataAccessException, NotFoundException {
        buscarPorId(id);
        valorAtributoDAO.delete(id);
        logger.info("Valor de atributo eliminado: {}", id);
    }

    private void validar(ValorAtributo valor) throws ValidationException {
        try {
            valor.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("valor_atributo", e.getMessage());
        }
    }
}