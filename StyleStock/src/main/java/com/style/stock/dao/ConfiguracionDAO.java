// ============================================
// ConfiguracionDAO.java
// ============================================
package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.Configuracion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfiguracionDAO {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionDAO.class);
    private final DatabaseManager dbManager;

    public ConfiguracionDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Configuracion save(Configuracion config) throws DataAccessException {
        String sql = "INSERT OR REPLACE INTO configuracion (clave, valor, descripcion, tipo) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, config.getClave());
            ps.setString(2, config.getValor());
            ps.setString(3, config.getDescripcion());
            ps.setString(4, config.getTipo().getValor());

            ps.executeUpdate();
            logger.info("Configuración guardada: {}", config.getClave());
            return config;

        } catch (SQLException e) {
            logger.error("Error guardando configuración", e);
            throw new DataAccessException("Error guardando configuración: " + e.getMessage(), e);
        }
    }

    public Optional<Configuracion> findByClave(String clave) throws DataAccessException {
        String sql = "SELECT * FROM configuracion WHERE clave = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToConfiguracion(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando configuración", e);
            throw new DataAccessException("Error buscando configuración: " + e.getMessage(), e);
        }
    }

    public List<Configuracion> findAll() throws DataAccessException {
        String sql = "SELECT * FROM configuracion ORDER BY clave";
        List<Configuracion> configs = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                configs.add(mapResultSetToConfiguracion(rs));
            }

            return configs;

        } catch (SQLException e) {
            logger.error("Error obteniendo configuraciones", e);
            throw new DataAccessException("Error obteniendo configuraciones: " + e.getMessage(), e);
        }
    }

    public void delete(String clave) throws DataAccessException {
        String sql = "DELETE FROM configuracion WHERE clave = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clave);
            ps.executeUpdate();
            logger.info("Configuración eliminada: {}", clave);

        } catch (SQLException e) {
            logger.error("Error eliminando configuración", e);
            throw new DataAccessException("Error eliminando configuración: " + e.getMessage(), e);
        }
    }

    private Configuracion mapResultSetToConfiguracion(ResultSet rs) throws SQLException {
        Configuracion c = new Configuracion();
        c.setClave(rs.getString("clave"));
        c.setValor(rs.getString("valor"));
        c.setDescripcion(rs.getString("descripcion"));
        
        String tipoStr = rs.getString("tipo");
        if (tipoStr != null) {
            c.setTipo(Configuracion.TipoConfiguracion.valueOf(tipoStr));
        }
        
        String updatedAtStr = rs.getString("updated_at");
        if (updatedAtStr != null) {
            c.setUpdatedAt(LocalDateTime.parse(updatedAtStr.replace(" ", "T")));
        }
        
        return c;
    }
}
