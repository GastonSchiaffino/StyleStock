package com.style.stock.controller;

import com.style.stock.model.Database;
import com.style.stock.model.Producto;
import com.style.stock.model.Cliente;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;

public class FacturaController {
    @FXML private ComboBox<Cliente> cbClientes;
    @FXML private ComboBox<Producto> cbProductos;
    @FXML private TextField txtCantidad;
    @FXML private TableView<FacturaItem> tablaItems;
    @FXML private Label lblTotal;
    @FXML private Button btnAgregar, btnGuardar, btnPDF;
    @FXML private TableColumn<FacturaItem, String> colCodigo;
    @FXML private TableColumn<FacturaItem, String> colDescripcion;
    @FXML private TableColumn<FacturaItem, Double> colPrecio;
    @FXML private TableColumn<FacturaItem, Integer> colCantidad;
    @FXML private TableColumn<FacturaItem, Double> colSubtotal;

    private ObservableList<Producto> productos = FXCollections.observableArrayList();
    private ObservableList<Cliente> clientes = FXCollections.observableArrayList();
    private ObservableList<FacturaItem> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        cbClientes.setItems(clientes);
        cbProductos.setItems(productos);
        tablaItems.setItems(items);
        cargarProductos();
        cargarClientes();
        updateTotal();
    }

    void cargarProductos() {
        productos.clear();
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM productos")) {
            while (rs.next()) {
                productos.add(new Producto(
                        rs.getInt("id"),
                        rs.getString("codigo"),
                        rs.getString("descripcion"),
                        rs.getDouble("precio"),
                        rs.getInt("stock")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    void cargarClientes() {
        clientes.clear();
        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM clientes")) {
            while (rs.next()) {
                clientes.add(new Cliente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("direccion"),
                        rs.getString("telefono"),
                        rs.getString("cuit")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    public void agregarItem() {
        Producto p = cbProductos.getSelectionModel().getSelectedItem();
        if (p == null) { new Alert(Alert.AlertType.WARNING, "Seleccione una producto").showAndWait(); return; }
        int qty = 1;
        try { qty = Integer.parseInt(txtCantidad.getText().trim()); } catch (Exception e) {}
        if (qty <= 0) { new Alert(Alert.AlertType.WARNING, "Cantidad inválida").showAndWait(); return; }
        items.add(new FacturaItem(p.getId(), p.getCodigo(), p.getDescripcion(), p.getPrecio(), qty));
        updateTotal();
    }

    @FXML
    public void guardarFactura() {
        Cliente c = cbClientes.getSelectionModel().getSelectedItem();
        if (c == null) { new Alert(Alert.AlertType.WARNING, "Seleccione un cliente").showAndWait(); return; }
        if (items.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Agregue items").showAndWait(); return; }
        double total = items.stream().mapToDouble(FacturaItem::getSubtotal).sum();
        String fecha = LocalDate.now().toString();
        try (Connection conn = Database.connect()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO facturas (cliente_id, fecha, total, tipo) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, c.getId());
            ps.setString(2, fecha);
            ps.setDouble(3, total);
            ps.setString(4, "A");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            int facturaId = -1;
            if (keys.next()) facturaId = keys.getInt(1);

            String sqlDet = "INSERT INTO detalle_factura (factura_id, producto_id, cantidad, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement psd = conn.prepareStatement(sqlDet);
            for (FacturaItem it : items) {
                psd.setInt(1, facturaId);
                psd.setInt(2, it.getProductoId());
                psd.setInt(3, it.getCantidad());
                psd.setDouble(4, it.getPrecioUnitario());
                psd.setDouble(5, it.getSubtotal());
                psd.addBatch();

                // update stock
                String upd = "UPDATE productos SET stock = stock - ? WHERE id = ?";
                PreparedStatement pu = conn.prepareStatement(upd);
                pu.setInt(1, it.getCantidad());
                pu.setInt(2, it.getProductoId());
                pu.executeUpdate();
            }
            psd.executeBatch();
            conn.commit();
            cargarProductos();
            cbProductos.setItems(productos);
            // clear
            items.clear();
            updateTotal();
            new Alert(Alert.AlertType.INFORMATION, "Factura guardada. ID: " + facturaId).showAndWait();
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error guardando factura: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    public void generarPDF() {
        // generate PDF of current items (doesn't require saving first)
        Cliente c = cbClientes.getSelectionModel().getSelectedItem();
        if (c == null) { new Alert(Alert.AlertType.WARNING, "Seleccione un cliente").showAndWait(); return; }
        if (items.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Agregue items").showAndWait(); return; }

        try {
            Path dir = Path.of(System.getProperty("user.home"), "style-stock", "facturas");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            int seq = (int)(System.currentTimeMillis()/1000);
            Path file = dir.resolve("factura_" + seq + ".pdf");

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    cs.newLineAtOffset(50, 720);
                    cs.showText("Style Stock - FACTURA");
                    cs.newLineAtOffset(0, -20);
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.showText("Cliente: " + c.getNombre());
                    cs.newLineAtOffset(0, -15);
                    cs.showText("Fecha: " + java.time.LocalDate.now().toString());
                    cs.newLineAtOffset(0, -20);
                    cs.showText("--------------------------------------------");
                    cs.newLineAtOffset(0, -15);
                    for (FacturaItem it : items) {
                        cs.showText(it.getCantidad() + " x " + it.getDescripcion() + " @ " + it.getPrecioUnitario() + " = " + it.getSubtotal());
                        cs.newLineAtOffset(0, -12);
                    }
                    cs.newLineAtOffset(0, -10);
                    cs.showText("--------------------------------------------");
                    cs.newLineAtOffset(0, -15);
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.showText("TOTAL: " + items.stream().mapToDouble(FacturaItem::getSubtotal).sum());
                    cs.endText();
                }
                doc.save(file.toFile());
            }
            new Alert(Alert.AlertType.INFORMATION, "PDF generado en: " + file.toString()).showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error generando PDF: " + ex.getMessage()).showAndWait();
        }
    }

    private void updateTotal() {
        double total = items.stream().mapToDouble(FacturaItem::getSubtotal).sum();
        lblTotal.setText(String.format("%.2f", total));
    }

    // Inner class for table items
    public static class FacturaItem {
        private int productoId;
        private String codigo;
        private String descripcion;
        private double precioUnitario;
        private int cantidad;

        public FacturaItem(int productoId, String codigo, String descripcion, double precioUnitario, int cantidad) {
            this.productoId = productoId;
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.precioUnitario = precioUnitario;
            this.cantidad = cantidad;
        }

        public int getProductoId() { return productoId; }
        public String getCodigo() { return codigo; }
        public String getDescripcion() { return descripcion; }
        public double getPrecioUnitario() { return precioUnitario; }
        public int getCantidad() { return cantidad; }
        public double getSubtotal() { return precioUnitario * cantidad; }
    }
}
