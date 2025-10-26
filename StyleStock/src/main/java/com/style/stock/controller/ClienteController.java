package com.style.stock.controller;

import com.style.stock.model.Cliente;
import com.style.stock.model.Database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;

public class ClienteController {
    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TextField txtNombre, txtDireccion, txtTelefono, txtCuit;
    @FXML private Button btnGuardar, btnEliminar;

    private ObservableList<Cliente> clientes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        tablaClientes.setItems(clientes);
        cargarClientes();
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
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error cargando clientes: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void guardarCliente() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Nombre requerido").showAndWait(); return; }
        try (Connection conn = Database.connect()) {
            String sql = "INSERT INTO clientes (nombre, direccion, telefono, cuit) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.setString(2, txtDireccion.getText().trim());
            ps.setString(3, txtTelefono.getText().trim());
            ps.setString(4, txtCuit.getText().trim());
            ps.executeUpdate();
            limpiarCampos();
            cargarClientes();
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error guardando cliente: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    public void eliminarCliente() {
        Cliente sel = tablaClientes.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Seleccione un cliente.").showAndWait(); return; }
        try (Connection conn = Database.connect()) {
            String sql = "DELETE FROM clientes WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, sel.getId());
            ps.executeUpdate();
            cargarClientes();
        } catch (SQLException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error eliminando cliente: " + ex.getMessage()).showAndWait();
        }
    }

    private void limpiarCampos() {
        txtNombre.clear(); txtDireccion.clear(); txtTelefono.clear(); txtCuit.clear();
    }
}
