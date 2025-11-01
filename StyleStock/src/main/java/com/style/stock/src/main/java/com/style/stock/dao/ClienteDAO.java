package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ============================================
// CLIENTE DAO
// ============================================
public class ClienteDAO {
    private static final Logger logger = LoggerFactory.getLogger(ClienteDAO.class);
    private final DatabaseManager dbManager;

    public ClienteDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Cliente save(Cliente cliente) throws DataAccessException {
        String sql = "INSERT INTO clientes (nombre, apellido, direccion, telefono, email, cuit, " +
                    "tipo_cliente_id, notas, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getApellido());
            ps.setString(3, cliente.getDireccion());
            ps.setString(4, cliente.getTelefono());
            ps.setString(5, cliente.getEmail());
            ps.setString(6, cliente.getCuit());
            ps.setInt(7, cliente.getTipoClienteId());
            ps.setString(8, cliente.getNotas());
            ps.setBoolean(9, cliente.getActivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    cliente.setId(rs.getInt(1));
                }
            }

            logger.info("Cliente guardado: {}", cliente.getNombreCompleto());
            return cliente;

        } catch (SQLException e) {
            logger.error("Error guardando cliente", e);
            throw new DataAccessException("Error guardando cliente: " + e.getMessage(), e);
        }
    }

    public Cliente update(Cliente cliente) throws DataAccessException {
        String sql = "UPDATE clientes SET nombre = ?, apellido = ?, direccion = ?, telefono = ?, " +
                    "email = ?, cuit = ?, tipo_cliente_id = ?, notas = ?, activo = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getApellido());
            ps.setString(3, cliente.getDireccion());
            ps.setString(4, cliente.getTelefono());
            ps.setString(5, cliente.getEmail());
            ps.setString(6, cliente.getCuit());
            ps.setInt(7, cliente.getTipoClienteId());
            ps.setString(8, cliente.getNotas());
            ps.setBoolean(9, cliente.getActivo());
            ps.setInt(10, cliente.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Cliente no encontrado con ID: " + cliente.getId());
            }

            logger.info("Cliente actualizado: {}", cliente.getNombreCompleto());
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
            logger.error("Error buscando cliente", e);
            throw new DataAccessException("Error buscando cliente: " + e.getMessage(), e);
        }
    }

    public List<Cliente> findAll(boolean soloActivos) throws DataAccessException {
        String sql = soloActivos 
            ? "SELECT * FROM clientes WHERE activo = 1 ORDER BY nombre, apellido"
            : "SELECT * FROM clientes ORDER BY nombre, apellido";

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

    public List<Cliente> buscarPorNombre(String nombre) throws DataAccessException {
        String sql = "SELECT * FROM clientes WHERE (nombre LIKE ? OR apellido LIKE ?) AND activo = 1 " +
                    "ORDER BY nombre, apellido";
        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String patron = "%" + nombre + "%";
            ps.setString(1, patron);
            ps.setString(2, patron);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapResultSetToCliente(rs));
                }
            }

            return clientes;

        } catch (SQLException e) {
            logger.error("Error buscando clientes", e);
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
        c.setApellido(rs.getString("apellido"));
        c.setDireccion(rs.getString("direccion"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setCuit(rs.getString("cuit"));
        c.setTipoClienteId(rs.getInt("tipo_cliente_id"));
        c.setNotas(rs.getString("notas"));
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
