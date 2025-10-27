package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.Cliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones CRUD de Clientes
 */
public class ClienteDAO {
    private static final Logger logger = LoggerFactory.getLogger(ClienteDAO.class);
    private final DatabaseManager dbManager;

    public ClienteDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Cliente save(Cliente cliente) throws DataAccessException {
        String sql = "INSERT INTO clientes (nombre, direccion, telefono, cuit, email, activo) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDireccion());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getCuit());
            ps.setString(5, cliente.getEmail());
            ps.setBoolean(6, cliente.getActivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    cliente.setId(rs.getInt(1));
                }
            }

            logger.info("Cliente guardado: {}", cliente.getNombre());
            return cliente;

        } catch (SQLException e) {
            logger.error("Error guardando cliente", e);
            throw new DataAccessException("Error guardando cliente: " + e.getMessage(), e);
        }
    }

    public Cliente update(Cliente cliente) throws DataAccessException {
        String sql = "UPDATE clientes SET nombre = ?, direccion = ?, telefono = ?, " +
                    "cuit = ?, email = ?, activo = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDireccion());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getCuit());
            ps.setString(5, cliente.getEmail());
            ps.setBoolean(6, cliente.getActivo());
            ps.setInt(7, cliente.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Cliente no encontrado con ID: " + cliente.getId());
            }

            logger.info("Cliente actualizado: {}", cliente.getNombre());
            return cliente;

        } catch (SQLException e) {
            logger.error("Error actualizando cliente", e);
            throw new DataAccessException("Error actualizando cliente: " + e.getMessage(), e);
        }
    }

    public Optional<Cliente> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM clientes WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCliente(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando cliente por ID", e);
            throw new DataAccessException("Error buscando cliente: " + e.getMessage(), e);
        }
    }

    public List<Cliente> findAll(boolean soloActivos) throws DataAccessException {
        String sql = soloActivos 
            ? "SELECT * FROM clientes WHERE activo = 1 ORDER BY nombre"
            : "SELECT * FROM clientes ORDER BY nombre";

        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(mapResultSetToCliente(rs));
            }

            return clientes;

        } catch (SQLException e) {
            logger.error("Error obteniendo clientes", e);
            throw new DataAccessException("Error obteniendo clientes: " + e.getMessage(), e);
        }
    }

    public List<Cliente> findByNombre(String nombre) throws DataAccessException {
        String sql = "SELECT * FROM clientes WHERE nombre LIKE ? AND activo = 1 ORDER BY nombre";
        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapResultSetToCliente(rs));
                }
            }

            return clientes;

        } catch (SQLException e) {
            logger.error("Error buscando clientes por nombre", e);
            throw new DataAccessException("Error buscando clientes: " + e.getMessage(), e);
        }
    }

    public void delete(Integer id) throws DataAccessException {
        String sql = "UPDATE clientes SET activo = 0 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Cliente no encontrado con ID: " + id);
            }

            logger.info("Cliente desactivado: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando cliente", e);
            throw new DataAccessException("Error eliminando cliente: " + e.getMessage(), e);
        }
    }

    private Cliente mapResultSetToCliente(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setDireccion(rs.getString("direccion"));
        c.setTelefono(rs.getString("telefono"));
        c.setCuit(rs.getString("cuit"));
        c.setEmail(rs.getString("email"));
        c.setActivo(rs.getBoolean("activo"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            c.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        String updatedAtStr = rs.getString("updated_at");
        if (updatedAtStr != null) {
            c.setUpdatedAt(LocalDateTime.parse(updatedAtStr.replace(" ", "T")));
        }
        
        return c;
    }
}

