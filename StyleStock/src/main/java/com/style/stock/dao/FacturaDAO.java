package com.style.stock.dao;

import com.style.stock.database.DatabaseManager;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones CRUD de Facturas
 */
public class FacturaDAO {
    private static final Logger logger = LoggerFactory.getLogger(FacturaDAO.class);
    private final DatabaseManager dbManager;
    private final ClienteDAO clienteDAO;
    private final ProductoDAO productoDAO;

    public FacturaDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.clienteDAO = new ClienteDAO();
        this.productoDAO = new ProductoDAO();
    }

    /**
     * Guarda una factura completa con sus detalles (transaccional)
     */
    public Factura save(Factura factura) throws DataAccessException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar factura
            String sqlFactura = "INSERT INTO facturas (cliente_id, fecha, subtotal, descuento, total, tipo, estado, notas) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            int facturaId;
            try (PreparedStatement ps = conn.prepareStatement(sqlFactura, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, factura.getClienteId());
                ps.setString(2, factura.getFecha().toString());
                ps.setDouble(3, factura.getSubtotal());
                ps.setDouble(4, factura.getDescuento() != null ? factura.getDescuento() : 0.0);
                ps.setDouble(5, factura.getTotal());
                ps.setString(6, factura.getTipo().getValor());
                ps.setString(7, factura.getEstado().getValor());
                ps.setString(8, factura.getNotas());

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        facturaId = rs.getInt(1);
                        factura.setId(facturaId);
                    } else {
                        throw new DataAccessException("No se pudo obtener el ID de la factura");
                    }
                }
            }

            // 2. Insertar detalles y actualizar stock
            String sqlDetalle = "INSERT INTO detalle_factura (factura_id, producto_id, cantidad, precio_unitario, descuento, subtotal) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS)) {
                for (DetalleFactura detalle : factura.getDetalles()) {
                    psDetalle.setInt(1, facturaId);
                    psDetalle.setInt(2, detalle.getProductoId());
                    psDetalle.setInt(3, detalle.getCantidad());
                    psDetalle.setDouble(4, detalle.getPrecioUnitario());
                    psDetalle.setDouble(5, detalle.getDescuento() != null ? detalle.getDescuento() : 0.0);
                    psDetalle.setDouble(6, detalle.getSubtotal());
                    psDetalle.addBatch();

                    // Actualizar stock del producto
                    Producto producto = productoDAO.findById(detalle.getProductoId())
                        .orElseThrow(() -> new DataAccessException("Producto no encontrado: " + detalle.getProductoId()));
                    
                    int nuevoStock = producto.getStock() - detalle.getCantidad();
                    updateStockInTransaction(conn, producto.getId(), nuevoStock);

                    // Registrar movimiento de stock
                    registrarMovimientoStock(conn, producto.getId(), MovimientoStock.TipoMovimiento.VENTA,
                        detalle.getCantidad(), producto.getStock(), nuevoStock, 
                        "Factura #" + facturaId);
                }
                psDetalle.executeBatch();
            }

            conn.commit();
            logger.info("Factura guardada: #{} - Cliente: {}", facturaId, factura.getClienteId());
            return factura;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Transacción revertida");
                } catch (SQLException ex) {
                    logger.error("Error al revertir transacción", ex);
                }
            }
            logger.error("Error guardando factura", e);
            throw new DataAccessException("Error guardando factura: " + e.getMessage(), e);
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

    public Optional<Factura> findById(Integer id) throws DataAccessException {
        String sql = "SELECT * FROM facturas WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Factura factura = mapResultSetToFactura(rs);
                    // Cargar detalles
                    factura.setDetalles(findDetallesByFacturaId(id));
                    return Optional.of(factura);
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Error buscando factura por ID", e);
            throw new DataAccessException("Error buscando factura: " + e.getMessage(), e);
        }
    }

    public List<Factura> findAll() throws DataAccessException {
        String sql = "SELECT * FROM facturas ORDER BY fecha DESC, id DESC LIMIT 100";
        List<Factura> facturas = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Factura factura = mapResultSetToFactura(rs);
                facturas.add(factura);
            }

            return facturas;

        } catch (SQLException e) {
            logger.error("Error obteniendo facturas", e);
            throw new DataAccessException("Error obteniendo facturas: " + e.getMessage(), e);
        }
    }

    public List<Factura> findByCliente(Integer clienteId) throws DataAccessException {
        String sql = "SELECT * FROM facturas WHERE cliente_id = ? ORDER BY fecha DESC";
        List<Factura> facturas = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    facturas.add(mapResultSetToFactura(rs));
                }
            }

            return facturas;

        } catch (SQLException e) {
            logger.error("Error buscando facturas por cliente", e);
            throw new DataAccessException("Error buscando facturas: " + e.getMessage(), e);
        }
    }

    public List<Factura> findByFechaRange(LocalDate desde, LocalDate hasta) throws DataAccessException {
        String sql = "SELECT * FROM facturas WHERE fecha BETWEEN ? AND ? ORDER BY fecha DESC";
        List<Factura> facturas = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    facturas.add(mapResultSetToFactura(rs));
                }
            }

            return facturas;

        } catch (SQLException e) {
            logger.error("Error buscando facturas por rango de fechas", e);
            throw new DataAccessException("Error buscando facturas: " + e.getMessage(), e);
        }
    }

    public void anularFactura(Integer id) throws DataAccessException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // Obtener factura con detalles
            Factura factura = findById(id)
                .orElseThrow(() -> new DataAccessException("Factura no encontrada: " + id));

            if (factura.getEstado() == Factura.EstadoFactura.ANULADA) {
                throw new DataAccessException("La factura ya está anulada");
            }

            // Revertir stock
            for (DetalleFactura detalle : factura.getDetalles()) {
                Producto producto = productoDAO.findById(detalle.getProductoId())
                    .orElseThrow(() -> new DataAccessException("Producto no encontrado"));
                
                int nuevoStock = producto.getStock() + detalle.getCantidad();
                updateStockInTransaction(conn, producto.getId(), nuevoStock);

                // Registrar movimiento
                registrarMovimientoStock(conn, producto.getId(), MovimientoStock.TipoMovimiento.AJUSTE,
                    detalle.getCantidad(), producto.getStock(), nuevoStock, 
                    "Anulación Factura #" + id);
            }

            // Actualizar estado de factura
            String sql = "UPDATE facturas SET estado = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, Factura.EstadoFactura.ANULADA.getValor());
                ps.setInt(2, id);
                ps.executeUpdate();
            }

            conn.commit();
            logger.info("Factura anulada: #{}", id);

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error al revertir transacción", ex);
                }
            }
            logger.error("Error anulando factura", e);
            throw new DataAccessException("Error anulando factura: " + e.getMessage(), e);
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

    private List<DetalleFactura> findDetallesByFacturaId(Integer facturaId) throws DataAccessException {
        String sql = "SELECT * FROM detalle_factura WHERE factura_id = ?";
        List<DetalleFactura> detalles = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, facturaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detalles.add(mapResultSetToDetalle(rs));
                }
            }

            return detalles;

        } catch (SQLException e) {
            logger.error("Error obteniendo detalles de factura", e);
            throw new DataAccessException("Error obteniendo detalles: " + e.getMessage(), e);
        }
    }

    private void updateStockInTransaction(Connection conn, Integer productoId, Integer nuevoStock) 
            throws SQLException {
        String sql = "UPDATE productos SET stock = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setInt(2, productoId);
            ps.executeUpdate();
        }
    }

    private void registrarMovimientoStock(Connection conn, Integer productoId, 
            MovimientoStock.TipoMovimiento tipo, Integer cantidad, Integer stockAnterior, 
            Integer stockNuevo, String referencia) throws SQLException {
        
        String sql = "INSERT INTO movimientos_stock (producto_id, tipo, cantidad, stock_anterior, stock_nuevo, referencia) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ps.setString(2, tipo.getValor());
            ps.setInt(3, cantidad);
            ps.setInt(4, stockAnterior);
            ps.setInt(5, stockNuevo);
            ps.setString(6, referencia);
            ps.executeUpdate();
        }
    }

    private Factura mapResultSetToFactura(ResultSet rs) throws SQLException, DataAccessException {
        Factura f = new Factura();
        f.setId(rs.getInt("id"));
        f.setNumeroFactura(rs.getString("numero_factura"));
        f.setClienteId(rs.getInt("cliente_id"));
        f.setFecha(LocalDate.parse(rs.getString("fecha")));
        f.setSubtotal(rs.getDouble("subtotal"));
        f.setDescuento(rs.getDouble("descuento"));
        f.setTotal(rs.getDouble("total"));
        f.setTipo(Factura.TipoFactura.valueOf(rs.getString("tipo")));
        f.setEstado(Factura.EstadoFactura.valueOf(rs.getString("estado")));
        f.setNotas(rs.getString("notas"));
        
        // Cargar cliente
        clienteDAO.findById(f.getClienteId()).ifPresent(f::setCliente);
        
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            f.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        
        return f;
    }

    private DetalleFactura mapResultSetToDetalle(ResultSet rs) throws SQLException, DataAccessException {
        DetalleFactura d = new DetalleFactura();
        d.setId(rs.getInt("id"));
        d.setFacturaId(rs.getInt("factura_id"));
        d.setProductoId(rs.getInt("producto_id"));
        d.setCantidad(rs.getInt("cantidad"));
        d.setPrecioUnitario(rs.getDouble("precio_unitario"));
        d.setDescuento(rs.getDouble("descuento"));
        d.setSubtotal(rs.getDouble("subtotal"));
        
        // Cargar producto
        productoDAO.findById(d.getProductoId()).ifPresent(d::setProducto);
        
        return d;
    }
}