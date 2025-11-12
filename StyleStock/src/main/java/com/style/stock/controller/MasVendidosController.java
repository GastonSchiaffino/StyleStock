package com.style.stock.controller;

import com.style.stock.exception.DataAccessException;
import com.style.stock.model.Categoria;
import com.style.stock.service.CategoriaService;
import com.style.stock.service.VentaService;
import com.style.stock.util.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MasVendidosController {
    private static final Logger logger = LoggerFactory.getLogger(MasVendidosController.class);

    private final VentaService ventaService;
    private final CategoriaService categoriaService;

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<Categoria> cbCategoria;
    @FXML private ComboBox<String> cbTop;
    @FXML private TableView<Map<String, Object>> tablaProductos;
    @FXML private TableColumn<Map<String, Object>, Integer> colPosicion;
    @FXML private TableColumn<Map<String, Object>, String> colSku;
    @FXML private TableColumn<Map<String, Object>, String> colProducto;
    @FXML private TableColumn<Map<String, Object>, Integer> colCantidad;
    @FXML private TableColumn<Map<String, Object>, Double> colIngresos;
    @FXML private TableColumn<Map<String, Object>, String> colPorcentaje;
    @FXML private Label lblTotalUnidades;
    @FXML private Label lblTotalIngresos;
    @FXML private ProgressIndicator progressIndicator;

    private final ObservableList<Map<String, Object>> productos;
    private final ObservableList<Categoria> categorias;

    public MasVendidosController() {
        this.ventaService = new VentaService();
        this.categoriaService = new CategoriaService();
        this.productos = FXCollections.observableArrayList();
        this.categorias = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        cargarCategorias();
        aplicarFiltrosPorDefecto();
    }

    private void configurarTabla() {
        colPosicion.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>((Integer) cellData.getValue().get("posicion")));

        colSku.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("sku")));

        colProducto.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("producto")));

        colCantidad.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>((Integer) cellData.getValue().get("cantidad")));

        colIngresos.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>((Double) cellData.getValue().get("ingresos")));

        colPorcentaje.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("porcentaje")));

        colIngresos.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double valor, boolean empty) {
                super.updateItem(valor, empty);
                setText(empty || valor == null ? null : String.format("$%.2f", valor));
                setStyle(empty ? "" : "-fx-font-weight: bold;");
            }
        });

        tablaProductos.setItems(productos);
    }

    private void configurarFiltros() {
        dpDesde.setValue(LocalDate.now().minusMonths(1));
        dpHasta.setValue(LocalDate.now());

        cbTop.setItems(FXCollections.observableArrayList("10", "20", "50", "100"));
        cbTop.setValue("10");

        cbCategoria.setItems(categorias);
        cbCategoria.setPromptText("Todas las categorías");

        // AGREGAR ESTE STRINGCONVERTER
        cbCategoria.setConverter(new javafx.util.StringConverter<Categoria>() {
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
    }

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

    private void aplicarFiltrosPorDefecto() {
        buscarProductos();
    }

    @FXML
    private void buscarProductos() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null) {
            AlertUtils.mostrarAdvertencia("Fechas requeridas", "Seleccione rango de fechas");
            return;
        }

        if (desde.isAfter(hasta)) {
            AlertUtils.mostrarAdvertencia("Fechas inválidas",
                    "La fecha 'Desde' no puede ser posterior a 'Hasta'");
            return;
        }

        int limite = Integer.parseInt(cbTop.getValue());
        Integer categoriaId = cbCategoria.getValue() != null ? cbCategoria.getValue().getId() : null;

        ejecutarEnBackground(() -> {
            try {
                List<Map<String, Object>> resultado = ventaService.obtenerProductosMasVendidos(
                        desde, hasta, limite, categoriaId);

                double totalUnidades = resultado.stream()
                        .mapToDouble(m -> ((Integer) m.get("cantidad")).doubleValue()).sum();
                double totalIngresos = resultado.stream()
                        .mapToDouble(m -> (Double) m.get("ingresos")).sum();

                Platform.runLater(() -> {
                    productos.setAll(resultado);
                    lblTotalUnidades.setText(String.format("%.0f", totalUnidades));
                    lblTotalIngresos.setText(String.format("$%.2f", totalIngresos));
                });

            } catch (DataAccessException e) {
                logger.error("Error obteniendo productos más vendidos", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudieron cargar los datos"));
            }
        });
    }

    @FXML
    private void limpiarFiltros() {
        dpDesde.setValue(LocalDate.now().minusMonths(1));
        dpHasta.setValue(LocalDate.now());
        cbCategoria.setValue(null);
        cbTop.setValue("10");
        buscarProductos();
    }

    @FXML
    private void exportarExcel() {
        AlertUtils.mostrarInfo("Exportar", "Funcionalidad en desarrollo");
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