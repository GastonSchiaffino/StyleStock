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
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MovimientoStockController {
    private static final Logger logger = LoggerFactory.getLogger(MovimientoStockController.class);

    private final MovimientoStockService movimientoService;
    private final VarianteService varianteService;
    private final ProductoService productoService;

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<Variante> cbVariante;
    @FXML private ComboBox<String> cbTipoMovimiento;
    @FXML private TextField txtBuscarVariante;

    @FXML private TableView<MovimientoStock> tablaMovimientos;
    @FXML private TableColumn<MovimientoStock, Integer> colId;
    @FXML private TableColumn<MovimientoStock, String> colFecha;
    @FXML private TableColumn<MovimientoStock, String> colVariante;
    @FXML private TableColumn<MovimientoStock, String> colTipo;
    @FXML private TableColumn<MovimientoStock, Integer> colCantidad;
    @FXML private TableColumn<MovimientoStock, Integer> colStockAnt;
    @FXML private TableColumn<MovimientoStock, Integer> colStockNuevo;
    @FXML private TableColumn<MovimientoStock, String> colReferencia;

    @FXML private Label lblTotalMovimientos;
    @FXML private Label lblIngresos;
    @FXML private Label lblEgresos;
    @FXML private Label lblAjustes;
    @FXML private ProgressIndicator progressIndicator;

    private final ObservableList<MovimientoStock> movimientos;
    private final ObservableList<Variante> variantes;

    public MovimientoStockController() {
        this.movimientoService = new MovimientoStockService();
        this.varianteService = new VarianteService();
        this.productoService = new ProductoService();
        this.movimientos = FXCollections.observableArrayList();
        this.variantes = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        configurarEventos();
        buscarMovimientos();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colFecha.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(
                        cellData.getValue().getCreatedAt().format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                        )
                );
            }
            return new SimpleStringProperty("-");
        });

        colVariante.setCellValueFactory(cellData -> {
            MovimientoStock m = cellData.getValue();
            try {
                Variante v = varianteService.buscarPorId(m.getProductoId());
                return new SimpleStringProperty(v.getSku() + " - " +
                        (v.getProducto() != null ? v.getProducto().getNombre() : ""));
            } catch (Exception e) {
                return new SimpleStringProperty("ID: " + m.getProductoId());
            }
        });

        colTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTipo().getValor()));

        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colStockAnt.setCellValueFactory(new PropertyValueFactory<>("stockAnterior"));
        colStockNuevo.setCellValueFactory(new PropertyValueFactory<>("stockNuevo"));
        colReferencia.setCellValueFactory(new PropertyValueFactory<>("referencia"));

        // Colorear según tipo
        colTipo.setCellFactory(col -> new TableCell<MovimientoStock, String>() {
            @Override
            protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(tipo);
                    switch (tipo) {
                        case "INGRESO":
                            setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                            break;
                        case "EGRESO":
                        case "VENTA":
                            setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
                            break;
                        case "AJUSTE":
                            setStyle("-fx-text-fill: #F57C00; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        tablaMovimientos.setItems(movimientos);
    }

    private void configurarFiltros() {
        dpDesde.setValue(LocalDate.now().minusDays(30));
        dpHasta.setValue(LocalDate.now());

        cbTipoMovimiento.setItems(FXCollections.observableArrayList(
                "TODOS", "INGRESO", "EGRESO", "AJUSTE", "VENTA", "DEVOLUCION"
        ));
        cbTipoMovimiento.setValue("TODOS");

        cbVariante.setItems(variantes);
        cbVariante.setPromptText("Todas las variantes");
        cbVariante.setConverter(new StringConverter<Variante>() {
            @Override
            public String toString(Variante variante) {
                if (variante == null) return "Todas las variantes";
                return variante.getSku() + " - " +
                        (variante.getProducto() != null ? variante.getProducto().getNombre() : "");
            }

            @Override
            public Variante fromString(String string) {
                return null;
            }
        });
    }

    private void configurarEventos() {
        txtBuscarVariante.textProperty().addListener((obs, old, nuevo) -> {
            if (nuevo != null && nuevo.length() >= 3) {
                buscarVariantes(nuevo);
            } else if (nuevo == null || nuevo.isEmpty()) {
                variantes.clear();
            }
        });
    }

    private void buscarVariantes(String termino) {
        ejecutarEnBackground(() -> {
            try {
                var resultado = varianteService.buscarPorTexto(termino);
                Platform.runLater(() -> variantes.setAll(resultado));
            } catch (DataAccessException e) {
                logger.error("Error buscando variantes", e);
            }
        });
    }

    @FXML
    private void buscarMovimientos() {
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

        Integer varianteId = cbVariante.getValue() != null ? cbVariante.getValue().getId() : null;
        String tipo = cbTipoMovimiento.getValue();

        ejecutarEnBackground(() -> {
            try {
                var resultado = movimientoService.buscarMovimientos(desde, hasta, varianteId, tipo);

                long ingresos = resultado.stream()
                        .filter(m -> m.getTipo() == MovimientoStock.TipoMovimiento.INGRESO)
                        .count();

                long egresos = resultado.stream()
                        .filter(m -> m.getTipo() == MovimientoStock.TipoMovimiento.EGRESO ||
                                m.getTipo() == MovimientoStock.TipoMovimiento.VENTA)
                        .count();

                long ajustes = resultado.stream()
                        .filter(m -> m.getTipo() == MovimientoStock.TipoMovimiento.AJUSTE)
                        .count();

                Platform.runLater(() -> {
                    movimientos.setAll(resultado);
                    lblTotalMovimientos.setText(String.valueOf(resultado.size()));
                    lblIngresos.setText(String.valueOf(ingresos));
                    lblEgresos.setText(String.valueOf(egresos));
                    lblAjustes.setText(String.valueOf(ajustes));
                });

            } catch (DataAccessException e) {
                logger.error("Error obteniendo movimientos", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudieron cargar los movimientos"));
            }
        });
    }

    @FXML
    private void limpiarFiltros() {
        dpDesde.setValue(LocalDate.now().minusDays(30));
        dpHasta.setValue(LocalDate.now());
        cbVariante.setValue(null);
        cbTipoMovimiento.setValue("TODOS");
        txtBuscarVariante.clear();
        buscarMovimientos();
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