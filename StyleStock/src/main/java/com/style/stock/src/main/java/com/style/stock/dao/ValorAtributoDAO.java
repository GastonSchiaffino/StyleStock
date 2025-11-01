// ============================================
// ValorAtributoDAO.java
// ============================================
package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.ValorAtributo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ValorAtributoDAO {
    private static final Logger logger = LoggerFactory.getLogger(ValorAtributoDAO.class);
    private final DatabaseManager dbManager;

    public ValorAtributoDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public ValorAtributo save(ValorAtributo valor) throws DataAccessException {
        String sql = "INSERT INTO valores_atributo (atributo_id, valor, codigo_hex, orden, activo) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, valor.getAtributoId());
            ps.setString(2, valor.getValor());
            ps.setString(3, valor.getCodigoHex());
            ps.setInt(4, valor.getOrden() != null ? valor.getOrden() : 0);
            ps.setBoolean(5, valor.getActivo() != null && valor.getActivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    valor.setId(rs.getInt(1));
                }
            }

            logger.info("Valor de atributo guardado: {}", valor.getValor());
            return valor;

        } catch (SQLException e) {
            logger.error("Error guardando valor de atributo", e);
            throw new DataAccessException("Error guardando valor: " + e.getMessage(), e);
        }
    }

    public Optional<ValorAtributo> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM valores_atributo WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToValor(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando valor de atributo", e);
            throw new DataAccessException("Error buscando valor: " + e.getMessage(), e);
        }
    }

    public List<ValorAtributo> findByAtributo(Integer atributoId) throws DataAccessException {
        String sql = "SELECT * FROM valores_atributo WHERE atributo_id = ? AND activo = 1 ORDER BY orden, valor";
        List<ValorAtributo> valores = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, atributoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    valores.add(mapResultSetToValor(rs));
                }
            }

            return valores;

        } catch (SQLException e) {
            logger.error("Error obteniendo valores de atributo", e);
            throw new DataAccessException("Error obteniendo valores: " + e.getMessage(), e);
        }
    }

    public List<ValorAtributo> findAll(Integer atributoId) throws DataAccessException {
        String sql = "SELECT * FROM valores_atributo WHERE atributo_id = ? ORDER BY orden, valor";
        List<ValorAtributo> valores = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, atributoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    valores.add(mapResultSetToValor(rs));
                }
            }

            return valores;

        } catch (SQLException e) {
            logger.error("Error obteniendo valores de atributo", e);
            throw new DataAccessException("Error obteniendo valores: " + e.getMessage(), e);
        }
    }

    public void delete(Integer id) throws DataAccessException {
        String sql = "UPDATE valores_atributo SET activo = 0 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Valor de atributo no encontrado con ID: " + id);
            }

            logger.info("Valor de atributo desactivado: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando valor de atributo", e);
            throw new DataAccessException("Error eliminando valor: " + e.getMessage(), e);
        }
    }

    private ValorAtributo mapResultSetToValor(ResultSet rs) throws SQLException {
        ValorAtributo v = new ValorAtributo();
        v.setId(rs.getInt("id"));
        v.setAtributoId(rs.getInt("atributo_id"));
        v.setValor(rs.getString("valor"));
        v.setCodigoHex(rs.getString("codigo_hex"));
        v.setOrden(rs.getInt("orden"));
        v.setActivo(rs.getBoolean("activo"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            v.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        return v;
    }
}