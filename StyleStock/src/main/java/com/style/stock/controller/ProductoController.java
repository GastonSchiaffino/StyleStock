package com.style.stock.controller;

import com.style.stock.model.Database;
import com.style.stock.model.Producto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;

public class ProductoController {
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colDescripcion;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;

    @FXML private TextField txtCodigo, txtDescripcion, txtPrecio, txtStock;
    @FXML private Button btnGuardar, btnEliminar;

    private ObservableList<Producto> productos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        tablaProductos.setItems(productos);
        cargarProductos();
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

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error cargando productos: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void guardarProductos() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Código requerido").showAndWait(); return; }
        try (Connection conn = Database.connect()) {
            String sql = "INSERT INTO productos (codigo, descripcion, precio, stock) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, codigo);
            ps.setString(2, txtDescripcion.getText().trim());
            ps.setDouble(3, Double.parseDouble(txtPrecio.getText().trim()));
            ps.setInt(4, Integer.parseInt(txtStock.getText().trim()));
            ps.executeUpdate();
            limpiarCampos();
            cargarProductos();
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error guardando producto: "+ex.getMessage()).showAndWait();
        }
    }

    @FXML
    public void eliminarProducto() {
        Producto sel = tablaProductos.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Seleccione un producto.").showAndWait(); return; }
        try (Connection conn = Database.connect()) {
            String sql = "DELETE FROM productos WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sel.getId());
            ps.executeUpdate();
            cargarProductos();
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error eliminando producto: "+ex.getMessage()).showAndWait();
        }
    }

    private void limpiarCampos() {
        txtCodigo.clear(); txtDescripcion.clear(); txtPrecio.clear(); txtStock.clear();
    }
}
