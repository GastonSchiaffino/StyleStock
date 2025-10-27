 package com.style.stock.controller;

import com.style.stock.exception.*;
import com.style.stock.model.Cliente;
import com.style.stock.service.ClienteService;
import com.style.stock.util.AlertUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Controlador mejorado para gestión de clientes
 */
public class ClienteController {
    private static final Logger logger = LoggerFactory.getLogger(ClienteController.class);

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colCuit;

    @FXML private TextField txtNombre;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCuit;
    @FXML private TextField txtEmail;
    @FXML private TextField txtBuscar;

    @FXML private Button btnGuardar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnNuevo;
    @FXML private Button btnCancelar;

    @FXML private ProgressIndicator progressIndicator;

    private final ClienteService clienteService;
    private final ObservableList<Cliente> clientes;
    private Cliente clienteSeleccionado;
    private boolean modoEdicion = false;

    public ClienteController() {
        this.clienteService = new ClienteService();
        this.clientes = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
        cargarClientes();
        actualizarEstadoBotones();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCuit.setCellValueFactory(new PropertyValueFactory<>("cuit"));
        tablaClientes.setItems(clientes);
    }

    private void configurarEventos() {
        tablaClientes.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                clienteSeleccionado = newVal;
                actualizarEstadoBotones();
            }
        );

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarClientes());
    }

    @FXML
    private void cargarClientes() {
        ejecutarEnBackground(() -> {
            try {
                var lista = clienteService.listarTodos();
                Platform.runLater(() -> clientes.setAll(lista));
            } catch (DataAccessException e) {
                logger.error("Error cargando clientes", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudieron cargar los clientes")
                );
            }
        });
    }

    @FXML
    private void buscarClientes() {
        String termino = txtBuscar.getText().trim();
        ejecutarEnBackground(() -> {
            try {
                var lista = termino.isEmpty() 
                    ? clienteService.listarTodos()
                    : clienteService.buscarPorNombre(termino);
                Platform.runLater(() -> clientes.setAll(lista));
            } catch (DataAccessException e) {
                logger.error("Error buscando clientes", e);
            }
        });
    }

    @FXML
    private void guardarCliente() {
        try {
            Cliente cliente = modoEdicion ? clienteSeleccionado : new Cliente();
            
            cliente.setNombre(txtNombre.getText().trim());
            cliente.setDireccion(txtDireccion.getText().trim());
            cliente.setTelefono(txtTelefono.getText().trim());
            cliente.setCuit(txtCuit.getText().trim());
            cliente.setEmail(txtEmail.getText().trim());

            ejecutarEnBackground(() -> {
                try {
                    Cliente guardado = modoEdicion 
                        ? clienteService.actualizar(cliente)
                        : clienteService.crear(cliente);

                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", 
                            modoEdicion ? "Cliente actualizado correctamente" : "Cliente guardado correctamente");
                        cargarClientes();
                        limpiarFormulario();
                        modoEdicion = false;
                    });

                } catch (ValidationException e) {
                    Platform.runLater(() -> 
                        AlertUtils.mostrarAdvertencia("Validación", "Error de validación", e.getMessage())
                    );
                } catch (DataAccessException e) {
                    logger.error("Error guardando cliente", e);
                    Platform.runLater(() -> 
                        AlertUtils.mostrarError("Error", "No se pudo guardar el cliente")
                    );
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (Exception e) {
            AlertUtils.mostrarError("Error", "Error procesando datos del cliente");
        }
    }

    @FXML
    private void editarCliente() {
        if (clienteSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un cliente para editar");
            return;
        }

        modoEdicion = true;
        txtNombre.setText(clienteSeleccionado.getNombre());
        txtDireccion.setText(clienteSeleccionado.getDireccion());
        txtTelefono.setText(clienteSeleccionado.getTelefono());
        txtCuit.setText(clienteSeleccionado.getCuit());
        txtEmail.setText(clienteSeleccionado.getEmail());
        actualizarEstadoBotones();
    }

    @FXML
    private void eliminarCliente() {
        if (clienteSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un cliente para eliminar");
            return;
        }

        Optional<ButtonType> resultado = AlertUtils.mostrarConfirmacion(
            "Confirmar eliminación",
            "¿Está seguro que desea eliminar el cliente?",
            clienteSeleccionado.getNombre()
        );

        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    clienteService.eliminar(clienteSeleccionado.getId());
                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Cliente eliminado correctamente");
                        cargarClientes();
                        limpiarFormulario();
                    });
                } catch (DataAccessException | NotFoundException e) {
                    logger.error("Error eliminando cliente", e);
                    Platform.runLater(() -> 
                        AlertUtils.mostrarError("Error", "No se pudo eliminar el cliente")
                    );
                }
            });
        }
    }

    @FXML
    private void nuevoCliente() {
        limpiarFormulario();
        modoEdicion = false;
        txtNombre.requestFocus();
    }

    @FXML
    private void cancelar() {
        limpiarFormulario();
        modoEdicion = false;
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtDireccion.clear();
        txtTelefono.clear();
        txtCuit.clear();
        txtEmail.clear();
        clienteSeleccionado = null;
        tablaClientes.getSelectionModel().clearSelection();
        actualizarEstadoBotones();
    }

    private void actualizarEstadoBotones() {
        boolean haySeleccion = clienteSeleccionado != null;
        btnEditar.setDisable(!haySeleccion || modoEdicion);
        btnEliminar.setDisable(!haySeleccion || modoEdicion);
        btnCancelar.setDisable(!modoEdicion);
        btnGuardar.setText(modoEdicion ? "Actualizar" : "Guardar");
    }

    private void ejecutarEnBackground(Runnable tarea) {
        progressIndicator.setVisible(true);
        new Thread(() -> {
            try {
                tarea.run();
            } finally {
                Platform.runLater(() -> progressIndicator.setVisible(false));
            }
        }).start();
    }
}