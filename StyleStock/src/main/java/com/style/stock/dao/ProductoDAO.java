package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ============================================
// PRODUCTO DAO
// ============================================
public class ProductoDAO {
    private static final Logger logger = LoggerFactory.getLogger(ProductoDAO.class);
    private final DatabaseManager dbManager;

    public ProductoDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Producto save(Producto producto) throws DataAccessException {
        String sql = "INSERT INTO productos (codigo, nombre, descripcion, categoria_id, marca, " +
                    "precio_costo, precio_minorista, precio_mayorista, imagen_url, activo) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getDescripcion());
            ps.setInt(4, producto.getCategoriaId());
            ps.setString(5, producto.getMarca());
            ps.setDouble(6, producto.getPrecioCosto());
            ps.setDouble(7, producto.getPrecioMinorista());
            ps.setDouble(8, producto.getPrecioMayorista());
            ps.setString(9, producto.getImagenUrl());
            ps.setBoolean(10, producto.getActivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    producto.setId(rs.getInt(1));
                }
            }

            logger.info("Producto guardado: {} - {}", producto.getCodigo(), producto.getNombre());
            return producto;

        } catch (SQLException e) {
            logger.error("Error guardando producto", e);
            throw new DataAccessException("Error guardando producto: " + e.getMessage(), e);
        }
    }

    public Producto update(Producto producto) throws DataAccessException {
        String sql = "UPDATE productos SET codigo = ?, nombre = ?, descripcion = ?, categoria_id = ?, " +
                    "marca = ?, precio_costo = ?, precio_minorista = ?, precio_mayorista = ?, " +
                    "imagen_url = ?, activo = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getNombre());
            ps.setString(3, producto.getDescripcion());
            ps.setInt(4, producto.getCategoriaId());
            ps.setString(5, producto.getMarca());
            ps.setDouble(6, producto.getPrecioCosto());
            ps.setDouble(7, producto.getPrecioMinorista());
            ps.setDouble(8, producto.getPrecioMayorista());
            ps.setString(9, producto.getImagenUrl());
            ps.setBoolean(10, producto.getActivo());
            ps.setInt(11, producto.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Producto no encontrado con ID: " + producto.getId());
            }

            logger.info("Producto actualizado: {}", producto.getNombre());
            return producto;

        } catch (SQLException e) {
            logger.error("Error actualizando producto", e);
            throw new DataAccessException("Error actualizando producto: " + e.getMessage(), e);
        }
    }

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

    public List<Producto> findAll(boolean soloActivos) throws DataAccessException {
        String sql = soloActivos 
            ? "SELECT * FROM productos WHERE activo = 1 ORDER BY nombre"
            : "SELECT * FROM productos ORDER BY nombre";

        List<Producto> productos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                productos.add(mapResultSetToProducto(rs));
            }

            return productos;

        } catch (SQLException e) {
            logger.error("Error obteniendo productos", e);
            throw new DataAccessException("Error obteniendo productos: " + e.getMessage(), e);
        }
    }

    public List<Producto> findByCategoria(Integer categoriaId) throws DataAccessException {
        String sql = "SELECT * FROM productos WHERE categoria_id = ? AND activo = 1 ORDER BY nombre";

        List<Producto> productos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoriaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapResultSetToProducto(rs));
                }
            }

            return productos;

        } catch (SQLException e) {
            logger.error("Error buscando productos por categoría", e);
            throw new DataAccessException("Error buscando productos: " + e.getMessage(), e);
        }
    }

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

    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        p.setMarca(rs.getString("marca"));
        p.setPrecioCosto(rs.getDouble("precio_costo"));
        p.setPrecioMinorista(rs.getDouble("precio_minorista"));
        p.setPrecioMayorista(rs.getDouble("precio_mayorista"));
        p.setImagenUrl(rs.getString("imagen_url"));
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
