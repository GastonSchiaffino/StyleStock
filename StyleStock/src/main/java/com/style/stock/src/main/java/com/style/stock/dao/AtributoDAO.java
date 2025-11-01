// ============================================
// AtributoDAO.java
// ============================================
package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.Atributo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AtributoDAO {
    private static final Logger logger = LoggerFactory.getLogger(AtributoDAO.class);
    private final DatabaseManager dbManager;

    public AtributoDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Atributo save(Atributo atributo) throws DataAccessException {
        String sql = "INSERT INTO atributos (nombre, tipo, descripcion, orden, activo) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, atributo.getNombre());
            ps.setString(2, atributo.getTipo().getValor());
            ps.setString(3, atributo.getDescripcion());
            ps.setInt(4, atributo.getOrden() != null ? atributo.getOrden() : 0);
            ps.setBoolean(5, atributo.getActivo() != null && atributo.getActivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    atributo.setId(rs.getInt(1));
                }
            }

            logger.info("Atributo guardado: {}", atributo.getNombre());
            return atributo;

        } catch (SQLException e) {
            logger.error("Error guardando atributo", e);
            throw new DataAccessException("Error guardando atributo: " + e.getMessage(), e);
        }
    }

    public Atributo update(Atributo atributo) throws DataAccessException {
        String sql = "UPDATE atributos SET nombre = ?, tipo = ?, descripcion = ?, orden = ?, activo = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, atributo.getNombre());
            ps.setString(2, atributo.getTipo().getValor());
            ps.setString(3, atributo.getDescripcion());
            ps.setInt(4, atributo.getOrden() != null ? atributo.getOrden() : 0);
            ps.setBoolean(5, atributo.getActivo() != null && atributo.getActivo());
            ps.setInt(6, atributo.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Atributo no encontrado con ID: " + atributo.getId());
            }

            logger.info("Atributo actualizado: {}", atributo.getNombre());
            return atributo;

        } catch (SQLException e) {
            logger.error("Error actualizando atributo", e);
            throw new DataAccessException("Error actualizando atributo: " + e.getMessage(), e);
        }
    }

    public Optional<Atributo> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM atributos WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAtributo(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando atributo por ID", e);
            throw new DataAccessException("Error buscando atributo: " + e.getMessage(), e);
        }
    }

    public List<Atributo> findAll(boolean soloActivos) throws DataAccessException {
        String sql = soloActivos 
            ? "SELECT * FROM atributos WHERE activo = 1 ORDER BY orden, nombre"
            : "SELECT * FROM atributos ORDER BY orden, nombre";

        List<Atributo> atributos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                atributos.add(mapResultSetToAtributo(rs));
            }

            return atributos;

        } catch (SQLException e) {
            logger.error("Error obteniendo atributos", e);
            throw new DataAccessException("Error obteniendo atributos: " + e.getMessage(), e);
        }
    }

    public void delete(Integer id) throws DataAccessException {
        String sql = "UPDATE atributos SET activo = 0 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Atributo no encontrado con ID: " + id);
            }

            logger.info("Atributo desactivado: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando atributo", e);
            throw new DataAccessException("Error eliminando atributo: " + e.getMessage(), e);
        }
    }

    private Atributo mapResultSetToAtributo(ResultSet rs) throws SQLException {
        Atributo a = new Atributo();
        a.setId(rs.getInt("id"));
        a.setNombre(rs.getString("nombre"));
        a.setTipo(Atributo.TipoAtributo.valueOf(rs.getString("tipo")));
        a.setDescripcion(rs.getString("descripcion"));
        a.setOrden(rs.getInt("orden"));
        a.setActivo(rs.getBoolean("activo"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            a.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        return a;
    }
}
