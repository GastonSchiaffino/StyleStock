package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ============================================
// VENTA DAO
// ============================================
public class VentaDAO {
    private static final Logger logger = LoggerFactory.getLogger(VentaDAO.class);
    private final DatabaseManager dbManager;
    private final VarianteDAO varianteDAO;

    public VentaDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.varianteDAO = new VarianteDAO();
    }

    /**
     * Guarda una venta completa con sus detalles y pagos (transaccional)
     */
    public Venta save(Venta venta) throws DataAccessException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar venta
            String sqlVenta = "INSERT INTO ventas (cliente_id, tipo_cliente_id, fecha, hora, subtotal, " +
                             "descuento, total, tipo_comprobante, tipo_venta, estado, notas, vendedor) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            int ventaId;
            try (PreparedStatement ps = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, venta.getClienteId());
                ps.setInt(2, venta.getTipoClienteId());
                ps.setString(3, venta.getFecha().toString());
                ps.setString(4, venta.getHora().toString());
                ps.setDouble(5, venta.getSubtotal());
                ps.setDouble(6, venta.getDescuento() != null ? venta.getDescuento() : 0.0);
                ps.setDouble(7, venta.getTotal());
                ps.setString(8, venta.getTipoComprobante().getValor());
                ps.setString(9, venta.getTipoVenta().getValor());
                ps.setString(10, venta.getEstado().getValor());
                ps.setString(11, venta.getNotas());
                ps.setString(12, venta.getVendedor());

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        ventaId = rs.getInt(1);
                        System.out.println("Se crea id de venta: " + ventaId);
                        venta.setId(ventaId);
                    } else {
                        throw new DataAccessException("No se pudo obtener el ID de la venta");
                    }
                }
            }

            // 2. Insertar detalles (el trigger actualiza stock automáticamente)
            String sqlDetalle = "INSERT INTO detalle_venta (venta_id, variante_id, cantidad, " +
                               "precio_unitario, precio_tipo, descuento, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                for (DetalleVenta detalle : venta.getDetalles()) {
                    ps.setInt(1, ventaId);
                    ps.setInt(2, detalle.getVarianteId());
                    ps.setInt(3, detalle.getCantidad());
                    ps.setDouble(4, detalle.getPrecioUnitario());
                    ps.setString(5, detalle.getPrecioTipo());
                    ps.setDouble(6, detalle.getDescuento() != null ? detalle.getDescuento() : 0.0);
                    ps.setDouble(7, detalle.getSubtotal());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 3. Insertar pagos
            if (venta.getPagos() != null && !venta.getPagos().isEmpty()) {
                String sqlPago = "INSERT INTO pagos_venta (venta_id, metodo_pago_id, monto, cuotas, comision, observaciones) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(sqlPago)) {
                    for (PagoVenta pago : venta.getPagos()) {
                        ps.setInt(1, ventaId);
                        ps.setInt(2, pago.getMetodoPagoId());
                        ps.setDouble(3, pago.getMonto());
                        ps.setInt(4, pago.getCuotas());
                        ps.setDouble(5, pago.getComision() != null ? pago.getComision() : 0.0);
                        ps.setString(6, pago.getObservaciones());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit();
            logger.info("Venta guardada: {} - Total: ${}", venta.getNumeroComprobante(), venta.getTotal());
            return venta;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Transacción revertida");
                } catch (SQLException ex) {
                    logger.error("Error al revertir transacción", ex);
                }
            }
            logger.error("Error guardando venta", e);
            throw new DataAccessException("Error guardando venta: " + e.getMessage(), e);
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

    public Optional<Venta> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM ventas WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venta venta = mapResultSetToVenta(rs);
                    venta.setDetalles(findDetallesByVenta(id));
                    venta.setPagos(findPagosByVenta(id));
                    return Optional.of(venta);
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando venta", e);
            throw new DataAccessException("Error buscando venta: " + e.getMessage(), e);
        }
    }

    public List<Venta> findAll(int limit) throws DataAccessException {
        String sql = "SELECT * FROM ventas ORDER BY fecha DESC, hora DESC LIMIT ?";
        List<Venta> ventas = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ventas.add(mapResultSetToVenta(rs));
                }
            }

            return ventas;

        } catch (SQLException e) {
            logger.error("Error obteniendo ventas", e);
            throw new DataAccessException("Error obteniendo ventas: " + e.getMessage(), e);
        }
    }

    public List<Venta> findByFecha(LocalDate fecha) throws DataAccessException {
        String sql = "SELECT * FROM ventas WHERE fecha = ? ORDER BY hora DESC";
        List<Venta> ventas = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fecha.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ventas.add(mapResultSetToVenta(rs));
                }
            }

            return ventas;

        } catch (SQLException e) {
            logger.error("Error buscando ventas por fecha", e);
            throw new DataAccessException("Error buscando ventas: " + e.getMessage(), e);
        }
    }

    public void anularVenta(Integer id) throws DataAccessException {
        String sql = "UPDATE ventas SET estado = 'ANULADA' WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affected = ps.executeUpdate();

            if (affected == 0) {
                throw new DataAccessException("Venta no encontrada con ID: " + id);
            }

            logger.info("Venta anulada: {}", id);

        } catch (SQLException e) {
            logger.error("Error anulando venta", e);
            throw new DataAccessException("Error anulando venta: " + e.getMessage(), e);
        }
    }

    private List<DetalleVenta> findDetallesByVenta(Integer ventaId) throws DataAccessException {
        String sql = "SELECT * FROM detalle_venta WHERE venta_id = ?";
        List<DetalleVenta> detalles = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detalles.add(mapResultSetToDetalle(rs));
                }
            }

            return detalles;

        } catch (SQLException e) {
            logger.error("Error obteniendo detalles de venta", e);
            throw new DataAccessException("Error obteniendo detalles: " + e.getMessage(), e);
        }
    }

    private List<PagoVenta> findPagosByVenta(Integer ventaId) throws DataAccessException {
        String sql = "SELECT * FROM pagos_venta WHERE venta_id = ?";
        List<PagoVenta> pagos = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(mapResultSetToPago(rs));
                }
            }

            return pagos;

        } catch (SQLException e) {
            logger.error("Error obteniendo pagos de venta", e);
            throw new DataAccessException("Error obteniendo pagos: " + e.getMessage(), e);
        }
    }

    private Venta mapResultSetToVenta(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getInt("id"));
        v.setNumeroComprobante(rs.getString("numero_comprobante"));
        v.setClienteId(rs.getInt("cliente_id"));
        v.setTipoClienteId(rs.getInt("tipo_cliente_id"));
        v.setFecha(LocalDate.parse(rs.getString("fecha")));
        v.setHora(LocalTime.parse(rs.getString("hora")));
        v.setSubtotal(rs.getDouble("subtotal"));
        v.setDescuento(rs.getDouble("descuento"));
        v.setTotal(rs.getDouble("total"));
        v.setTipoComprobante(Venta.TipoComprobante.valueOf(rs.getString("tipo_comprobante")));
        v.setTipoVenta(Venta.TipoVenta.valueOf(rs.getString("tipo_venta")));
        v.setEstado(Venta.EstadoVenta.valueOf(rs.getString("estado")));
        v.setNotas(rs.getString("notas"));
        v.setVendedor(rs.getString("vendedor"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            v.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        return v;
    }

    private DetalleVenta mapResultSetToDetalle(ResultSet rs) throws SQLException, DataAccessException {
        DetalleVenta d = new DetalleVenta();
        d.setId(rs.getInt("id"));
        d.setVentaId(rs.getInt("venta_id"));
        d.setVarianteId(rs.getInt("variante_id"));
        d.setCantidad(rs.getInt("cantidad"));
        d.setPrecioUnitario(rs.getDouble("precio_unitario"));
        d.setPrecioTipo(rs.getString("precio_tipo"));
        d.setDescuento(rs.getDouble("descuento"));
        d.setSubtotal(rs.getDouble("subtotal"));
        
        // Cargar variante
        varianteDAO.findById(d.getVarianteId()).ifPresent(d::setVariante);
        
        return d;
    }

    private PagoVenta mapResultSetToPago(ResultSet rs) throws SQLException {
        PagoVenta p = new PagoVenta();
        p.setId(rs.getInt("id"));
        p.setVentaId(rs.getInt("venta_id"));
        p.setMetodoPagoId(rs.getInt("metodo_pago_id"));
        p.setMonto(rs.getDouble("monto"));
        p.setCuotas(rs.getInt("cuotas"));
        p.setComision(rs.getDouble("comision"));
        p.setObservaciones(rs.getString("observaciones"));
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            p.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        return p;
    }
}
