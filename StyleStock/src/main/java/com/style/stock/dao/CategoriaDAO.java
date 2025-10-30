package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.Atributo;
import com.style.stock.model.Categoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones CRUD de Categorías
 */
public class CategoriaDAO {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaDAO.class);
    private final DatabaseManager dbManager;

    public CategoriaDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Categoria save(Categoria categoria) throws DataAccessException {
        String sql = "INSERT INTO categorias (nombre, descripcion, requiere_variantes, activo) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, categoria.getNombre());
            ps.setString(2, categoria.getDescripcion());
            ps.setBoolean(3, categoria.getRequiereVariantes());
            ps.setBoolean(4, categoria.getActivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    categoria.setId(rs.getInt(1));
                }
            }

            logger.info("Categoría guardada: {}", categoria.getNombre());
            return categoria;

        } catch (SQLException e) {
            logger.error("Error guardando categoría", e);
            throw new DataAccessException("Error guardando categoría: " + e.getMessage(), e);
        }
    }

    public Categoria update(Categoria categoria) throws DataAccessException {
        String sql = "UPDATE categorias SET nombre = ?, descripcion = ?, requiere_variantes = ?, activo = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, categoria.getNombre());
            ps.setString(2, categoria.getDescripcion());
            ps.setBoolean(3, categoria.getRequiereVariantes());
            ps.setBoolean(4, categoria.getActivo());
            ps.setInt(5, categoria.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Categoría no encontrada con ID: " + categoria.getId());
            }

            logger.info("Categoría actualizada: {}", categoria.getNombre());
            return categoria;

        } catch (SQLException e) {
            logger.error("Error actualizando categoría", e);
            throw new DataAccessException("Error actualizando categoría: " + e.getMessage(), e);
        }
    }

    public Optional<Categoria> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM categorias WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCategoria(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando categoría por ID", e);
            throw new DataAccessException("Error buscando categoría: " + e.getMessage(), e);
        }
    }

    public List<Categoria> findAll(boolean soloActivas) throws DataAccessException {
        String sql = soloActivas 
            ? "SELECT * FROM categorias WHERE activo = 1 ORDER BY nombre"
            : "SELECT * FROM categorias ORDER BY nombre";

        List<Categoria> categorias = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categorias.add(mapResultSetToCategoria(rs));
            }

            return categorias;

        } catch (SQLException e) {
            logger.error("Error obteniendo categorías", e);
            throw new DataAccessException("Error obteniendo categorías: " + e.getMessage(), e);
        }
    }

    public void delete(Integer id) throws DataAccessException {
        String sql = "UPDATE categorias SET activo = 0 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Categoría no encontrada con ID: " + id);
            }

            logger.info("Categoría desactivada: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando categoría", e);
            throw new DataAccessException("Error eliminando categoría: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene los atributos asociados a una categoría
     */
    public List<Atributo> findAtributosByCategoria(Integer categoriaId) throws DataAccessException {
        String sql = "SELECT a.* FROM atributos a " +
                    "INNER JOIN categoria_atributos ca ON a.id = ca.atributo_id " +
                    "WHERE ca.categoria_id = ? AND a.activo = 1 " +
                    "ORDER BY ca.orden, a.orden";

        List<Atributo> atributos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoriaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    atributos.add(mapResultSetToAtributo(rs));
                }
            }

            return atributos;

        } catch (SQLException e) {
            logger.error("Error obteniendo atributos de categoría", e);
            throw new DataAccessException("Error obteniendo atributos: " + e.getMessage(), e);
        }
    }

    /**
     * Asocia un atributo a una categoría
     */
    public void asociarAtributo(Integer categoriaId, Integer atributoId, boolean requerido, int orden) 
            throws DataAccessException {
        String sql = "INSERT OR REPLACE INTO categoria_atributos (categoria_id, atributo_id, requerido, orden) " +
                    "VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoriaId);
            ps.setInt(2, atributoId);
            ps.setBoolean(3, requerido);
            ps.setInt(4, orden);

            ps.executeUpdate();
            logger.debug("Atributo {} asociado a categoría {}", atributoId, categoriaId);

        } catch (SQLException e) {
            logger.error("Error asociando atributo a categoría", e);
            throw new DataAccessException("Error asociando atributo: " + e.getMessage(), e);
        }
    }

    /**
     * Desasocia un atributo de una categoría
     */
    public void desasociarAtributo(Integer categoriaId, Integer atributoId) throws DataAccessException {
        String sql = "DELETE FROM categoria_atributos WHERE categoria_id = ? AND atributo_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoriaId);
            ps.setInt(2, atributoId);

            ps.executeUpdate();
            logger.debug("Atributo {} desasociado de categoría {}", atributoId, categoriaId);

        } catch (SQLException e) {
            logger.error("Error desasociando atributo de categoría", e);
            throw new DataAccessException("Error desasociando atributo: " + e.getMessage(), e);
        }
    }

    private Categoria mapResultSetToCategoria(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setRequiereVariantes(rs.getBoolean("requiere_variantes"));
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
