package com.style.stock.controller;

import com.style.stock.exception.*;
import com.style.stock.model.*;
import com.style.stock.service.*;
import com.style.stock.util.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Controlador UNIFICADO para Productos y Variantes
 * Vista Master-Detail: Productos arriba, Variantes abajo
 */
public class ProductoVarianteController {
    private static final Logger logger = LoggerFactory.getLogger(ProductoVarianteController.class);

    // Servicios
    private final ProductoService productoService;
    private final VarianteService varianteService;
    private final CategoriaService categoriaService;

    // ========== TABLA PRODUCTOS ==========
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, Integer> colProdId;
    @FXML private TableColumn<Producto, String> colProdCodigo;
    @FXML private TableColumn<Producto, String> colProdNombre;
    @FXML private TableColumn<Producto, String> colProdCategoria;
    @FXML private TableColumn<Producto, String> colProdMarca;
    @FXML private TableColumn<Producto, Double> colProdPrecioMin;
    @FXML private TableColumn<Producto, Double> colProdPrecioMay;
    @FXML private TableColumn<Producto, Integer> colProdVariantes;

    // ========== TABLA VARIANTES ==========
    @FXML private TableView<Variante> tablaVariantes;
    @FXML private TableColumn<Variante, Integer> colVarId;
    @FXML private TableColumn<Variante, String> colVarSku;
    @FXML private TableColumn<Variante, String> colVarAtributos;
    @FXML private TableColumn<Variante, Double> colVarPrecioCosto;
    @FXML private TableColumn<Variante, Double> colVarPrecioMin;
    @FXML private TableColumn<Variante, Double> colVarPrecioMay;
    @FXML private TableColumn<Variante, Integer> colVarStock;
    @FXML private TableColumn<Variante, Integer> colVarStockMin;

    // ========== COMPONENTES UI ==========
    @FXML private TextField txtBuscar;
    @FXML private Label lblInfoProducto;
    @FXML private Label lblProductoSeleccionado;
    @FXML private Label lblTotalVariantes;
    @FXML private Button btnNuevoProducto;
    @FXML private Button btnEditarProducto;
    @FXML private Button btnEliminarProducto;
    @FXML private Button btnNuevaVariante;
    @FXML private Button btnAjustarStock;
    @FXML private Button btnEditarVariante;
    @FXML private Button btnEliminarVariante;
    @FXML private ProgressIndicator progressIndicator;

    // Datos
    private final ObservableList<Producto> productos;
    private final ObservableList<Variante> variantes;
    private Producto productoSeleccionado;
    private Variante varianteSeleccionada;

    public ProductoVarianteController() {
        this.productoService = new ProductoService();
        this.varianteService = new VarianteService();
        this.categoriaService = new CategoriaService();
        this.productos = FXCollections.observableArrayList();
        this.variantes = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTablaProductos();
        configurarTablaVariantes();
        configurarEventos();
        cargarProductos();
    }

    // ============================================
    // CONFIGURACIÓN TABLAS
    // ============================================

