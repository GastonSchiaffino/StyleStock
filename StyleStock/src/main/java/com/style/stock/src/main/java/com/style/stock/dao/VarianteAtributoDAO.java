// ============================================
// VarianteAtributoDAO.java
// ============================================
package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.VarianteAtributo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VarianteAtributoDAO {
    private static final Logger logger = LoggerFactory.getLogger(VarianteAtributoDAO.class);
    private final DatabaseManager dbManager;

    public VarianteAtributoDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public VarianteAtributo save(VarianteAtributo varAttr) throws DataAccessException {
        String sql = "INSERT INTO variante_atributos (variante_id, atributo_id, valor) VALUES (?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, varAttr.getVarianteId());
            ps.setInt(2, varAttr.getAtributoId());
            ps.setString(3, varAttr.getValor());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    varAttr.setId(rs.getInt(1));
                }
            }

            logger.debug("Atributo de variante guardado");
            return varAttr;

        } catch (SQLException e) {
            logger.error("Error guardando atributo de variante", e);
            throw new DataAccessException("Error guardando atributo de variante: " + e.getMessage(), e);
        }
    }

    public List<VarianteAtributo> findByVariante(Integer varianteId) throws DataAccessException {
        String sql = "SELECT va.*, a.nombre as atributo_nombre FROM variante_atributos va " +
                    "INNER JOIN atributos a ON va.atributo_id = a.id WHERE va.variante_id = ?";

        List<VarianteAtributo> atributos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, varianteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    atributos.add(mapResultSetToVarianteAtributo(rs));
                }
            }

            return atributos;

        } catch (SQLException e) {
            logger.error("Error obteniendo atributos de variante", e);
            throw new DataAccessException("Error obteniendo atributos: " + e.getMessage(), e);
        }
    }

    public void deleteByVariante(Integer varianteId) throws DataAccessException {
        String sql = "DELETE FROM variante_atributos WHERE variante_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, varianteId);
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.error("Error eliminando atributos de variante", e);
            throw new DataAccessException("Error eliminando atributos: " + e.getMessage(), e);
        }
    }

    private VarianteAtributo mapResultSetToVarianteAtributo(ResultSet rs) throws SQLException {
        VarianteAtributo va = new VarianteAtributo();
        va.setId(rs.getInt("id"));
        va.setVarianteId(rs.getInt("variante_id"));
        va.setAtributoId(rs.getInt("atributo_id"));
        va.setValor(rs.getString("valor"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            va.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        return va;
    }
}