package com.style.stock.controller;

import com.style.stock.exception.*;
import com.style.stock.model.Producto;
import com.style.stock.service.ProductoService;
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
 * Controlador mejorado para gestión de productos
 */
public class ProductoController {
    private static final Logger logger = LoggerFactory.getLogger(ProductoController.class);

    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colDescripcion;
    @FXML private TableColumn<Producto, Double> colPrecio;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, Integer> colStockMinimo;

    @FXML private TextField txtCodigo;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;
    @FXML private TextField txtStockMinimo;
    @FXML private TextField txtCategoria;
    @FXML private TextField txtBuscar;

    @FXML private Button btnGuardar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnNuevo;
    @FXML private Button btnCancelar;

    @FXML private ProgressIndicator progressIndicator;

    private final ProductoService productoService;
    private final ObservableList<Producto> productos;
    private Producto productoSeleccionado;
    private boolean modoEdicion = false;

    public ProductoController() {
        this.productoService = new ProductoService();
        this.productos = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
        cargarProductos();
        actualizarEstadoBotones();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStockMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        // Formatear columna de precio
        colPrecio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                if (empty || precio == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", precio));
                }
            }
        });

        // Resaltar stock bajo
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stock.toString());
                    Producto producto = getTableView().getItems().get(getIndex());
                    if (producto != null && producto.isStockBajo()) {
                        setStyle("-fx-background-color: #ffcccc;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        tablaProductos.setItems(productos);
    }

    private void configurarEventos() {
        // Selección en tabla
        tablaProductos.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                productoSeleccionado = newVal;
                actualizarEstadoBotones();
            }
        );

        // Búsqueda en tiempo real
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarProductos());

        // Validación de números
        txtPrecio.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                txtPrecio.setText(oldVal);
            }
        });

        txtStock.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtStock.setText(oldVal);
            }
        });

        txtStockMinimo.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtStockMinimo.setText(oldVal);
            }
        });
    }

    @FXML
    private void cargarProductos() {
        ejecutarEnBackground(() -> {
            try {
                var lista = productoService.listarTodos();
                Platform.runLater(() -> {
                    productos.setAll(lista);
                    logger.debug("Productos cargados: {}", lista.size());
                });
            } catch (DataAccessException e) {
                logger.error("Error cargando productos", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudieron cargar los productos", e.getMessage())
                );
            }
        });
    }

    @FXML
    private void buscarProductos() {
        String termino = txtBuscar.getText().trim();
        ejecutarEnBackground(() -> {
            try {
                var lista = termino.isEmpty() 
                    ? productoService.listarTodos()
                    : productoService.buscarPorDescripcion(termino);
                
                Platform.runLater(() -> productos.setAll(lista));
            } catch (DataAccessException e) {
                logger.error("Error buscando productos", e);
            }
        });
    }

    @FXML
    private void guardarProducto() {
        try {
            Producto producto = modoEdicion ? productoSeleccionado : new Producto();
            
            // Mapear datos del formulario
            producto.setCodigo(txtCodigo.getText().trim());
            producto.setDescripcion(txtDescripcion.getText().trim());
            producto.setPrecio(Double.parseDouble(txtPrecio.getText().trim()));
            producto.setStock(Integer.parseInt(txtStock.getText().trim()));
            producto.setStockMinimo(txtStockMinimo.getText().trim().isEmpty() 
                ? 5 
                : Integer.parseInt(txtStockMinimo.getText().trim()));
            producto.setCategoria(txtCategoria.getText().trim());

            ejecutarEnBackground(() -> {
                try {
                    Producto guardado = modoEdicion 
                        ? productoService.actualizar(producto)
                        : productoService.crear(producto);

                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", 
                            modoEdicion ? "Producto actualizado correctamente" : "Producto guardado correctamente");
                        cargarProductos();
                        limpiarFormulario();
                        modoEdicion = false;
                    });

                } catch (ValidationException e) {
                    Platform.runLater(() -> 
                        AlertUtils.mostrarAdvertencia("Validación", "Error de validación", e.getMessage())
                    );
                } catch (BusinessException | DataAccessException e) {
                    logger.error("Error guardando producto", e);
                    Platform.runLater(() -> 
                        AlertUtils.mostrarError("Error", "No se pudo guardar el producto", e.getMessage())
                    );
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (NumberFormatException e) {
            AlertUtils.mostrarAdvertencia("Datos inválidos", "Por favor ingrese valores numéricos válidos");
        }
    }

    @FXML
    private void editarProducto() {
        if (productoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un producto para editar");
            return;
        }

        modoEdicion = true;
        txtCodigo.setText(productoSeleccionado.getCodigo());
        txtDescripcion.setText(productoSeleccionado.getDescripcion());
        txtPrecio.setText(productoSeleccionado.getPrecio().toString());
        txtStock.setText(productoSeleccionado.getStock().toString());
        txtStockMinimo.setText(productoSeleccionado.getStockMinimo().toString());
        txtCategoria.setText(productoSeleccionado.getCategoria());

        txtCodigo.setDisable(true); // No permitir cambiar código en edición
        actualizarEstadoBotones();
    }

    @FXML
    private void eliminarProducto() {
        if (productoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un producto para eliminar");
            return;
        }

        Optional<ButtonType> resultado = AlertUtils.mostrarConfirmacion(
            "Confirmar eliminación",
            "¿Está seguro que desea eliminar el producto?",
            productoSeleccionado.getDescripcion()
        );

        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    productoService.eliminar(productoSeleccionado.getId());
                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Producto eliminado correctamente");
                        cargarProductos();
                        limpiarFormulario();
                    });
                } catch (BusinessException | DataAccessException e) {
                    logger.error("Error eliminando producto", e);
                    Platform.runLater(() -> 
                        AlertUtils.mostrarError("Error", "No se pudo eliminar el producto", e.getMessage())
                    );
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @FXML
    private void nuevoProducto() {
        limpiarFormulario();
        modoEdicion = false;
        txtCodigo.setDisable(false);
        txtCodigo.requestFocus();
    }

    @FXML
    private void cancelar() {
        limpiarFormulario();
        modoEdicion = false;
    }

    private void limpiarFormulario() {
        txtCodigo.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtStock.clear();
        txtStockMinimo.setText("5");
        txtCategoria.clear();
        txtCodigo.setDisable(false);
        productoSeleccionado = null;
        tablaProductos.getSelectionModel().clearSelection();
        actualizarEstadoBotones();
    }

    private void actualizarEstadoBotones() {
        boolean haySeleccion = productoSeleccionado != null;
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

    @FXML
    private void mostrarStockBajo() {
        ejecutarEnBackground(() -> {
            try {
                var lista = productoService.listarStockBajo();
                Platform.runLater(() -> {
                    if (lista.isEmpty()) {
                        AlertUtils.mostrarInfo("Stock", "No hay productos con stock bajo");
                    } else {
                        productos.setAll(lista);
                        AlertUtils.mostrarInfo("Stock Bajo", 
                            String.format("Se encontraron %d productos con stock bajo", lista.size()));
                    }
                });
            } catch (DataAccessException e) {
                logger.error("Error obteniendo stock bajo", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudo consultar el stock")
                );
            }
        });
    }
}