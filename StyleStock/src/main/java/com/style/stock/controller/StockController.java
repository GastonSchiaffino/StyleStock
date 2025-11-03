package com.style.stock.controller;

import com.style.stock.exception.DataAccessException;
import com.style.stock.model.*;
import com.style.stock.service.*;
import com.style.stock.util.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StockController {
    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    private final VarianteService varianteService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    // TAB STOCK TOTAL
    @FXML private TableView<Variante> tablaStockTotal;
    @FXML private TableColumn<Variante, String> colTotalSku;
    @FXML private TableColumn<Variante, String> colTotalProducto;
    @FXML private TableColumn<Variante, String> colTotalAtributos;
    @FXML private TableColumn<Variante, String> colTotalCategoria;
    @FXML private TableColumn<Variante, Integer> colTotalStock;
    @FXML private TableColumn<Variante, Integer> colTotalStockMin;
    @FXML private TableColumn<Variante, String> colTotalEstado;
    @FXML private TextField txtBuscarTotal;
    @FXML private ComboBox<Categoria> cbFiltroCategoria;
    @FXML private Label lblTotalStockTotal;
    @FXML private Label lblTotalVariantesTotal;
    @FXML private Label lblValorInventario;

    // TAB STOCK BAJO
    @FXML private TableView<Variante> tablaStockBajo;
    @FXML private TableColumn<Variante, String> colSku;
    @FXML private TableColumn<Variante, String> colProducto;
    @FXML private TableColumn<Variante, String> colAtributos;
    @FXML private TableColumn<Variante, Integer> colStock;
    @FXML private TableColumn<Variante, Integer> colStockMin;
    @FXML private TableColumn<Variante, Integer> colFaltante;
    @FXML private TableColumn<Variante, String> colCategoria;
    @FXML private Label lblTotalVariantes;
    @FXML private Label lblTotalFaltante;

    @FXML private ProgressIndicator progressIndicator;

    private final ObservableList<Variante> variantesTotal;
    private final ObservableList<Variante> variantesBajo;
    private final ObservableList<Categoria> categorias;
    private Variante varianteSeleccionada;

    public StockController() {
        this.varianteService = new VarianteService();
        this.productoService = new ProductoService();
        this.categoriaService = new CategoriaService();
        this.variantesTotal = FXCollections.observableArrayList();
        this.variantesBajo = FXCollections.observableArrayList();
        this.categorias = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTablaTotal();
        configurarTablaStockBajo();
        configurarFiltros();
        configurarEventos();
        cargarCategorias();
        cargarStockTotal();
        cargarStockBajo();
    }

    // ========== CONFIGURACIÓN STOCK TOTAL ==========

    private void configurarTablaTotal() {
        colTotalSku.setCellValueFactory(new PropertyValueFactory<>("sku"));

        colTotalProducto.setCellValueFactory(cellData -> {
            Producto p = cellData.getValue().getProducto();
            return new SimpleStringProperty(p != null ? p.getNombre() : "-");
        });

        colTotalAtributos.setCellValueFactory(cellData ->
                new SimpleStringProperty(getAtributosTexto(cellData.getValue()))
        );

        colTotalCategoria.setCellValueFactory(cellData -> {
            Producto p = cellData.getValue().getProducto();
            if (p != null && p.getCategoria() != null) {
                return new SimpleStringProperty(p.getCategoria().getNombre());
            }
            return new SimpleStringProperty("-");
        });

        colTotalStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colTotalStockMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        colTotalEstado.setCellValueFactory(cellData -> {
            Variante v = cellData.getValue();
            if (v.getStock() == 0) return new SimpleStringProperty("SIN STOCK");
            if (v.isStockBajo()) return new SimpleStringProperty("BAJO");
            return new SimpleStringProperty("OK");
        });

        // Estilo para columnas
        colTotalStock.setCellFactory(col -> new TableCell<Variante, Integer>() {
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
                        if (v.getStock() == 0) {
                            setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                        } else if (v.isStockBajo()) {
                            setStyle("-fx-text-fill: #F57C00; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        });

        colTotalEstado.setCellFactory(col -> new TableCell<Variante, String>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(estado);
                    switch (estado) {
                        case "SIN STOCK":
                            setStyle("-fx-background-color: #FFCDD2; -fx-text-fill: #C62828; -fx-font-weight: bold;");
                            break;
                        case "BAJO":
                            setStyle("-fx-background-color: #FFE0B2; -fx-text-fill: #E65100; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tablaStockTotal.setItems(variantesTotal);
    }

    // ========== CONFIGURACIÓN STOCK BAJO ==========

    private void configurarTablaStockBajo() {
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));

        colProducto.setCellValueFactory(cellData -> {
            Producto p = cellData.getValue().getProducto();
            return new SimpleStringProperty(p != null ? p.getNombre() : "-");
        });

        colAtributos.setCellValueFactory(cellData ->
                new SimpleStringProperty(getAtributosTexto(cellData.getValue()))
        );

        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStockMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        colFaltante.setCellValueFactory(cellData -> {
            Variante v = cellData.getValue();
            int faltante = v.getStockMinimo() - v.getStock();
            return new javafx.beans.property.SimpleObjectProperty<>(faltante);
        });

        colCategoria.setCellValueFactory(cellData -> {
            Producto p = cellData.getValue().getProducto();
            if (p != null && p.getCategoria() != null) {
                return new SimpleStringProperty(p.getCategoria().getNombre());
            }
            return new SimpleStringProperty("-");
        });

        // Estilo para resaltar
        colStock.setCellFactory(col -> new TableCell<Variante, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(stock.toString());
                    setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
                }
            }
        });

        colFaltante.setCellFactory(col -> new TableCell<Variante, Integer>() {
            @Override
            protected void updateItem(Integer faltante, boolean empty) {
                super.updateItem(faltante, empty);
                if (empty || faltante == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(faltante.toString());
                    setStyle("-fx-background-color: #FFCDD2; -fx-font-weight: bold;");
                }
            }
        });

        tablaStockBajo.setItems(variantesBajo);
    }

    private void configurarFiltros() {
        if (cbFiltroCategoria != null) {
            cbFiltroCategoria.setItems(categorias);
        }
    }

    private void configurarEventos() {
        if (tablaStockTotal != null) {
            tablaStockTotal.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldVal, newVal) -> varianteSeleccionada = newVal
            );
        }

        if (tablaStockBajo != null) {
            tablaStockBajo.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldVal, newVal) -> varianteSeleccionada = newVal
            );

            tablaStockBajo.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && varianteSeleccionada != null) {
                    ajustarStock();
                }
            });
        }

        if (txtBuscarTotal != null) {
            txtBuscarTotal.textProperty().addListener((obs, old, nuevo) -> filtrarStockTotal());
        }
    }

    // ========== CARGA DE DATOS ==========

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

    @FXML
    private void cargarStockTotal() {
        ejecutarEnBackground(() -> {
            try {
                List<Variante> lista = new ArrayList<>();
                var productos = productoService.listarTodos();

                for (Producto p : productos) {
                    var variantes = varianteService.listarPorProducto(p.getId());
                    for (Variante v : variantes) {
                        v.setProducto(p);
                    }
                    lista.addAll(variantes);
                }

                int totalStock = lista.stream().mapToInt(Variante::getStock).sum();
                double valorTotal = lista.stream()
                        .mapToDouble(v -> v.getStock() * v.getPrecioCosto())
                        .sum();

                Platform.runLater(() -> {
                    variantesTotal.setAll(lista);
                    lblTotalVariantesTotal.setText(String.valueOf(lista.size()));
                    lblTotalStockTotal.setText(String.valueOf(totalStock));
                    lblValorInventario.setText(String.format("$%.2f", valorTotal));
                });

            } catch (DataAccessException e) {
                logger.error("Error cargando stock total", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudo cargar el stock total")
                );
            }
        });
    }

    @FXML
    private void cargarStockBajo() {
        ejecutarEnBackground(() -> {
            try {
                var lista = varianteService.listarStockBajo();

                for (Variante v : lista) {
                    if (v.getProductoId() != null) {
                        try {
                            Producto p = productoService.buscarPorId(v.getProductoId());
                            v.setProducto(p);
                        } catch (Exception e) {
                            logger.warn("Error cargando producto: {}", v.getSku());
                        }
                    }
                }

                int totalFaltante = lista.stream()
                        .mapToInt(v -> v.getStockMinimo() - v.getStock())
                        .sum();

                Platform.runLater(() -> {
                    variantesBajo.setAll(lista);
                    lblTotalVariantes.setText(String.valueOf(lista.size()));
                    lblTotalFaltante.setText(String.valueOf(totalFaltante));
                });

            } catch (DataAccessException e) {
                logger.error("Error cargando stock bajo", e);
            }
        });
    }

    // ========== FILTROS ==========

    @FXML
    private void filtrarStockTotal() {
        String busqueda = txtBuscarTotal != null ? txtBuscarTotal.getText().toLowerCase() : "";
        Categoria catFiltro = cbFiltroCategoria != null ? cbFiltroCategoria.getValue() : null;

        ejecutarEnBackground(() -> {
            try {
                List<Variante> lista = new ArrayList<>();
                var productos = productoService.listarTodos();

                for (Producto p : productos) {
                    // Filtro por categoría
                    if (catFiltro != null && !p.getCategoriaId().equals(catFiltro.getId())) {
                        continue;
                    }

                    var variantes = varianteService.listarPorProducto(p.getId());
                    for (Variante v : variantes) {
                        v.setProducto(p);

                        // Filtro por búsqueda
                        if (!busqueda.isEmpty()) {
                            boolean coincide = v.getSku().toLowerCase().contains(busqueda) ||
                                    p.getNombre().toLowerCase().contains(busqueda);
                            if (!coincide) continue;
                        }

                        lista.add(v);
                    }
                }

                Platform.runLater(() -> variantesTotal.setAll(lista));

            } catch (DataAccessException e) {
                logger.error("Error filtrando stock", e);
            }
        });
    }

    @FXML
    private void limpiarFiltros() {
        if (txtBuscarTotal != null) txtBuscarTotal.clear();
        if (cbFiltroCategoria != null) cbFiltroCategoria.setValue(null);
        cargarStockTotal();
    }

    // ========== ACCIONES ==========

    @FXML
    private void ajustarStock() {
        if (varianteSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una variante");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Ajustar Stock");
        dialog.setHeaderText(
                String.format("SKU: %s\nStock actual: %d\nStock mínimo: %d",
                        varianteSeleccionada.getSku(),
                        varianteSeleccionada.getStock(),
                        varianteSeleccionada.getStockMinimo())
        );
        dialog.setContentText("Cantidad (+ agregar / - quitar):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cantidad -> {
            try {
                int cant = Integer.parseInt(cantidad);

                ejecutarEnBackground(() -> {
                    try {
                        varianteService.ajustarStock(
                                varianteSeleccionada.getId(),
                                cant,
                                "Ajuste manual"
                        );

                        Platform.runLater(() -> {
                            AlertUtils.mostrarExito("Éxito", "Stock ajustado correctamente");
                            cargarStockTotal();
                            cargarStockBajo();
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
    private void exportarExcel() {
        AlertUtils.mostrarInfo("Exportar", "Exportación en desarrollo");
    }

    // ========== UTILIDADES ==========

    private String getAtributosTexto(Variante v) {
        if (v.getAtributos() != null && !v.getAtributos().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < v.getAtributos().size(); i++) {
                sb.append(v.getAtributos().get(i).getValor());
                if (i < v.getAtributos().size() - 1) sb.append(", ");
            }
            return sb.toString();
        }
        return "-";
    }

    private void ejecutarEnBackground(Runnable tarea) {
        if (progressIndicator != null) progressIndicator.setVisible(true);
        new Thread(() -> {
            try {
                tarea.run();
            } finally {
                Platform.runLater(() -> {
                    if (progressIndicator != null) progressIndicator.setVisible(false);
                });
            }
        }).start();
    }
}