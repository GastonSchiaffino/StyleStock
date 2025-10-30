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
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Controlador para gestión de variantes con atributos dinámicos
 */
public class VarianteController {
    private static final Logger logger = LoggerFactory.getLogger(VarianteController.class);

    // Servicios
    private final VarianteService varianteService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final AtributoService atributoService;

    // Componentes UI - Filtros
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Producto> cbProductos;
    @FXML private ComboBox<Categoria> cbCategorias;

    // Componentes UI - Formulario
    @FXML private TitledPane panelFormulario;
    @FXML private ComboBox<Producto> cbProductoBase;
    @FXML private TextField txtSku;
    @FXML private TextField txtCodigoBarras;
    @FXML private TextField txtPrecioCosto;
    @FXML private TextField txtPrecioMinorista;
    @FXML private TextField txtPrecioMayorista;
    @FXML private TextField txtStock;
    @FXML private TextField txtStockMinimo;
    @FXML private VBox vboxAtributos;

    // Botones
    @FXML private Button btnNuevaVariante;
    @FXML private Button btnGuardarVariante;
    @FXML private Button btnCancelarVariante;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    // Tabla
    @FXML private TableView<Variante> tablaVariantes;
    @FXML private TableColumn<Variante, Integer> colId;
    @FXML private TableColumn<Variante, String> colSku;
    @FXML private TableColumn<Variante, String> colProducto;
    @FXML private TableColumn<Variante, String> colAtributos;
    @FXML private TableColumn<Variante, Double> colPrecioMinorista;
    @FXML private TableColumn<Variante, Double> colPrecioMayorista;
    @FXML private TableColumn<Variante, Integer> colStock;
    @FXML private TableColumn<Variante, Integer> colStockMinimo;

    @FXML private Label lblTotalVariantes;
    @FXML private ProgressIndicator progressIndicator;

    // Datos
    private final ObservableList<Variante> variantes;
    private final ObservableList<Producto> productos;
    private final ObservableList<Categoria> categorias;
    private Variante varianteSeleccionada;
    private boolean modoEdicion = false;
    private Map<Integer, ComboBox<String>> atributosComboBoxes;

