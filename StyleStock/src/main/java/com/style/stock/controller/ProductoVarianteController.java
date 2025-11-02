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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador COMPLETO para Productos y Variantes
 * Con formulario de variantes funcional y filtros avanzados
 */
public class ProductoVarianteController {
    private static final Logger logger = LoggerFactory.getLogger(ProductoVarianteController.class);

    // Servicios
    private final ProductoService productoService;
    private final VarianteService varianteService;
    private final CategoriaService categoriaService;
    private final AtributoService atributoService;
    private final ValorAtributoService valorAtributoService;

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

    // ========== TABLA VARIANTES CON COLUMNAS DINÁMICAS ==========
    @FXML private TableView<Variante> tablaVariantes;
    @FXML private TableColumn<Variante, Integer> colVarId;
    @FXML private TableColumn<Variante, String> colVarSku;
    // Columnas de atributos dinámicos (se crean en tiempo de ejecución)
    private List<TableColumn<Variante, String>> columnasAtributosDinamicas = new ArrayList<>();
    @FXML private TableColumn<Variante, Double> colVarPrecioCosto;
    @FXML private TableColumn<Variante, Double> colVarPrecioMin;
    @FXML private TableColumn<Variante, Double> colVarPrecioMay;
    @FXML private TableColumn<Variante, Integer> colVarStock;
    @FXML private TableColumn<Variante, Integer> colVarStockMin;

    // ========== COMPONENTES UI ==========
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Categoria> cbFiltroCategoria; // NUEVO
    @FXML private ComboBox<String> cbFiltroAtributo; // NUEVO
    @FXML private TextField txtFiltroValorAtributo; // NUEVO
    @FXML private ComboBox<String> cbFiltroValorAtributo;
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
    private final ObservableList<Producto> productosFiltrados; // NUEVO
    private final ObservableList<Variante> variantes;
    private final ObservableList<Variante> variantesFiltradas; // NUEVO
    private final ObservableList<Categoria> categorias;
    private Producto productoSeleccionado;
    private Variante varianteSeleccionada;
    private List<Atributo> atributosCategoria = new ArrayList<>();

    public ProductoVarianteController() {
        this.productoService = new ProductoService();
        this.varianteService = new VarianteService();
        this.categoriaService = new CategoriaService();
        this.atributoService = new AtributoService();
        this.valorAtributoService = new ValorAtributoService();
        this.productos = FXCollections.observableArrayList();
        this.productosFiltrados = FXCollections.observableArrayList();
        this.variantes = FXCollections.observableArrayList();
        this.variantesFiltradas = FXCollections.observableArrayList();
        this.categorias = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTablaProductos();
        configurarTablaVariantes();
        configurarFiltros(); // NUEVO
        configurarEventos();
        cargarCategorias();
        cargarProductos();
    }

    // ============================================
    // CONFIGURACIÓN FILTROS (NUEVO)
    // ============================================

    private void configurarFiltros() {
        if (cbFiltroCategoria != null) {
            cbFiltroCategoria.setItems(categorias);
            cbFiltroCategoria.setConverter(new StringConverter<Categoria>() {
                @Override
                public String toString(Categoria categoria) {
                    return categoria == null ? "Todas las categorías" : categoria.getNombre();
                }

                @Override
                public Categoria fromString(String string) {
                    return categorias.stream()
                            .filter(c -> c.getNombre().equals(string))
                            .findFirst()
                            .orElse(null);
                }
            });

            cbFiltroCategoria.valueProperty().addListener((obs, old, nuevo) -> {
                aplicarFiltrosProductos();
                if (nuevo != null) {
                    cargarAtributosParaFiltro(nuevo);
                } else {
                    if (productoSeleccionado == null) {
                        limpiarFiltrosVariantes();
                        cbFiltroAtributo.getItems().clear();
                    } else {
                        // Si hay producto seleccionado, mantener sus atributos
                        cargarAtributosDeProductoSeleccionado(productoSeleccionado);
                    }
                   //if (cbFiltroAtributo != null) {
                   //     cbFiltroAtributo.getItems().clear();
                    // }
                }
            });
        }

        if (cbFiltroAtributo != null) {
            cbFiltroAtributo.valueProperty().addListener((obs, old, nuevo) -> {
                aplicarFiltrosVariantes();
            });
        }

        if (txtFiltroValorAtributo != null) {
            txtFiltroValorAtributo.textProperty().addListener((obs, old, nuevo) -> {
                aplicarFiltrosVariantes();
            });
        }
    }

