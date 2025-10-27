package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones CRUD de Productos
 */
public class ProductoDAO {
    private static final Logger logger = LoggerFactory.getLogger(ProductoDAO.class);
    private final DatabaseManager dbManager;

    public ProductoDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Guarda un nuevo producto
     */
    public Producto save(Producto producto) throws DataAccessException {
        String sql = "INSERT INTO productos (codigo, descripcion, precio, stock, stock_minimo, categoria, activo) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getDescripcion());
            ps.setDouble(3, producto.getPrecio());
            ps.setInt(4, producto.getStock());
            ps.setInt(5, producto.getStockMinimo());
            ps.setString(6, producto.getCategoria());
            ps.setBoolean(7, producto.getActivo());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("No se pudo guardar el producto");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    producto.setId(rs.getInt(1));
                }
            }

            logger.info("Producto guardado: {} - {}", producto.getCodigo(), producto.getDescripcion());
            return producto;

        } catch (SQLException e) {
            logger.error("Error guardando producto", e);
            throw new DataAccessException("Error guardando producto: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza un producto existente
     */
    public Producto update(Producto producto) throws DataAccessException {
        String sql = "UPDATE productos SET codigo = ?, descripcion = ?, precio = ?, stock = ?, " +
                    "stock_minimo = ?, categoria = ?, activo = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getDescripcion());
            ps.setDouble(3, producto.getPrecio());
            ps.setInt(4, producto.getStock());
            ps.setInt(5, producto.getStockMinimo());
            ps.setString(6, producto.getCategoria());
            ps.setBoolean(7, producto.getActivo());
            ps.setInt(8, producto.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Producto no encontrado con ID: " + producto.getId());
            }

            logger.info("Producto actualizado: {} - {}", producto.getCodigo(), producto.getDescripcion());
            return producto;

        } catch (SQLException e) {
            logger.error("Error actualizando producto", e);
            throw new DataAccessException("Error actualizando producto: " + e.getMessage(), e);
        }
    }

    /**
     * Busca un producto por ID
     */
    public Optional<Producto> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM productos WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProducto(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando producto por ID", e);
            throw new DataAccessException("Error buscando producto: " + e.getMessage(), e);
        }
    }

    /**
     * Busca un producto por código
     */
    public Optional<Producto> findByCodigo(String codigo) throws DataAccessException {
        String sql = "SELECT * FROM productos WHERE codigo = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProducto(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando producto por código", e);
            throw new DataAccessException("Error buscando producto: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene todos los productos activos
     */
    public List<Producto> findAll() throws DataAccessException {
        return findAll(true);
    }

    /**
     * Obtiene todos los productos (activos o todos)
     */
    public List<Producto> findAll(boolean soloActivos) throws DataAccessException {
        String sql = soloActivos 
            ? "SELECT * FROM productos WHERE activo = 1 ORDER BY descripcion"
            : "SELECT * FROM productos ORDER BY descripcion";

        List<Producto> productos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                productos.add(mapResultSetToProducto(rs));
            }

            logger.debug("Productos encontrados: {}", productos.size());
            return productos;

        } catch (SQLException e) {
            logger.error("Error obteniendo productos", e);
            throw new DataAccessException("Error obteniendo productos: " + e.getMessage(), e);
        }
    }

    /**
     * Busca productos por descripción (LIKE)
     */
    public List<Producto> findByDescripcion(String descripcion) throws DataAccessException {
        String sql = "SELECT * FROM productos WHERE descripcion LIKE ? AND activo = 1 ORDER BY descripcion";
        List<Producto> productos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + descripcion + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapResultSetToProducto(rs));
                }
            }

            return productos;

        } catch (SQLException e) {
            logger.error("Error buscando productos por descripción", e);
            throw new DataAccessException("Error buscando productos: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene productos con stock bajo
     */
    public List<Producto> findStockBajo() throws DataAccessException {
        String sql = "SELECT * FROM v_productos_stock_bajo ORDER BY faltante DESC";
        List<Producto> productos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                productos.add(mapResultSetToProducto(rs));
            }

            logger.debug("Productos con stock bajo: {}", productos.size());
            return productos;

        } catch (SQLException e) {
            logger.error("Error obteniendo productos con stock bajo", e);
            throw new DataAccessException("Error obteniendo productos: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un producto (soft delete)
     */
    public void delete(Integer id) throws DataAccessException {
        String sql = "UPDATE productos SET activo = 0 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Producto no encontrado con ID: " + id);
            }

            logger.info("Producto desactivado: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando producto", e);
            throw new DataAccessException("Error eliminando producto: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina permanentemente un producto
     */
    public void deletePermantente(Integer id) throws DataAccessException {
        String sql = "DELETE FROM productos WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Producto no encontrado con ID: " + id);
            }

            logger.warn("Producto eliminado permanentemente: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando producto permanentemente", e);
            throw new DataAccessException("Error eliminando producto: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza el stock de un producto
     */
    public void updateStock(Integer id, Integer nuevoStock) throws DataAccessException {
        String sql = "UPDATE productos SET stock = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, nuevoStock);
            ps.setInt(2, id);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Producto no encontrado con ID: " + id);
            }

            logger.info("Stock actualizado para producto {}: {}", id, nuevoStock);

        } catch (SQLException e) {
            logger.error("Error actualizando stock", e);
            throw new DataAccessException("Error actualizando stock: " + e.getMessage(), e);
        }
    }

    /**
     * Cuenta total de productos
     */
    public long count() throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM productos WHERE activo = 1";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Error contando productos", e);
            throw new DataAccessException("Error contando productos: " + e.getMessage(), e);
        }
    }

    /**
     * Mapea ResultSet a objeto Producto
     */
    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecio(rs.getDouble("precio"));
        p.setStock(rs.getInt("stock"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setCategoria(rs.getString("categoria"));
        p.setActivo(rs.getBoolean("activo"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            p.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        String updatedAtStr = rs.getString("updated_at");
        if (updatedAtStr != null) {
            p.setUpdatedAt(LocalDateTime.parse(updatedAtStr.replace(" ", "T")));
        }
        
        return p;
    }
}