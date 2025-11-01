package com.style.stock.service;

import com.style.stock.dao.*;
import com.style.stock.exception.*;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// ============================================
// CLIENTE SERVICE
// ============================================
public class ClienteService {
    private static final Logger logger = LoggerFactory.getLogger(ClienteService.class);
    private final ClienteDAO clienteDAO;

    public ClienteService() {
        this.clienteDAO = new ClienteDAO();
    }

    public Cliente crear(Cliente cliente) throws ValidationException, DataAccessException {
        logger.debug("Creando cliente: {}", cliente.getNombre());
        validar(cliente);

        Cliente guardado = clienteDAO.save(cliente);
        logger.info("Cliente creado: {} - {}", guardado.getId(), guardado.getNombreCompleto());
        return guardado;
    }

    public Cliente actualizar(Cliente cliente) throws ValidationException, DataAccessException, NotFoundException {
        logger.debug("Actualizando cliente: {}", cliente.getId());

        if (cliente.getId() == null) {
            throw new ValidationException("id", "El ID del cliente es obligatorio");
        }

        validar(cliente);

        clienteDAO.findById(cliente.getId())
            .orElseThrow(() -> new NotFoundException("Cliente", cliente.getId()));

        Cliente actualizado = clienteDAO.update(cliente);
        logger.info("Cliente actualizado: {}", actualizado.getId());
        return actualizado;
    }

    public Cliente buscarPorId(Integer id) throws NotFoundException, DataAccessException {
        return clienteDAO.findById(id)
            .orElseThrow(() -> new NotFoundException("Cliente", id));
    }

    public List<Cliente> listarTodos() throws DataAccessException {
        return clienteDAO.findAll(true);
    }

    public List<Cliente> buscarPorNombre(String nombre) throws DataAccessException {
        if (nombre == null || nombre.trim().isEmpty()) {
            return listarTodos();
        }
        return clienteDAO.buscarPorNombre(nombre);
    }

    public void eliminar(Integer id) throws DataAccessException, NotFoundException {
        buscarPorId(id);
        clienteDAO.delete(id);
        logger.info("Cliente eliminado: {}", id);
    }

    private void validar(Cliente cliente) throws ValidationException {
        try {
            cliente.validate();
        } catch (IllegalArgumentException e) {
            throw new ValidationException("cliente", e.getMessage());
        }
    }
}