    private void cargarAtributosParaFiltro(Categoria categoria) {
        ejecutarEnBackground(() -> {
            try {
                List<Atributo> atributos = categoriaService.obtenerAtributosDeCategoria(categoria.getId());

                Platform.runLater(() -> {
                    if (cbFiltroAtributo != null) {
                        List<String> nombresAtributos = atributos.stream()
                                .map(Atributo::getNombre)
                                .collect(Collectors.toList());
                        cbFiltroAtributo.setItems(FXCollections.observableArrayList(nombresAtributos));
                    }
                    // Limpiar valores al cambiar atributo
                    if (cbFiltroValorAtributo != null) {
                        cbFiltroValorAtributo.getItems().clear();
                    }
                });
            } catch (DataAccessException e) {
                logger.error("Error cargando atributos para filtro", e);
            }
        });
    }

    @FXML
    private void cargarAtributosDeProductoSeleccionado(Producto producto) {
        if (producto == null || producto.getCategoriaId() == null) {
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                // Obtener la categoría del producto
                Categoria categoriaDelProducto = producto.getCategoria();

                if (categoriaDelProducto == null) {
                    // Si no está cargada, buscarla
                    categoriaDelProducto = categoriaService.buscarPorId(producto.getCategoriaId());
                }

                // Cargar atributos de esa categoría
                List<Atributo> atributos = categoriaService.obtenerAtributosDeCategoria(categoriaDelProducto.getId());

                // Guardar para uso posterior
                final Categoria categoriaFinal = categoriaDelProducto;
                atributosCategoria = atributos;

                Platform.runLater(() -> {
                    if (cbFiltroAtributo != null) {
                        List<String> nombresAtributos = atributos.stream()
                                .map(Atributo::getNombre)
                                .collect(Collectors.toList());
                        nombresAtributos.add(0, "Todos");
                        cbFiltroAtributo.setItems(FXCollections.observableArrayList(nombresAtributos));
                        cbFiltroAtributo.setValue("Todos");

                        // ⭐ OPCIONAL: Mostrar de qué categoría son los atributos
                        logger.debug("Atributos cargados de categoría: {}", categoriaFinal.getNombre());
                    }

                    // Limpiar el ComboBox de valores al cambiar de producto
                    if (cbFiltroValorAtributo != null) {
                        cbFiltroValorAtributo.getItems().clear();
                    }
                });

            } catch (DataAccessException | NotFoundException e) {
                logger.error("Error cargando atributos del producto seleccionado", e);
            }
        });
    }

    @FXML
    private void cargarValoresAtributoFiltro() {
        String atributoNombre = cbFiltroAtributo.getValue();
        if (atributoNombre == null || atributoNombre.isEmpty()) {
            cbFiltroValorAtributo.getItems().clear();
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                // Buscar el atributo por nombre
                Atributo atributo = atributosCategoria.stream()
                        .filter(a -> a.getNombre().equals(atributoNombre))
                        .findFirst()
                        .orElse(null);

                if (atributo != null) {
                    List<ValorAtributo> valores = valorAtributoService.listarPorAtributo(atributo.getId());

                    Platform.runLater(() -> {
                        List<String> nombresValores = valores.stream()
                                .map(ValorAtributo::getValor)
                                .collect(Collectors.toList());
                        cbFiltroValorAtributo.setItems(FXCollections.observableArrayList(nombresValores));
                    });
                }
            } catch (DataAccessException e) {
                logger.error("Error cargando valores de atributo", e);
            }
        });
    }

    @FXML
    private void aplicarFiltrosProductos() {
        Categoria categoriaFiltro = cbFiltroCategoria != null ? cbFiltroCategoria.getValue() : null;
        String busqueda = txtBuscar.getText().trim().toLowerCase();

        List<Producto> filtrados = productos.stream()
                .filter(p -> {
                    // Filtro por categoría
                    if (categoriaFiltro != null && !p.getCategoriaId().equals(categoriaFiltro.getId())) {
                        return false;
                    }

                    // Filtro por búsqueda
                    if (!busqueda.isEmpty()) {
                        return p.getCodigo().toLowerCase().contains(busqueda) ||
                                p.getNombre().toLowerCase().contains(busqueda) ||
                                (p.getMarca() != null && p.getMarca().toLowerCase().contains(busqueda));
                    }

                    return true;
                })
                .collect(Collectors.toList());

        productosFiltrados.setAll(filtrados);
    }

    @FXML
    private void aplicarFiltrosVariantes() {
        String atributoFiltro = cbFiltroAtributo != null ? cbFiltroAtributo.getValue() : null;
        String valorFiltro = cbFiltroValorAtributo != null ? cbFiltroValorAtributo.getValue() : null;

        List<Variante> filtradas = variantes.stream()
                .filter(v -> {
                    // Si no hay filtro, mostrar todas
                    if (atributoFiltro == null || atributoFiltro.isEmpty()) {
                        return true;
                    }

                    // Buscar el atributo en la variante
                    Optional<VarianteAtributo> vaEncontrado = v.getAtributos().stream()
                            .filter(va -> {
                                Atributo attr = atributosCategoria.stream()
                                        .filter(a -> a.getId().equals(va.getAtributoId()))
                                        .findFirst()
                                        .orElse(null);
                                return attr != null && attr.getNombre().equals(atributoFiltro);
                            })
                            .findFirst();

                    if (vaEncontrado.isEmpty()) {
                        return false;
                    }

                    // Si hay valor específico, filtrar por él
                    if (valorFiltro != null && !valorFiltro.isEmpty()) {
                        return vaEncontrado.get().getValor().equals(valorFiltro);
                    }

                    return true;
                })
                .collect(Collectors.toList());

        variantesFiltradas.setAll(filtradas);
        lblTotalVariantes.setText("Variantes mostradas: " + filtradas.size() + " de " + variantes.size());
    }

    @FXML
    private void limpiarFiltrosProductos() {
        if (cbFiltroCategoria != null) cbFiltroCategoria.setValue(null);
        txtBuscar.clear();

        productosFiltrados.setAll(productos);
    }

    @FXML
    private void limpiarFiltrosVariantes() {
        if (cbFiltroAtributo != null) cbFiltroAtributo.setValue(null);
        if (txtFiltroValorAtributo != null) txtFiltroValorAtributo.clear();

        variantesFiltradas.setAll(variantes);
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
            if (p.getCategoriaId() != null) {
                Categoria cat;
                try {
                    cat = categoriaService.buscarPorId(p.getCategoriaId());
                } catch (NotFoundException | DataAccessException e) {
                    throw new RuntimeException(e);
                }
                return new SimpleStringProperty(cat.getNombre());
            }
            return new SimpleStringProperty("-");
        });

        colProdMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colProdPrecioMin.setCellValueFactory(new PropertyValueFactory<>("precioMinorista"));
        colProdPrecioMay.setCellValueFactory(new PropertyValueFactory<>("precioMayorista"));

        colProdVariantes.setCellValueFactory(cellData -> {
            try {
                int count = varianteService.listarPorProducto(cellData.getValue().getId()).size();
                return new javafx.beans.property.SimpleObjectProperty<>(count);
            } catch (DataAccessException e) {
                return new javafx.beans.property.SimpleObjectProperty<>(0);
            }
        });

        colProdPrecioMin.setCellFactory(col -> createMoneyCell());
        colProdPrecioMay.setCellFactory(col -> createMoneyCell());

        tablaProductos.setItems(productosFiltrados);

        tablaProductos.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && productoSeleccionado != null) {
                editarProducto();
            }
        });
    }

    // MEJORADO: Columnas dinámicas para atributos
    private void configurarTablaVariantes() {
        colVarId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colVarSku.setCellValueFactory(new PropertyValueFactory<>("sku"));

        // Las columnas de atributos se crean dinámicamente en cargarVariantesDelProducto()

        colVarPrecioCosto.setCellValueFactory(new PropertyValueFactory<>("precioCosto"));
        colVarPrecioMin.setCellValueFactory(new PropertyValueFactory<>("precioMinorista"));
        colVarPrecioMay.setCellValueFactory(new PropertyValueFactory<>("precioMayorista"));
        colVarStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colVarStockMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        colVarPrecioCosto.setCellFactory(col -> createMoneyCell());
        colVarPrecioMin.setCellFactory(col -> createMoneyCell());
        colVarPrecioMay.setCellFactory(col -> createMoneyCell());

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

        tablaVariantes.setItems(variantesFiltradas);

        tablaVariantes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && varianteSeleccionada != null) {
                editarVariante();
            }
        });
    }

    // NUEVO: Crear columnas dinámicas según atributos de la categoría
    private void crearColumnasAtributosDinamicas(List<Atributo> atributos) {
        // Eliminar columnas dinámicas anteriores
        tablaVariantes.getColumns().removeAll(columnasAtributosDinamicas);
        columnasAtributosDinamicas.clear();

        // Encontrar posición donde insertar (después de SKU)
        int posicionInsercion = tablaVariantes.getColumns().indexOf(colVarSku) + 1;

        // Crear una columna por cada atributo
        for (Atributo atributo : atributos) {
            TableColumn<Variante, String> columna = new TableColumn<>(atributo.getNombre());
            columna.setPrefWidth(100);

            columna.setCellValueFactory(cellData -> {
                Variante v = cellData.getValue();
                if (v.getAtributos() != null) {
                    Optional<VarianteAtributo> va = v.getAtributos().stream()
                            .filter(attr -> attr.getAtributoId().equals(atributo.getId()))
                            .findFirst();

                    if (va.isPresent()) {
                        return new SimpleStringProperty(va.get().getValor());
                    }
                }
                return new SimpleStringProperty("-");
            });

            // Estilo según tipo de atributo
            if (atributo.getTipo() == Atributo.TipoAtributo.COLOR) {
                columna.setCellFactory(col -> new TableCell<Variante, String>() {
                    @Override
                    protected void updateItem(String valor, boolean empty) {
                        super.updateItem(valor, empty);
                        if (empty || valor == null || valor.equals("-")) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(valor);
                            // Buscar código hex si existe
                            TableRow<Variante> row = getTableRow();
                            if (row != null && row.getItem() != null) {
                                Variante v = row.getItem();
                                Optional<VarianteAtributo> va = v.getAtributos().stream()
                                        .filter(a -> a.getAtributoId().equals(atributo.getId()))
                                        .findFirst();

                                if (va.isPresent()) {
                                    // Aquí podrías buscar el código hex del ValorAtributo
                                    setStyle("-fx-font-weight: bold;");
                                }
                            }
                        }
                    }
                });
            }

            columnasAtributosDinamicas.add(columna);
            tablaVariantes.getColumns().add(posicionInsercion++, columna);
        }
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
        tablaProductos.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    productoSeleccionado = newVal;
                    actualizarEstadoBotonesProducto();
                    if (newVal != null) {
                        cargarVariantesDelProducto(newVal);
                        cargarAtributosDeProductoSeleccionado(newVal);
                    } else {
                        variantes.clear();
                        variantesFiltradas.clear();
                        lblProductoSeleccionado.setText("Ninguno");
                        lblInfoProducto.setText("Seleccione un producto para ver sus variantes");

                        if (cbFiltroCategoria.getValue() == null) {
                            limpiarFiltrosVariantes();
                        }
                    }
                }
        );

        tablaVariantes.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    varianteSeleccionada = newVal;
                    actualizarEstadoBotonesVariante();
                }
        );

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltrosProductos());
    }

    // ============================================
    // CARGA DE DATOS
    // ============================================

    private void cargarCategorias() {
        ejecutarEnBackground(() -> {
            try {
                var lista = categoriaService.listarTodas();
                Platform.runLater(() -> categorias.setAll(lista));
            } catch (DataAccessException e) {
                logger.error("Error cargando categorías", e);
            }
        });
    }

    private void cargarProductos() {
        ejecutarEnBackground(() -> {
            try {
                var lista = productoService.listarTodos();

                Map<Integer, Categoria> categoriasMap = new HashMap<>();
                for (Categoria cat : categorias) {
                    categoriasMap.put(cat.getId(), cat);
                }

                for (Producto p : lista) {
                    p.setCategoria(categoriasMap.get(p.getCategoriaId()));
                }

                Platform.runLater(() -> {
                    productos.setAll(lista);
                    productosFiltrados.setAll(lista);
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

    // MEJORADO: Cargar atributos y crear columnas dinámicas
    private void cargarVariantesDelProducto(Producto producto) {
        lblProductoSeleccionado.setText(producto.getNombre());
        lblInfoProducto.setText("Cargando variantes...");

        ejecutarEnBackground(() -> {
            try {
                // Cargar atributos de la categoría
                atributosCategoria = categoriaService.obtenerAtributosDeCategoria(producto.getCategoriaId());

                // Cargar variantes
                var lista = varianteService.listarPorProducto(producto.getId());

                for (Variante v : lista) {
                    v.setProducto(producto);
                }

                Platform.runLater(() -> {
                    // Crear columnas dinámicas
                    crearColumnasAtributosDinamicas(atributosCategoria);

                    variantes.setAll(lista);
                    variantesFiltradas.setAll(lista);
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
                    variantesFiltradas.clear();
                    lblInfoProducto.setText("Error cargando variantes");
                });
            }
        });
    }

    // ============================================
    // ACCIONES PRODUCTOS
    // ============================================

    @FXML
    private void nuevoProducto() {
        mostrarDialogoProducto(null);
    }

    @FXML
    private void editarProducto() {
        if (productoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un producto");
            return;
        }
        mostrarDialogoProducto(productoSeleccionado);
    }

    private void mostrarDialogoProducto(Producto producto) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(producto == null ? "Nuevo Producto" : "Editar Producto");
        dialog.setHeaderText(producto == null ? "Ingrese los datos del nuevo producto" :
                "Modifique los datos del producto");

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtCodigo = new TextField();
        TextField txtNombre = new TextField();
        TextField txtMarca = new TextField();
        ComboBox<Categoria> cbCategoria = new ComboBox<>(categorias);
        cbCategoria.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria categoria) {
                return categoria == null ? "" : categoria.getNombre();
            }

            @Override
            public Categoria fromString(String string) {
                return null;
            }
        });

        TextField txtPrecioCosto = new TextField("0");
        TextField txtPrecioMin = new TextField();
        TextField txtPrecioMay = new TextField();

        if (producto != null) {
            txtCodigo.setText(producto.getCodigo());
            txtCodigo.setDisable(true);
            txtNombre.setText(producto.getNombre());
            txtMarca.setText(producto.getMarca());
            cbCategoria.setValue(producto.getCategoria());
            txtPrecioCosto.setText(String.valueOf(producto.getPrecioCosto()));
            txtPrecioMin.setText(String.valueOf(producto.getPrecioMinorista()));
            txtPrecioMay.setText(String.valueOf(producto.getPrecioMayorista()));
        }

        grid.add(new Label("Código:"), 0, 0);
        grid.add(txtCodigo, 1, 0);
        grid.add(new Label("Nombre:"), 0, 1);
        grid.add(txtNombre, 1, 1);
        grid.add(new Label("Marca:"), 0, 2);
        grid.add(txtMarca, 1, 2);
        grid.add(new Label("Categoría:"), 0, 3);
        grid.add(cbCategoria, 1, 3);
        grid.add(new Label("Precio Costo:"), 0, 4);
        grid.add(txtPrecioCosto, 1, 4);
        grid.add(new Label("Precio Minorista:"), 0, 5);
        grid.add(txtPrecioMin, 1, 5);
        grid.add(new Label("Precio Mayorista:"), 0, 6);
        grid.add(txtPrecioMay, 1, 6);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == btnGuardar) {
            try {
                Producto p = producto != null ? producto : new Producto();

                if (producto == null) {
                    p.setCodigo(txtCodigo.getText().trim());
                }
                p.setNombre(txtNombre.getText().trim());
                p.setMarca(txtMarca.getText().trim());
                p.setCategoriaId(cbCategoria.getValue().getId());
                p.setCategoria(cbCategoria.getValue());
                p.setPrecioCosto(Double.parseDouble(txtPrecioCosto.getText().trim()));
                p.setPrecioMinorista(Double.parseDouble(txtPrecioMin.getText().trim()));
                p.setPrecioMayorista(Double.parseDouble(txtPrecioMay.getText().trim()));

                ejecutarEnBackground(() -> {
                    try {
                        if (producto == null) {
                            productoService.crear(p);
                        } else {
                            productoService.actualizar(p);
                        }

                        Platform.runLater(() -> {
                            AlertUtils.mostrarExito("Éxito",
                                    producto == null ? "Producto creado" : "Producto actualizado");
                            cargarProductos();
                        });
                    } catch (Exception e) {
                        logger.error("Error guardando producto", e);
                        Platform.runLater(() ->
                                AlertUtils.mostrarError("Error", e.getMessage())
                        );
                    }
                });

            } catch (NumberFormatException e) {
                AlertUtils.mostrarAdvertencia("Error", "Los precios deben ser números válidos");
            }
        }
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
                        variantesFiltradas.clear();
                    });
                } catch (Exception e) {
                    logger.error("Error eliminando producto", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", e.getMessage())
                    );
                }
            });
        }
    }

    // ============================================
    // ACCIONES VARIANTES - FORMULARIO COMPLETO
    // ============================================

    @FXML
    private void nuevaVariante() {
        if (productoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un producto primero");
            return;
        }
        mostrarDialogoVariante(null);
    }

    @FXML
    private void editarVariante() {
        if (varianteSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una variante");
            return;
        }
        mostrarDialogoVariante(varianteSeleccionada);
    }

    // NUEVO: Formulario completo de variantes con atributos dinámicos
    private void mostrarDialogoVariante(Variante variante) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(variante == null ? "Nueva Variante" : "Editar Variante");
        dialog.setHeaderText("Producto: " + productoSeleccionado.getNombre());

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        VBox contenido = new VBox(15);
        contenido.setPadding(new Insets(20));

        // ===== DATOS BÁSICOS =====
        GridPane gridBasico = new GridPane();
        gridBasico.setHgap(10);
        gridBasico.setVgap(10);

        TextField txtSku = new TextField();
        TextField txtCodigoBarras = new TextField();
        TextField txtPrecioCosto = new TextField("0");
        TextField txtPrecioMin = new TextField();
        TextField txtPrecioMay = new TextField();
        TextField txtStock = new TextField("0");
        TextField txtStockMin = new TextField("5");

        // Heredar precios del producto
        if (variante == null) {
            txtPrecioCosto.setText(String.valueOf(productoSeleccionado.getPrecioCosto()));
            txtPrecioMin.setText(String.valueOf(productoSeleccionado.getPrecioMinorista()));
            txtPrecioMay.setText(String.valueOf(productoSeleccionado.getPrecioMayorista()));
        } else {
            txtSku.setText(variante.getSku());
            txtSku.setDisable(true); // No permitir cambiar SKU
            txtCodigoBarras.setText(variante.getCodigoBarras());
            txtPrecioCosto.setText(String.valueOf(variante.getPrecioCosto()));
            txtPrecioMin.setText(String.valueOf(variante.getPrecioMinorista()));
            txtPrecioMay.setText(String.valueOf(variante.getPrecioMayorista()));
            txtStock.setText(String.valueOf(variante.getStock()));
            txtStockMin.setText(String.valueOf(variante.getStockMinimo()));
        }

        gridBasico.add(new Label("SKU:"), 0, 0);
        gridBasico.add(txtSku, 1, 0);
        Button btnGenerarSku = new Button("Generar");
        btnGenerarSku.setOnAction(e -> {
            String sku = String.format("%s-%03d",
                    productoSeleccionado.getCodigo().toUpperCase(),
                    new Random().nextInt(999) + 1);
            txtSku.setText(sku);
        });
        gridBasico.add(btnGenerarSku, 2, 0);

        gridBasico.add(new Label("Código Barras:"), 0, 1);
        gridBasico.add(txtCodigoBarras, 1, 1);

        gridBasico.add(new Label("Precio Costo:"), 0, 2);
        gridBasico.add(txtPrecioCosto, 1, 2);

        gridBasico.add(new Label("Precio Minorista:"), 0, 3);
        gridBasico.add(txtPrecioMin, 1, 3);

        gridBasico.add(new Label("Precio Mayorista:"), 0, 4);
        gridBasico.add(txtPrecioMay, 1, 4);

        gridBasico.add(new Label("Stock Inicial:"), 0, 5);
        gridBasico.add(txtStock, 1, 5);

        gridBasico.add(new Label("Stock Mínimo:"), 0, 6);
        gridBasico.add(txtStockMin, 1, 6);

        contenido.getChildren().add(new TitledPane("Datos Básicos", gridBasico));

        // ===== ATRIBUTOS DINÁMICOS =====
        if (!atributosCategoria.isEmpty()) {
            VBox vboxAtributos = new VBox(10);
            vboxAtributos.setPadding(new Insets(10));

            Map<Integer, ComboBox<ValorAtributo>> combosAtributos = new HashMap<>();

            for (Atributo atributo : atributosCategoria) {
                HBox hboxAttr = new HBox(10);
                hboxAttr.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label lblAttr = new Label(atributo.getNombre() + ":");
                lblAttr.setPrefWidth(120);
                lblAttr.setStyle("-fx-font-weight: bold;");

                if (atributo.getTipo() == Atributo.TipoAtributo.LISTA ||
                        atributo.getTipo() == Atributo.TipoAtributo.COLOR) {

                    ComboBox<ValorAtributo> combo = new ComboBox<>();
                    combo.setPrefWidth(200);
                    combo.setPromptText("Seleccione " + atributo.getNombre());

                    combo.setConverter(new StringConverter<ValorAtributo>() {
                        @Override
                        public String toString(ValorAtributo valor) {
                            return valor == null ? "" : valor.getValor();
                        }

                        @Override
                        public ValorAtributo fromString(String string) {
                            return null;
                        }
                    });

                    // Cargar valores
                    ejecutarEnBackground(() -> {
                        try {
                            List<ValorAtributo> valores = valorAtributoService.listarPorAtributo(atributo.getId());
                            Platform.runLater(() -> {
                                combo.setItems(FXCollections.observableArrayList(valores));

                                // Si estamos editando, seleccionar el valor actual
                                if (variante != null && variante.getAtributos() != null) {
                                    Optional<VarianteAtributo> vaExistente = variante.getAtributos().stream()
                                            .filter(va -> va.getAtributoId().equals(atributo.getId()))
                                            .findFirst();

                                    if (vaExistente.isPresent()) {
                                        String valorActual = vaExistente.get().getValor();
                                        combo.getItems().stream()
                                                .filter(v -> v.getValor().equals(valorActual))
                                                .findFirst()
                                                .ifPresent(combo::setValue);
                                    }
                                }
                            });
                        } catch (DataAccessException e) {
                            logger.error("Error cargando valores de atributo", e);
                        }
                    });

                    combosAtributos.put(atributo.getId(), combo);
                    hboxAttr.getChildren().addAll(lblAttr, combo);

                } else if (atributo.getTipo() == Atributo.TipoAtributo.TEXTO) {
                    TextField txtValor = new TextField();
                    txtValor.setPrefWidth(200);
                    txtValor.setPromptText("Ingrese " + atributo.getNombre());

                    if (variante != null && variante.getAtributos() != null) {
                        variante.getAtributos().stream()
                                .filter(va -> va.getAtributoId().equals(atributo.getId()))
                                .findFirst()
                                .ifPresent(va -> txtValor.setText(va.getValor()));
                    }

                    // Crear un ComboBox temporal para almacenar el valor
                    ComboBox<ValorAtributo> comboTemp = new ComboBox<>();
                    txtValor.textProperty().addListener((obs, old, nuevo) -> {
                        if (!nuevo.isEmpty()) {
                            ValorAtributo va = new ValorAtributo();
                            va.setAtributoId(atributo.getId());
                            va.setValor(nuevo);
                            comboTemp.getItems().clear();
                            comboTemp.getItems().add(va);
                            comboTemp.setValue(va);
                        }
                    });

                    combosAtributos.put(atributo.getId(), comboTemp);
                    hboxAttr.getChildren().addAll(lblAttr, txtValor);

                } else if (atributo.getTipo() == Atributo.TipoAtributo.NUMERO) {
                    TextField txtNumero = new TextField("0");
                    txtNumero.setPrefWidth(100);

                    txtNumero.textProperty().addListener((obs, old, nuevo) -> {
                        if (!nuevo.matches("\\d*\\.?\\d*")) {
                            txtNumero.setText(old);
                        }
                    });

                    if (variante != null && variante.getAtributos() != null) {
                        variante.getAtributos().stream()
                                .filter(va -> va.getAtributoId().equals(atributo.getId()))
                                .findFirst()
                                .ifPresent(va -> txtNumero.setText(va.getValor()));
                    }

                    ComboBox<ValorAtributo> comboTemp = new ComboBox<>();
                    txtNumero.textProperty().addListener((obs, old, nuevo) -> {
                        if (!nuevo.isEmpty()) {
                            ValorAtributo va = new ValorAtributo();
                            va.setAtributoId(atributo.getId());
                            va.setValor(nuevo);
                            comboTemp.getItems().clear();
                            comboTemp.getItems().add(va);
                            comboTemp.setValue(va);
                        }
                    });

                    combosAtributos.put(atributo.getId(), comboTemp);
                    hboxAttr.getChildren().addAll(lblAttr, txtNumero);
                }

                vboxAtributos.getChildren().add(hboxAttr);
            }

            TitledPane tpAtributos = new TitledPane("Atributos de la Variante", vboxAtributos);
            tpAtributos.setCollapsible(false);
            contenido.getChildren().add(tpAtributos);

            // ===== GUARDAR VARIANTE =====
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == btnGuardar) {
                    try {
                        Variante v = variante != null ? variante : new Variante();

                        if (variante == null) {
                            v.setSku(txtSku.getText().trim());
                            v.setProductoId(productoSeleccionado.getId());
                        }

                        v.setCodigoBarras(txtCodigoBarras.getText().trim());
                        v.setPrecioCosto(Double.parseDouble(txtPrecioCosto.getText().trim()));
                        v.setPrecioMinorista(Double.parseDouble(txtPrecioMin.getText().trim()));
                        v.setPrecioMayorista(Double.parseDouble(txtPrecioMay.getText().trim()));
                        v.setStock(Integer.parseInt(txtStock.getText().trim()));
                        v.setStockMinimo(Integer.parseInt(txtStockMin.getText().trim()));

                        // Recopilar atributos
                        List<VarianteAtributo> atributos = new ArrayList<>();
                        for (Map.Entry<Integer, ComboBox<ValorAtributo>> entry : combosAtributos.entrySet()) {
                            ValorAtributo valor = entry.getValue().getValue();
                            if (valor != null) {
                                VarianteAtributo va = new VarianteAtributo();
                                va.setAtributoId(entry.getKey());
                                va.setValor(valor.getValor());
                                atributos.add(va);
                            }
                        }
                        v.setAtributos(atributos);

                        ejecutarEnBackground(() -> {
                            try {
                                if (variante == null) {
                                    varianteService.crear(v);
                                    Platform.runLater(() -> {
                                        AlertUtils.mostrarExito("Éxito", "Variante creada correctamente");
                                        cargarVariantesDelProducto(productoSeleccionado);
                                    });
                                } else {
                                    varianteService.actualizar(v);
                                    Platform.runLater(() -> {
                                        AlertUtils.mostrarExito("Éxito", "Variante actualizada correctamente");
                                        cargarVariantesDelProducto(productoSeleccionado);
                                    });
                                }
                            } catch (Exception e) {
                                logger.error("Error guardando variante", e);
                                Platform.runLater(() ->
                                        AlertUtils.mostrarError("Error", e.getMessage())
                                );
                            }
                        });

                    } catch (NumberFormatException e) {
                        Platform.runLater(() ->
                                AlertUtils.mostrarAdvertencia("Datos inválidos",
                                        "Verifique que todos los campos numéricos sean válidos")
                        );
                    }
                }
                return dialogButton;
            });
        }

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(500);
        scroll.setPrefViewportWidth(500);

        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
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
                "SKU: " + varianteSeleccionada.getSku() + "\n\n⚠️ Esta acción desactivará la variante"
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    varianteService.eliminar(varianteSeleccionada.getId());
                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Variante eliminada correctamente");
                        if (productoSeleccionado != null) {
                            cargarVariantesDelProducto(productoSeleccionado);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error eliminando variante", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", "No se pudo eliminar la variante: " + e.getMessage())
                    );
                }
            });
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
                    } catch (Exception e) {
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