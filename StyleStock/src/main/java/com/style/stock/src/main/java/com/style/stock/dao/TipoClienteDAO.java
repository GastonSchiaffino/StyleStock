// ============================================
// TipoClienteDAO.java
// ============================================
package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.TipoCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TipoClienteDAO {
    private static final Logger logger = LoggerFactory.getLogger(TipoClienteDAO.class);
    private final DatabaseManager dbManager;

    public TipoClienteDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public TipoCliente save(TipoCliente tipo) throws DataAccessException {
        String sql = "INSERT INTO tipos_cliente (nombre, usa_precio_mayorista, descripcion, activo) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, tipo.getNombre());
            ps.setBoolean(2, tipo.getUsaPrecioMayorista() != null && tipo.getUsaPrecioMayorista());
            ps.setString(3, tipo.getDescripcion());
            ps.setBoolean(4, tipo.getActivo() != null && tipo.getActivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    tipo.setId(rs.getInt(1));
                }
            }

            logger.info("Tipo de cliente guardado: {}", tipo.getNombre());
            return tipo;

        } catch (SQLException e) {
            logger.error("Error guardando tipo de cliente", e);
            throw new DataAccessException("Error guardando tipo de cliente: " + e.getMessage(), e);
        }
    }

    public TipoCliente update(TipoCliente tipo) throws DataAccessException {
        String sql = "UPDATE tipos_cliente SET nombre = ?, usa_precio_mayorista = ?, descripcion = ?, activo = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tipo.getNombre());
            ps.setBoolean(2, tipo.getUsaPrecioMayorista() != null && tipo.getUsaPrecioMayorista());
            ps.setString(3, tipo.getDescripcion());
            ps.setBoolean(4, tipo.getActivo() != null && tipo.getActivo());
            ps.setInt(5, tipo.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Tipo de cliente no encontrado con ID: " + tipo.getId());
            }

            logger.info("Tipo de cliente actualizado: {}", tipo.getNombre());
            return tipo;

        } catch (SQLException e) {
            logger.error("Error actualizando tipo de cliente", e);
            throw new DataAccessException("Error actualizando tipo de cliente: " + e.getMessage(), e);
        }
    }

    public Optional<TipoCliente> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM tipos_cliente WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTipo(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando tipo de cliente", e);
            throw new DataAccessException("Error buscando tipo de cliente: " + e.getMessage(), e);
        }
    }

    public List<TipoCliente> findAll(boolean soloActivos) throws DataAccessException {
        String sql = soloActivos 
            ? "SELECT * FROM tipos_cliente WHERE activo = 1 ORDER BY nombre"
            : "SELECT * FROM tipos_cliente ORDER BY nombre";

        List<TipoCliente> tipos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tipos.add(mapResultSetToTipo(rs));
            }

            return tipos;

        } catch (SQLException e) {
            logger.error("Error obteniendo tipos de cliente", e);
            throw new DataAccessException("Error obteniendo tipos de cliente: " + e.getMessage(), e);
        }
    }

    public void delete(Integer id) throws DataAccessException {
        String sql = "UPDATE tipos_cliente SET activo = 0 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Tipo de cliente no encontrado con ID: " + id);
            }

            logger.info("Tipo de cliente desactivado: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando tipo de cliente", e);
            throw new DataAccessException("Error eliminando tipo de cliente: " + e.getMessage(), e);
        }
    }

    private TipoCliente mapResultSetToTipo(ResultSet rs) throws SQLException {
        TipoCliente t = new TipoCliente();
        t.setId(rs.getInt("id"));
        t.setNombre(rs.getString("nombre"));
        t.setUsaPrecioMayorista(rs.getBoolean("usa_precio_mayorista"));
        t.setDescripcion(rs.getString("descripcion"));
        t.setActivo(rs.getBoolean("activo"));
        return t;
    }
}
