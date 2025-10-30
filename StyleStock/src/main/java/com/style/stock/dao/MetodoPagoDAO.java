// ============================================
// MetodoPagoDAO.java
// ============================================
package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.MetodoPago;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MetodoPagoDAO {
    private static final Logger logger = LoggerFactory.getLogger(MetodoPagoDAO.class);
    private final DatabaseManager dbManager;

    public MetodoPagoDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public MetodoPago save(MetodoPago metodo) throws DataAccessException {
        String sql = "INSERT INTO metodos_pago (nombre, requiere_cuotas, comision_porcentaje, activo) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, metodo.getNombre());
            ps.setBoolean(2, metodo.getRequiereCuotas() != null && metodo.getRequiereCuotas());
            ps.setDouble(3, metodo.getComisionPorcentaje() != null ? metodo.getComisionPorcentaje() : 0.0);
            ps.setBoolean(4, metodo.getActivo() != null && metodo.getActivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    metodo.setId(rs.getInt(1));
                }
            }

            logger.info("Método de pago guardado: {}", metodo.getNombre());
            return metodo;

        } catch (SQLException e) {
            logger.error("Error guardando método de pago", e);
            throw new DataAccessException("Error guardando método de pago: " + e.getMessage(), e);
        }
    }

    public MetodoPago update(MetodoPago metodo) throws DataAccessException {
        String sql = "UPDATE metodos_pago SET nombre = ?, requiere_cuotas = ?, comision_porcentaje = ?, activo = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, metodo.getNombre());
            ps.setBoolean(2, metodo.getRequiereCuotas() != null && metodo.getRequiereCuotas());
            ps.setDouble(3, metodo.getComisionPorcentaje() != null ? metodo.getComisionPorcentaje() : 0.0);
            ps.setBoolean(4, metodo.getActivo() != null && metodo.getActivo());
            ps.setInt(5, metodo.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Método de pago no encontrado con ID: " + metodo.getId());
            }

            logger.info("Método de pago actualizado: {}", metodo.getNombre());
            return metodo;

        } catch (SQLException e) {
            logger.error("Error actualizando método de pago", e);
            throw new DataAccessException("Error actualizando método de pago: " + e.getMessage(), e);
        }
    }

    public Optional<MetodoPago> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM metodos_pago WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMetodo(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando método de pago", e);
            throw new DataAccessException("Error buscando método de pago: " + e.getMessage(), e);
        }
    }

    public List<MetodoPago> findAll(boolean soloActivos) throws DataAccessException {
        String sql = soloActivos 
            ? "SELECT * FROM metodos_pago WHERE activo = 1 ORDER BY nombre"
            : "SELECT * FROM metodos_pago ORDER BY nombre";

        List<MetodoPago> metodos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                metodos.add(mapResultSetToMetodo(rs));
            }

            return metodos;

        } catch (SQLException e) {
            logger.error("Error obteniendo métodos de pago", e);
            throw new DataAccessException("Error obteniendo métodos de pago: " + e.getMessage(), e);
        }
    }

    public void delete(Integer id) throws DataAccessException {
        String sql = "UPDATE metodos_pago SET activo = 0 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Método de pago no encontrado con ID: " + id);
            }

            logger.info("Método de pago desactivado: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando método de pago", e);
            throw new DataAccessException("Error eliminando método de pago: " + e.getMessage(), e);
        }
    }

    private MetodoPago mapResultSetToMetodo(ResultSet rs) throws SQLException {
        MetodoPago m = new MetodoPago();
        m.setId(rs.getInt("id"));
        m.setNombre(rs.getString("nombre"));
        m.setRequiereCuotas(rs.getBoolean("requiere_cuotas"));
        m.setComisionPorcentaje(rs.getDouble("comision_porcentaje"));
        m.setActivo(rs.getBoolean("activo"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            m.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        return m;
    }
}