    private void configurarTablaProductos() {
        colProdId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProdCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colProdNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        colProdCategoria.setCellValueFactory(cellData -> {
            Producto p = cellData.getValue();
            if (p.getCategoria() != null) {
                return new SimpleStringProperty(p.getCategoria().getNombre());
            }
            return new SimpleStringProperty("-");
        });

        colProdMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colProdPrecioMin.setCellValueFactory(new PropertyValueFactory<>("precioMinorista"));
        colProdPrecioMay.setCellValueFactory(new PropertyValueFactory<>("precioMayorista"));

        colProdVariantes.setCellValueFactory(cellData -> {
            // Placeholder: contar variantes del producto
            return new javafx.beans.property.SimpleObjectProperty<>(0);
        });

        // Formatear precios
        colProdPrecioMin.setCellFactory(col -> createMoneyCell());
        colProdPrecioMay.setCellFactory(col -> createMoneyCell());

        tablaProductos.setItems(productos);

        // Doble clic para editar
        tablaProductos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && productoSeleccionado != null) {
                editarProducto();
            }
        });
    }

    private void configurarTablaVariantes() {
        colVarId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colVarSku.setCellValueFactory(new PropertyValueFactory<>("sku"));

        colVarAtributos.setCellValueFactory(cellData -> {
            Variante v = cellData.getValue();
            if (v.getAtributos() != null && !v.getAtributos().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < v.getAtributos().size(); i++) {
                    VarianteAtributo va = v.getAtributos().get(i);
                    sb.append(va.getValor());
                    if (i < v.getAtributos().size() - 1) sb.append(", ");
                }
                return new SimpleStringProperty(sb.toString());
            }
            return new SimpleStringProperty("-");
        });

        colVarPrecioCosto.setCellValueFactory(new PropertyValueFactory<>("precioCosto"));
        colVarPrecioMin.setCellValueFactory(new PropertyValueFactory<>("precioMinorista"));
        colVarPrecioMay.setCellValueFactory(new PropertyValueFactory<>("precioMayorista"));
        colVarStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colVarStockMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        // Formatear precios
        colVarPrecioCosto.setCellFactory(col -> createMoneyCell());
        colVarPrecioMin.setCellFactory(col -> createMoneyCell());
        colVarPrecioMay.setCellFactory(col -> createMoneyCell());

        // Resaltar stock bajo en ROJO
        colVarStock.setCellFactory(col -> new TableCell<Variante, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stock.toString());
                    TableRow<Variante> row = getTableRow();
                    if (row != null && row.getItem() != null) {
                        Variante v = row.getItem();
                        if (v.isStockBajo()) {
                            setStyle("-fx-text-fill: #FF5722; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            }
        });

        tablaVariantes.setItems(variantes);

        // Doble clic para editar
        tablaVariantes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && varianteSeleccionada != null) {
                editarVariante();
            }
        });
    }

    private <T> TableCell<T, Double> createMoneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                setText(empty || valor == null ? null : String.format("$%.2f", valor));
            }
        };
    }

    // ============================================
    // EVENTOS
    // ============================================

    private void configurarEventos() {
        // Selección de producto
        tablaProductos.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    productoSeleccionado = newVal;
                    actualizarEstadoBotonesProducto();
                    if (newVal != null) {
                        cargarVariantesDelProducto(newVal);
                    } else {
                        variantes.clear();
                        lblProductoSeleccionado.setText("Ninguno");
                        lblInfoProducto.setText("Seleccione un producto para ver sus variantes");
                    }
                }
        );

        // Selección de variante
        tablaVariantes.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    varianteSeleccionada = newVal;
                    actualizarEstadoBotonesVariante();
                }
        );

        // Búsqueda en tiempo real
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarProductos());
    }

    // ============================================
    // CARGA DE DATOS
    // ============================================

    private void cargarProductos() {
        ejecutarEnBackground(() -> {
            try {
                var lista = productoService.listarTodos();

                // Cargar categorías para cada producto
                for (Producto p : lista) {
                    try {
                        Categoria cat = categoriaService.buscarPorId(p.getCategoriaId());
                        p.setCategoria(cat);
                    } catch (NotFoundException e) {
                        logger.warn("Categoría no encontrada para producto: {}", p.getId());
                    }
                }

                Platform.runLater(() -> {
                    productos.setAll(lista);
                    logger.debug("Productos cargados: {}", lista.size());
                });
            } catch (DataAccessException e) {
                logger.error("Error cargando productos", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudieron cargar los productos")
                );
            }
        });
    }

    private void cargarVariantesDelProducto(Producto producto) {
        lblProductoSeleccionado.setText(producto.getNombre());
        lblInfoProducto.setText("Cargando variantes...");

        ejecutarEnBackground(() -> {
            try {
                var lista = varianteService.listarPorProducto(producto.getId());

                // Asignar producto a cada variante
                for (Variante v : lista) {
                    v.setProducto(producto);
                }

                Platform.runLater(() -> {
                    variantes.setAll(lista);
                    lblTotalVariantes.setText("Total variantes: " + lista.size());
                    lblInfoProducto.setText(
                            lista.isEmpty() ?
                                    "Este producto no tiene variantes. Cree una nueva." :
                                    String.format("Mostrando %d variante(s)", lista.size())
                    );
                });
            } catch (DataAccessException e) {
                logger.error("Error cargando variantes", e);
                Platform.runLater(() -> {
                    variantes.clear();
                    lblInfoProducto.setText("Error cargando variantes");
                });
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
                        : productoService.buscarPorNombre(termino);

                // Cargar categorías
                for (Producto p : lista) {
                    try {
                        Categoria cat = categoriaService.buscarPorId(p.getCategoriaId());
                        p.setCategoria(cat);
                    } catch (NotFoundException e) {
                        // Ignorar
                    }
                }

                Platform.runLater(() -> productos.setAll(lista));
            } catch (DataAccessException e) {
                logger.error("Error buscando productos", e);
            }
        });
    }

    // ============================================
    // ACCIONES PRODUCTOS
    // ============================================

    @FXML
    private void nuevoProducto() {
        abrirDialogoProducto(null);
    }

    @FXML
    private void editarProducto() {
        if (productoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un producto");
            return;
        }
        abrirDialogoProducto(productoSeleccionado);
    }

    @FXML
    private void eliminarProducto() {
        if (productoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un producto");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Está seguro de eliminar el producto?",
                productoSeleccionado.getNombre() + "\n\n⚠️ Esto eliminará todas sus variantes"
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    productoService.eliminar(productoSeleccionado.getId());
                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Producto eliminado correctamente");
                        cargarProductos();
                        variantes.clear();
                    });
                } catch (BusinessException | DataAccessException | NotFoundException e) {
                    logger.error("Error eliminando producto", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", "No se pudo eliminar el producto", e.getMessage())
                    );
                }
            });
        }
    }

    // ============================================
    // ACCIONES VARIANTES
    // ============================================

    @FXML
    private void nuevaVariante() {
        if (productoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un producto primero");
            return;
        }
        abrirDialogoVariante(null, productoSeleccionado);
    }

    @FXML
    private void editarVariante() {
        if (varianteSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una variante");
            return;
        }
        abrirDialogoVariante(varianteSeleccionada, productoSeleccionado);
    }

    @FXML
    private void eliminarVariante() {
        if (varianteSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una variante");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Está seguro de eliminar esta variante?",
                "SKU: " + varianteSeleccionada.getSku()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            AlertUtils.mostrarAdvertencia("En desarrollo",
                    "La eliminación de variantes estará disponible próximamente");
        }
    }

    @FXML
    private void ajustarStock() {
        if (varianteSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una variante");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Ajustar Stock");
        dialog.setHeaderText("Ajuste de stock para: " + varianteSeleccionada.getSku());
        dialog.setContentText("Cantidad (+ para ingreso, - para egreso):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cantidad -> {
            try {
                int cant = Integer.parseInt(cantidad);
                ejecutarEnBackground(() -> {
                    try {
                        varianteService.ajustarStock(varianteSeleccionada.getId(), cant, "Ajuste manual");
                        Platform.runLater(() -> {
                            AlertUtils.mostrarExito("Éxito", "Stock ajustado correctamente");
                            if (productoSeleccionado != null) {
                                cargarVariantesDelProducto(productoSeleccionado);
                            }
                        });
                    } catch (BusinessException | DataAccessException | NotFoundException e) {
                        logger.error("Error ajustando stock", e);
                        Platform.runLater(() ->
                                AlertUtils.mostrarError("Error", "No se pudo ajustar el stock")
                        );
                    }
                });
            } catch (NumberFormatException e) {
                AlertUtils.mostrarAdvertencia("Valor inválido", "Ingrese un número válido");
            }
        });
    }

    @FXML
    private void mostrarStockBajo() {
        ejecutarEnBackground(() -> {
            try {
                var lista = varianteService.listarStockBajo();
                Platform.runLater(() -> {
                    if (lista.isEmpty()) {
                        AlertUtils.mostrarInfo("Stock Bajo", "¡Excelente! No hay productos con stock bajo.");
                    } else {
                        // Mostrar diálogo con lista de stock bajo
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Stock Bajo");
                        alert.setHeaderText(String.format("Hay %d variante(s) con stock bajo", lista.size()));

                        StringBuilder content = new StringBuilder();
                        for (Variante v : lista) {
                            content.append(String.format("• %s - Stock: %d (Mín: %d)\n",
                                    v.getSku(), v.getStock(), v.getStockMinimo()));
                        }

                        TextArea textArea = new TextArea(content.toString());
                        textArea.setEditable(false);
                        textArea.setWrapText(true);
                        textArea.setPrefHeight(300);

                        alert.getDialogPane().setContent(textArea);
                        alert.showAndWait();
                    }
                });
            } catch (DataAccessException e) {
                logger.error("Error obteniendo stock bajo", e);
            }
        });
    }

    // ============================================
    // DIÁLOGOS (MODALES)
    // ============================================

    private void abrirDialogoProducto(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/producto-form.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(producto == null ? "Nuevo Producto" : "Editar Producto");
            stage.setScene(new Scene(loader.load()));

            // TODO: Pasar producto al controlador del diálogo
            // ProductoFormController controller = loader.getController();
            // controller.setProducto(producto);

            stage.showAndWait();
            cargarProductos(); // Recargar después de cerrar

        } catch (IOException e) {
            logger.error("Error abriendo diálogo de producto", e);
            AlertUtils.mostrarError("Error", "No se pudo abrir el formulario");
        }
    }

    private void abrirDialogoVariante(Variante variante, Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/variante-form.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(variante == null ? "Nueva Variante" : "Editar Variante");
            stage.setScene(new Scene(loader.load()));

            // TODO: Pasar variante y producto al controlador del diálogo
            // VarianteFormController controller = loader.getController();
            // controller.setDatos(variante, producto);

            stage.showAndWait();
            if (productoSeleccionado != null) {
                cargarVariantesDelProducto(productoSeleccionado); // Recargar variantes
            }

        } catch (IOException e) {
            logger.error("Error abriendo diálogo de variante", e);
            AlertUtils.mostrarError("Error", "No se pudo abrir el formulario");
        }
    }

    // ============================================
    // ACTUALIZACIÓN DE BOTONES
    // ============================================

    private void actualizarEstadoBotonesProducto() {
        boolean haySeleccion = productoSeleccionado != null;
        btnEditarProducto.setDisable(!haySeleccion);
        btnEliminarProducto.setDisable(!haySeleccion);
        btnNuevaVariante.setDisable(!haySeleccion);
    }

    private void actualizarEstadoBotonesVariante() {
        boolean haySeleccion = varianteSeleccionada != null;
        btnAjustarStock.setDisable(!haySeleccion);
        btnEditarVariante.setDisable(!haySeleccion);
        btnEliminarVariante.setDisable(!haySeleccion);
    }

    // ============================================
    // UTILIDADES
    // ============================================

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