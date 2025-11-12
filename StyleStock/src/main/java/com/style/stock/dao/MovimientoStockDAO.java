package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.MovimientoStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimientoStockDAO {
    private static final Logger logger = LoggerFactory.getLogger(MovimientoStockDAO.class);
    private final DatabaseManager dbManager;

    public MovimientoStockDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public List<MovimientoStock> findByFechas(LocalDate desde, LocalDate hasta, Integer varianteId, String tipo)
            throws DataAccessException {
        StringBuilder sql = new StringBuilder(
                "SELECT ms.*, v.sku, p.nombre as producto_nombre " +
                        "FROM movimientos_stock ms " +
                        "INNER JOIN variantes v ON ms.variante_id = v.id " +
                        "INNER JOIN productos p ON v.producto_id = p.id " +
                        "WHERE DATE(ms.created_at) BETWEEN ? AND ? "
        );

        if (varianteId != null) {
            sql.append("AND ms.variante_id = ? ");
        }
        if (tipo != null && !tipo.equals("TODOS")) {
            sql.append("AND ms.tipo = ? ");
        }

        sql.append("ORDER BY ms.created_at DESC LIMIT 500");

        List<MovimientoStock> movimientos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            int paramIndex = 3;
            if (varianteId != null) {
                ps.setInt(paramIndex++, varianteId);
            }
            if (tipo != null && !tipo.equals("TODOS")) {
                ps.setString(paramIndex, tipo);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimientoStock m = mapResultSetToMovimiento(rs);
                    movimientos.add(m);
                }
            }

            return movimientos;

        } catch (SQLException e) {
            logger.error("Error obteniendo movimientos", e);
            throw new DataAccessException("Error: " + e.getMessage(), e);
        }
    }

    public List<MovimientoStock> findByVariante(Integer varianteId, int limite) throws DataAccessException {
        String sql = "SELECT ms.*, v.sku, p.nombre as producto_nombre " +
                "FROM movimientos_stock ms " +
                "INNER JOIN variantes v ON ms.variante_id = v.id " +
                "INNER JOIN productos p ON v.producto_id = p.id " +
                "WHERE ms.variante_id = ? " +
                "ORDER BY ms.created_at DESC LIMIT ?";

        List<MovimientoStock> movimientos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, varianteId);
            ps.setInt(2, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(mapResultSetToMovimiento(rs));
                }
            }

            return movimientos;

        } catch (SQLException e) {
            logger.error("Error obteniendo movimientos por variante", e);
            throw new DataAccessException("Error: " + e.getMessage(), e);
        }
    }

    private MovimientoStock mapResultSetToMovimiento(ResultSet rs) throws SQLException {
        MovimientoStock m = new MovimientoStock();
        m.setId(rs.getInt("id"));
        m.setProductoId(rs.getInt("variante_id"));
        m.setTipo(MovimientoStock.TipoMovimiento.valueOf(rs.getString("tipo")));
        m.setCantidad(rs.getInt("cantidad"));
        m.setStockAnterior(rs.getInt("stock_anterior"));
        m.setStockNuevo(rs.getInt("stock_nuevo"));
        m.setReferencia(rs.getString("referencia"));
        m.setObservaciones(rs.getString("observaciones"));

        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            m.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }

        return m;
    }

    public MovimientoStock save(MovimientoStock movimiento) throws DataAccessException {
        String sql = "INSERT INTO movimientos_stock (variante_id, tipo, cantidad, stock_anterior, " +
                "stock_nuevo, referencia, observaciones, usuario) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, movimiento.getProductoId());
            ps.setString(2, movimiento.getTipo().getValor());
            ps.setInt(3, movimiento.getCantidad());
            ps.setInt(4, movimiento.getStockAnterior());
            ps.setInt(5, movimiento.getStockNuevo());
            ps.setString(6, movimiento.getReferencia());
            ps.setString(7, movimiento.getObservaciones());
            ps.setString(8, "Sistema");

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    movimiento.setId(rs.getInt(1));
                }
            }

            logger.info("Movimiento registrado: {} - Variante: {}",
                    movimiento.getTipo(), movimiento.getProductoId());
            return movimiento;

        } catch (SQLException e) {
            logger.error("Error guardando movimiento", e);
            throw new DataAccessException("Error: " + e.getMessage(), e);
        }
    }
}