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
// VARIANTE DAO
// ============================================
public class VarianteDAO {
    private static final Logger logger = LoggerFactory.getLogger(VarianteDAO.class);
    private final DatabaseManager dbManager;

    public VarianteDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Variante save(Variante variante) throws DataAccessException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar variante
            String sql = "INSERT INTO variantes (producto_id, sku, codigo_barras, precio_costo, " +
                        "precio_minorista, precio_mayorista, stock, stock_minimo, activo) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int varianteId;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, variante.getProductoId());
                ps.setString(2, variante.getSku());
                ps.setString(3, variante.getCodigoBarras());
                ps.setDouble(4, variante.getPrecioCosto());
                ps.setDouble(5, variante.getPrecioMinorista());
                ps.setDouble(6, variante.getPrecioMayorista());
                ps.setInt(7, variante.getStock());
                ps.setInt(8, variante.getStockMinimo());
                ps.setBoolean(9, variante.getActivo());

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        varianteId = rs.getInt(1);
                        variante.setId(varianteId);
                    } else {
                        throw new DataAccessException("No se pudo obtener el ID de la variante");
                    }
                }
            }

            // 2. Insertar atributos de la variante
            if (variante.getAtributos() != null && !variante.getAtributos().isEmpty()) {
                String sqlAttr = "INSERT INTO variante_atributos (variante_id, atributo_id, valor) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlAttr)) {
                    for (VarianteAtributo attr : variante.getAtributos()) {
                        ps.setInt(1, varianteId);
                        ps.setInt(2, attr.getAtributoId());
                        ps.setString(3, attr.getValor());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit();
            logger.info("Variante guardada: {}", variante.getSku());
            return variante;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error en rollback", ex);
                }
            }
            logger.error("Error guardando variante", e);
            throw new DataAccessException("Error guardando variante: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error cerrando conexión", e);
                }
            }
        }
    }

    public Variante update(Variante variante) throws DataAccessException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // 1. Actualizar variante
            String sql = "UPDATE variantes SET codigo_barras = ?, precio_costo = ?, " +
                    "precio_minorista = ?, precio_mayorista = ?, stock = ?, stock_minimo = ?, " +
                    "activo = ? WHERE id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, variante.getCodigoBarras());
                ps.setDouble(2, variante.getPrecioCosto());
                ps.setDouble(3, variante.getPrecioMinorista());
                ps.setDouble(4, variante.getPrecioMayorista());
                ps.setInt(5, variante.getStock());
                ps.setInt(6, variante.getStockMinimo());
                ps.setBoolean(7, variante.getActivo());
                ps.setInt(8, variante.getId());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new DataAccessException("Variante no encontrada con ID: " + variante.getId());
                }
            }

            // 2. Actualizar atributos: eliminar existentes y recrear
            String sqlDeleteAttr = "DELETE FROM variante_atributos WHERE variante_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteAttr)) {
                ps.setInt(1, variante.getId());
                ps.executeUpdate();
            }

            // 3. Insertar nuevos atributos
            if (variante.getAtributos() != null && !variante.getAtributos().isEmpty()) {
                String sqlAttr = "INSERT INTO variante_atributos (variante_id, atributo_id, valor) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlAttr)) {
                    for (VarianteAtributo attr : variante.getAtributos()) {
                        ps.setInt(1, variante.getId());
                        ps.setInt(2, attr.getAtributoId());
                        ps.setString(3, attr.getValor());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit();
            logger.info("Variante actualizada: {}", variante.getSku());
            return variante;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error en rollback", ex);
                }
            }
            logger.error("Error actualizando variante", e);
            throw new DataAccessException("Error actualizando variante: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error cerrando conexión", e);
                }
            }
        }
    }

    public void delete(Integer id) throws DataAccessException {
        String sql = "UPDATE variantes SET activo = 0 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Variante no encontrada con ID: " + id);
            }

            logger.info("Variante desactivada: {}", id);

        } catch (SQLException e) {
            logger.error("Error eliminando variante", e);
            throw new DataAccessException("Error eliminando variante: " + e.getMessage(), e);
        }
    }

    public Optional<Variante> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM variantes WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Variante variante = mapResultSetToVariante(rs);
                    // Cargar atributos
                    variante.setAtributos(findAtributosByVariante(id));
                    return Optional.of(variante);
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando variante", e);
            throw new DataAccessException("Error buscando variante: " + e.getMessage(), e);
        }
    }

    public Optional<Variante> findBySku(String sku) throws DataAccessException {
        String sql = "SELECT * FROM variantes WHERE sku = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Variante variante = mapResultSetToVariante(rs);
                    variante.setAtributos(findAtributosByVariante(variante.getId()));
                    return Optional.of(variante);
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando variante por SKU", e);
            throw new DataAccessException("Error buscando variante: " + e.getMessage(), e);
        }
    }

    public List<Variante> findByProducto(Integer productoId) throws DataAccessException {
        String sql = "SELECT * FROM variantes WHERE producto_id = ? AND activo = 1 ORDER BY sku";

        List<Variante> variantes = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Variante variante = mapResultSetToVariante(rs);
                    variante.setAtributos(findAtributosByVariante(variante.getId()));
                    variantes.add(variante);
                }
            }

            return variantes;

        } catch (SQLException e) {
            logger.error("Error buscando variantes por producto", e);
            throw new DataAccessException("Error buscando variantes: " + e.getMessage(), e);
        }
    }

    public List<Variante> findStockBajo() throws DataAccessException {
        String sql = "SELECT * FROM variantes WHERE stock<=stock_minimo";

        List<Variante> variantes = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Variante v = new Variante();
                v.setId(rs.getInt("id"));
                v.setSku(rs.getString("sku"));
                v.setStock(rs.getInt("stock"));
                v.setStockMinimo(rs.getInt("stock_minimo"));
                variantes.add(v);
            }

            return variantes;

        } catch (SQLException e) {
            logger.error("Error obteniendo stock bajo", e);
            throw new DataAccessException("Error obteniendo stock bajo: " + e.getMessage(), e);
        }
    }

    public void updateStock(Integer id, Integer nuevoStock) throws DataAccessException {
        String sql = "UPDATE variantes SET stock = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, nuevoStock);
            ps.setInt(2, id);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DataAccessException("Variante no encontrada con ID: " + id);
            }

            logger.info("Stock actualizado para variante {}: {}", id, nuevoStock);

        } catch (SQLException e) {
            logger.error("Error actualizando stock", e);
            throw new DataAccessException("Error actualizando stock: " + e.getMessage(), e);
        }
    }

    private List<VarianteAtributo> findAtributosByVariante(Integer varianteId) throws DataAccessException {
        String sql = "SELECT va.*, a.nombre as atributo_nombre FROM variante_atributos va " +
                    "INNER JOIN atributos a ON va.atributo_id = a.id WHERE va.variante_id = ?";

        List<VarianteAtributo> atributos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, varianteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VarianteAtributo va = new VarianteAtributo();
                    va.setId(rs.getInt("id"));
                    va.setVarianteId(rs.getInt("variante_id"));
                    va.setAtributoId(rs.getInt("atributo_id"));
                    va.setValor(rs.getString("valor"));
                    atributos.add(va);
                }
            }

            return atributos;

        } catch (SQLException e) {
            logger.error("Error obteniendo atributos de variante", e);
            throw new DataAccessException("Error obteniendo atributos: " + e.getMessage(), e);
        }
    }

    private Variante mapResultSetToVariante(ResultSet rs) throws SQLException {
        Variante v = new Variante();
        v.setId(rs.getInt("id"));
        v.setProductoId(rs.getInt("producto_id"));
        v.setSku(rs.getString("sku"));
        v.setCodigoBarras(rs.getString("codigo_barras"));
        v.setPrecioCosto(rs.getDouble("precio_costo"));
        v.setPrecioMinorista(rs.getDouble("precio_minorista"));
        v.setPrecioMayorista(rs.getDouble("precio_mayorista"));
        v.setStock(rs.getInt("stock"));
        v.setStockMinimo(rs.getInt("stock_minimo"));
        v.setActivo(rs.getBoolean("activo"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            v.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        return v;
    }
}