    public VarianteController() {
        this.varianteService = new VarianteService();
        this.productoService = new ProductoService();
        this.categoriaService = new CategoriaService();
        this.atributoService = new AtributoService();
        this.variantes = FXCollections.observableArrayList();
        this.productos = FXCollections.observableArrayList();
        this.categorias = FXCollections.observableArrayList();
        this.atributosComboBoxes = new HashMap<>();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
        cargarDatosIniciales();
        actualizarEstadoBotones();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        
        colProducto.setCellValueFactory(cellData -> {
            Variante v = cellData.getValue();
            if (v.getProducto() != null) {
                return new SimpleStringProperty(v.getProducto().getNombre());
            }
            return new SimpleStringProperty("");
        });

        colAtributos.setCellValueFactory(cellData -> {
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

        colPrecioMinorista.setCellValueFactory(new PropertyValueFactory<>("precioMinorista"));
        colPrecioMayorista.setCellValueFactory(new PropertyValueFactory<>("precioMayorista"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStockMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        // Formatear precios
        colPrecioMinorista.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                setText(empty || precio == null ? null : String.format("$%.2f", precio));
            }
        });

        colPrecioMayorista.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                setText(empty || precio == null ? null : String.format("$%.2f", precio));
            }
        });

        // Resaltar stock bajo en ROJO
        colStock.setCellFactory(col -> new TableCell<>() {
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
    }

    private void configurarEventos() {
        tablaVariantes.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                varianteSeleccionada = newVal;
                actualizarEstadoBotones();
            }
        );

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarVariantes());

        cbProductoBase.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarAtributosDinamicos(newVal);
                heredarPreciosProducto(newVal);
            }
        });

        // Validaciones numéricas
        txtPrecioCosto.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*\\.?\\d*")) txtPrecioCosto.setText(o);
        });
        txtPrecioMinorista.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*\\.?\\d*")) txtPrecioMinorista.setText(o);
        });
        txtPrecioMayorista.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*\\.?\\d*")) txtPrecioMayorista.setText(o);
        });
        txtStock.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) txtStock.setText(o);
        });
        txtStockMinimo.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) txtStockMinimo.setText(o);
        });
    }

    private void cargarDatosIniciales() {
        ejecutarEnBackground(() -> {
            try {
                // Cargar productos
                var listaProductos = productoService.listarTodos();
                Platform.runLater(() -> {
                    productos.setAll(listaProductos);
                    cbProductos.setItems(productos);
                    cbProductoBase.setItems(productos);
                });

                // Cargar categorías
                var listaCategorias = categoriaService.listarTodas();
                Platform.runLater(() -> {
                    categorias.setAll(listaCategorias);
                    cbCategorias.setItems(categorias);
                });

                cargarVariantes();

            } catch (DataAccessException e) {
                logger.error("Error cargando datos iniciales", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudieron cargar los datos")
                );
            }
        });
    }

    private void cargarVariantes() {
        ejecutarEnBackground(() -> {
            try {
                List<Variante> lista = new ArrayList<>();
                // Cargar todas las variantes de todos los productos
                for (Producto p : productos) {
                    List<Variante> vars = varianteService.listarPorProducto(p.getId());
                    for (Variante v : vars) {
                        v.setProducto(p);
                    }
                    lista.addAll(vars);
                }

                Platform.runLater(() -> {
                    variantes.setAll(lista);
                    lblTotalVariantes.setText("Total variantes: " + lista.size());
                });

            } catch (DataAccessException e) {
                logger.error("Error cargando variantes", e);
            }
        });
    }

    @FXML
    private void buscarVariantes() {
        String termino = txtBuscar.getText().trim();
        if (termino.isEmpty()) {
            cargarVariantes();
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                var lista = varianteService.buscarPorTexto(termino);
                Platform.runLater(() -> {
                    variantes.setAll(lista);
                    lblTotalVariantes.setText("Resultados: " + lista.size());
                });
            } catch (DataAccessException e) {
                logger.error("Error buscando variantes", e);
            }
        });
    }

    @FXML
    private void filtrarPorProducto() {
        Producto producto = cbProductos.getValue();
        if (producto == null) {
            cargarVariantes();
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                var lista = varianteService.listarPorProducto(producto.getId());
                for (Variante v : lista) v.setProducto(producto);
                
                Platform.runLater(() -> {
                    variantes.setAll(lista);
                    lblTotalVariantes.setText("Variantes de " + producto.getNombre() + ": " + lista.size());
                });
            } catch (DataAccessException e) {
                logger.error("Error filtrando por producto", e);
            }
        });
    }

    @FXML
    private void filtrarPorCategoria() {
        Categoria categoria = cbCategorias.getValue();
        if (categoria == null) {
            cargarVariantes();
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                var prods = productoService.listarPorCategoria(categoria.getId());
                List<Variante> lista = new ArrayList<>();
                for (Producto p : prods) {
                    var vars = varianteService.listarPorProducto(p.getId());
                    for (Variante v : vars) v.setProducto(p);
                    lista.addAll(vars);
                }

                Platform.runLater(() -> {
                    variantes.setAll(lista);
                    lblTotalVariantes.setText("Variantes de " + categoria.getNombre() + ": " + lista.size());
                });
            } catch (DataAccessException e) {
                logger.error("Error filtrando por categoría", e);
            }
        });
    }

    @FXML
    private void limpiarFiltros() {
        cbProductos.setValue(null);
        cbCategorias.setValue(null);
        txtBuscar.clear();
        cargarVariantes();
    }

    @FXML
    private void mostrarStockBajo() {
        ejecutarEnBackground(() -> {
            try {
                var lista = varianteService.listarStockBajo();
                Platform.runLater(() -> {
                    variantes.setAll(lista);
                    lblTotalVariantes.setText("Stock bajo: " + lista.size() + " variantes");
                    if (lista.isEmpty()) {
                        AlertUtils.mostrarInfo("Stock Bajo", "¡Excelente! No hay productos con stock bajo.");
                    }
                });
            } catch (DataAccessException e) {
                logger.error("Error obteniendo stock bajo", e);
            }
        });
    }

    @FXML
    private void mostrarFormulario() {
        panelFormulario.setExpanded(true);
        limpiarFormulario();
        modoEdicion = false;
    }

    @FXML
    private void verAtributosRequeridos() {
        Producto producto = cbProductoBase.getValue();
        if (producto == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un producto base");
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                var atributos = categoriaService.obtenerAtributosDeCategoria(producto.getCategoriaId());
                
                StringBuilder info = new StringBuilder("Atributos requeridos:\n\n");
                for (Atributo a : atributos) {
                    info.append("• ").append(a.getNombre())
                        .append(" (").append(a.getTipo().getValor()).append(")\n");
                }

                Platform.runLater(() -> 
                    AlertUtils.mostrarInfo("Atributos", info.toString())
                );
            } catch (DataAccessException e) {
                logger.error("Error obteniendo atributos", e);
            }
        });
    }

    private void cargarAtributosDinamicos(Producto producto) {
        vboxAtributos.getChildren().clear();
        atributosComboBoxes.clear();

        ejecutarEnBackground(() -> {
            try {
                var atributos = categoriaService.obtenerAtributosDeCategoria(producto.getCategoriaId());

                Platform.runLater(() -> {
                    for (Atributo atributo : atributos) {
                        HBox hbox = new HBox(10);
                        hbox.setPadding(new Insets(5));
                        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        Label label = new Label(atributo.getNombre() + ":");
                        label.setPrefWidth(100);

                        ComboBox<String> combo = new ComboBox<>();
                        combo.setPrefWidth(200);
                        combo.setPromptText("Seleccione " + atributo.getNombre());

                        // Cargar valores posibles
                        cargarValoresAtributo(atributo.getId(), combo);

                        atributosComboBoxes.put(atributo.getId(), combo);

                        hbox.getChildren().addAll(label, combo);
                        vboxAtributos.getChildren().add(hbox);
                    }
                });

            } catch (DataAccessException e) {
                logger.error("Error cargando atributos dinámicos", e);
            }
        });
    }

    private void cargarValoresAtributo(Integer atributoId, ComboBox<String> combo) {
        ejecutarEnBackground(() -> {
            try {
                var valores = atributoService.obtenerValoresPosibles(atributoId);
                ObservableList<String> items = FXCollections.observableArrayList();
                for (ValorAtributo v : valores) {
                    items.add(v.getValor());
                }
                Platform.runLater(() -> combo.setItems(items));
            } catch (DataAccessException e) {
                logger.error("Error cargando valores de atributo", e);
            }
        });
    }

    private void heredarPreciosProducto(Producto producto) {
        txtPrecioCosto.setText(String.valueOf(producto.getPrecioCosto()));
        txtPrecioMinorista.setText(String.valueOf(producto.getPrecioMinorista()));
        txtPrecioMayorista.setText(String.valueOf(producto.getPrecioMayorista()));
    }

    @FXML
    private void generarSkuAutomatico() {
        Producto producto = cbProductoBase.getValue();
        if (producto == null) {
            AlertUtils.mostrarAdvertencia("Producto requerido", "Seleccione un producto base");
            return;
        }

        String sku = String.format("%s-%03d", 
            producto.getCodigo().toUpperCase(), 
            new Random().nextInt(999) + 1);
        txtSku.setText(sku);
    }

    @FXML
    private void guardarVariante() {
        try {
            Producto productoBase = cbProductoBase.getValue();
            if (productoBase == null) {
                AlertUtils.mostrarAdvertencia("Validación", "Seleccione un producto base");
                return;
            }

            Variante variante = modoEdicion ? varianteSeleccionada : new Variante();
            
            variante.setProductoId(productoBase.getId());
            variante.setSku(txtSku.getText().trim());
            variante.setCodigoBarras(txtCodigoBarras.getText().trim());
            variante.setPrecioCosto(Double.parseDouble(txtPrecioCosto.getText().trim()));
            variante.setPrecioMinorista(Double.parseDouble(txtPrecioMinorista.getText().trim()));
            variante.setPrecioMayorista(Double.parseDouble(txtPrecioMayorista.getText().trim()));
            variante.setStock(Integer.parseInt(txtStock.getText().trim()));
            variante.setStockMinimo(Integer.parseInt(txtStockMinimo.getText().trim()));

            // Recopilar atributos
            List<VarianteAtributo> atributos = new ArrayList<>();
            for (Map.Entry<Integer, ComboBox<String>> entry : atributosComboBoxes.entrySet()) {
                String valor = entry.getValue().getValue();
                if (valor != null && !valor.isEmpty()) {
                    VarianteAtributo va = new VarianteAtributo();
                    va.setAtributoId(entry.getKey());
                    va.setValor(valor);
                    atributos.add(va);
                }
            }
            variante.setAtributos(atributos);

            ejecutarEnBackground(() -> {
                try {
                    if (modoEdicion) {
                        // Actualizar no implementado en este ejemplo
                        Platform.runLater(() -> 
                            AlertUtils.mostrarAdvertencia("No implementado", 
                                "La actualización de variantes está en desarrollo")
                        );
                    } else {
                        varianteService.crear(variante);
                        Platform.runLater(() -> {
                            AlertUtils.mostrarExito("Éxito", "Variante creada correctamente");
                            cargarVariantes();
                            limpiarFormulario();
                            panelFormulario.setExpanded(false);
                        });
                    }
                } catch (ValidationException e) {
                    Platform.runLater(() -> 
                        AlertUtils.mostrarAdvertencia("Validación", e.getMessage())
                    );
                } catch (BusinessException | DataAccessException e) {
                    logger.error("Error guardando variante", e);
                    Platform.runLater(() -> 
                        AlertUtils.mostrarError("Error", "No se pudo guardar la variante")
                    );
                }
            });

        } catch (NumberFormatException e) {
            AlertUtils.mostrarAdvertencia("Datos inválidos", "Ingrese valores numéricos válidos");
        }
    }

    @FXML
    private void editarVariante() {
        if (varianteSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una variante");
            return;
        }

        AlertUtils.mostrarAdvertencia("En desarrollo", 
            "La edición de variantes estará disponible próximamente");
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
                            cargarVariantes();
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
    private void exportarExcel() {
        AlertUtils.mostrarInfo("Exportar", "Funcionalidad de exportación en desarrollo");
    }

    @FXML
    private void cancelarFormulario() {
        limpiarFormulario();
        panelFormulario.setExpanded(false);
    }

    private void limpiarFormulario() {
        cbProductoBase.setValue(null);
        txtSku.clear();
        txtCodigoBarras.clear();
        txtPrecioCosto.clear();
        txtPrecioMinorista.clear();
        txtPrecioMayorista.clear();
        txtStock.setText("0");
        txtStockMinimo.setText("5");
        vboxAtributos.getChildren().clear();
        atributosComboBoxes.clear();
        varianteSeleccionada = null;
        modoEdicion = false;
    }

    private void actualizarEstadoBotones() {
        boolean haySeleccion = varianteSeleccionada != null;
        btnEditar.setDisable(!haySeleccion);
        btnEliminar.setDisable(!haySeleccion);
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