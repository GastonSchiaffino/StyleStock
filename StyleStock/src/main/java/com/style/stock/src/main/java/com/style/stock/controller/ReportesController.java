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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador para reportes y estadísticas
 */
public class ReportesController {
    private static final Logger logger = LoggerFactory.getLogger(ReportesController.class);

    // Servicios
    private final VentaService ventaService;
    private final VarianteService varianteService;
    private final CategoriaService categoriaService;
    private final ClienteService clienteService;

    // Labels de resumen
    @FXML private Label lblFechaActual;
    @FXML private Label lblVentasHoy;
    @FXML private Label lblCantVentasHoy;
    @FXML private Label lblVentasMes;
    @FXML private Label lblCantVentasMes;
    @FXML private Label lblStockBajo;
    @FXML private Label lblTotalVariantes;

    // Filtros
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<Categoria> cbCategoria;
    @FXML private ComboBox<Cliente> cbCliente;

    // Tab Ventas
    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, String> colVentaNumero;
    @FXML private TableColumn<Venta, String> colVentaFecha;
    @FXML private TableColumn<Venta, String> colVentaCliente;
    @FXML private TableColumn<Venta, String> colVentaTipo;
    @FXML private TableColumn<Venta, Integer> colVentaItems;
    @FXML private TableColumn<Venta, Double> colVentaTotal;
    @FXML private TableColumn<Venta, String> colVentaEstado;
    @FXML private Label lblTotalPeriodo;

    // Tab Top Productos
    @FXML private ComboBox<String> cbTopProductos;
    @FXML private TableView<Object> tablaTopProductos;
    @FXML private TableColumn<Object, Integer> colTopPosicion;
    @FXML private TableColumn<Object, String> colTopSku;
    @FXML private TableColumn<Object, String> colTopProducto;
    @FXML private TableColumn<Object, Integer> colTopCantidad;
    @FXML private TableColumn<Object, Double> colTopIngresos;
    @FXML private TableColumn<Object, String> colTopPorcentaje;

    // Tab Valorización
    @FXML private TableView<Variante> tablaValorizacion;
    @FXML private TableColumn<Variante, String> colValSku;
    @FXML private TableColumn<Variante, String> colValProducto;
    @FXML private TableColumn<Variante, Integer> colValStock;
    @FXML private TableColumn<Variante, Double> colValCosto;
    @FXML private TableColumn<Variante, Double> colValTotal;
    @FXML private TableColumn<Variante, Double> colValPrecio;
    @FXML private TableColumn<Variante, Double> colValPotencial;
    @FXML private Label lblTotalInversion;
    @FXML private Label lblValorPotencial;
    @FXML private Label lblGananciaPotencial;

    // Tab Movimientos
    @FXML private ComboBox<Variante> cbVarianteMovimientos;
    @FXML private ComboBox<String> cbTipoMovimiento;
    @FXML private TableView<MovimientoStock> tablaMovimientos;
    @FXML private TableColumn<MovimientoStock, Integer> colMovId;
    @FXML private TableColumn<MovimientoStock, String> colMovFecha;
    @FXML private TableColumn<MovimientoStock, String> colMovVariante;
    @FXML private TableColumn<MovimientoStock, String> colMovTipo;
    @FXML private TableColumn<MovimientoStock, Integer> colMovCantidad;
    @FXML private TableColumn<MovimientoStock, Integer> colMovStockAnt;
    @FXML private TableColumn<MovimientoStock, Integer> colMovStockNuevo;
    @FXML private TableColumn<MovimientoStock, String> colMovReferencia;

    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblEstadoCarga;

    // Datos
    private final ObservableList<Venta> ventas;
    private final ObservableList<Variante> variantes;

    public ReportesController() {
        this.ventaService = new VentaService();
        this.varianteService = new VarianteService();
        this.categoriaService = new CategoriaService();
        this.clienteService = new ClienteService();
        this.ventas = FXCollections.observableArrayList();
        this.variantes = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTablas();
        configurarFiltros();
        cargarDatosIniciales();
        actualizarResumen();
    }

    private void configurarTablas() {
        // Tabla Ventas
        colVentaNumero.setCellValueFactory(new PropertyValueFactory<>("numeroComprobante"));
        colVentaFecha.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFecha().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        );
        colVentaCliente.setCellValueFactory(cellData -> {
            Cliente c = cellData.getValue().getCliente();
            return new SimpleStringProperty(c != null ? c.getNombreCompleto() : "-");
        });
        colVentaTipo.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTipoComprobante().getValor())
        );
        colVentaItems.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(
                cellData.getValue().getDetalles() != null ? 
                cellData.getValue().getDetalles().size() : 0
            )
        );
        colVentaTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colVentaEstado.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getEstado().getValor())
        );

        // Formatear total
        colVentaTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                setText(empty || total == null ? null : String.format("$%.2f", total));
            }
        });

        tablaVentas.setItems(ventas);

        // Tabla Valorización
        colValSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colValProducto.setCellValueFactory(cellData -> {
            Producto p = cellData.getValue().getProducto();
            return new SimpleStringProperty(p != null ? p.getNombre() : "-");
        });
        colValStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colValCosto.setCellValueFactory(new PropertyValueFactory<>("precioCosto"));
        colValTotal.setCellValueFactory(cellData -> {
            Variante v = cellData.getValue();
            double total = v.getStock() * v.getPrecioCosto();
            return new javafx.beans.property.SimpleObjectProperty<>(total);
        });
        colValPrecio.setCellValueFactory(new PropertyValueFactory<>("precioMinorista"));
        colValPotencial.setCellValueFactory(cellData -> {
            Variante v = cellData.getValue();
            double potencial = v.getStock() * v.getPrecioMinorista();
            return new javafx.beans.property.SimpleObjectProperty<>(potencial);
        });

        // Formatear valores
        colValCosto.setCellFactory(col -> createMoneyCell());
        colValTotal.setCellFactory(col -> createMoneyCell());
        colValPrecio.setCellFactory(col -> createMoneyCell());
        colValPotencial.setCellFactory(col -> createMoneyCell());

        tablaValorizacion.setItems(variantes);
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

    private void configurarFiltros() {
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
        lblFechaActual.setText("Hoy: " + LocalDate.now().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private void cargarDatosIniciales() {
        ejecutarEnBackground(() -> {
            try {
                // Cargar categorías
                var cats = categoriaService.listarTodas();
                Platform.runLater(() -> cbCategoria.setItems(
                    FXCollections.observableArrayList(cats)));

                // Cargar clientes
                var clientes = clienteService.listarTodos();
                Platform.runLater(() -> cbCliente.setItems(
                    FXCollections.observableArrayList(clientes)));

            } catch (DataAccessException e) {
                logger.error("Error cargando datos iniciales", e);
            }
        });
    }

    private void actualizarResumen() {
        ejecutarEnBackground(() -> {
            try {
                // Ventas de hoy
                LocalDate hoy = LocalDate.now();
                var ventasHoy = ventaService.listarRecientes(100).stream()
                    .filter(v -> v.getFecha().equals(hoy))
                    .toList();
                
                double totalHoy = ventasHoy.stream()
                    .mapToDouble(Venta::getTotal).sum();

                // Ventas del mes
                LocalDate inicioMes = hoy.withDayOfMonth(1);
                var ventasMes = ventaService.listarRecientes(500).stream()
                    .filter(v -> !v.getFecha().isBefore(inicioMes))
                    .toList();
                
                double totalMes = ventasMes.stream()
                    .mapToDouble(Venta::getTotal).sum();

                // Stock bajo
                var stockBajo = varianteService.listarStockBajo();

                // Total variantes
                // Necesitaríamos un método para contar todas, por ahora aproximado
                int totalVariantes = stockBajo.size(); // Placeholder

                Platform.runLater(() -> {
                    lblVentasHoy.setText(String.format("$%.2f", totalHoy));
                    lblCantVentasHoy.setText(ventasHoy.size() + " ventas");
                    lblVentasMes.setText(String.format("$%.2f", totalMes));
                    lblCantVentasMes.setText(ventasMes.size() + " ventas");
                    lblStockBajo.setText(String.valueOf(stockBajo.size()));
                    lblTotalVariantes.setText(String.valueOf(totalVariantes));
                });

            } catch (DataAccessException e) {
                logger.error("Error actualizando resumen", e);
            }
        });
    }

    @FXML
    private void aplicarFiltros() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null) {
            AlertUtils.mostrarAdvertencia("Fechas requeridas", 
                "Seleccione rango de fechas");
            return;
        }

        if (desde.isAfter(hasta)) {
            AlertUtils.mostrarAdvertencia("Fechas inválidas", 
                "La fecha 'Desde' no puede ser posterior a 'Hasta'");
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                var todasVentas = ventaService.listarRecientes(1000);
                var ventasFiltradas = todasVentas.stream()
                    .filter(v -> !v.getFecha().isBefore(desde) && 
                                !v.getFecha().isAfter(hasta))
                    .toList();

                double total = ventasFiltradas.stream()
                    .mapToDouble(Venta::getTotal).sum();

                Platform.runLater(() -> {
                    ventas.setAll(ventasFiltradas);
                    lblTotalPeriodo.setText(String.format("$%.2f", total));
                });

            } catch (DataAccessException e) {
                logger.error("Error aplicando filtros", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudieron aplicar los filtros")
                );
            }
        });
    }

    @FXML
    private void limpiarFiltros() {
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());
        cbCategoria.setValue(null);
        cbCliente.setValue(null);
        aplicarFiltros();
    }

    @FXML
    private void exportarVentas() {
        AlertUtils.mostrarInfo("Exportar", 
            "Exportación de ventas a Excel en desarrollo");
    }

    @FXML
    private void imprimirVentas() {
        AlertUtils.mostrarInfo("Imprimir", 
            "Funcionalidad de impresión en desarrollo");
    }

    @FXML
    private void actualizarTopProductos() {
        AlertUtils.mostrarInfo("Top Productos", 
            "Reporte de productos más vendidos en desarrollo");
    }

    @FXML
    private void exportarTopProductos() {
        AlertUtils.mostrarInfo("Exportar", "Exportación en desarrollo");
    }

    @FXML
    private void calcularValorizacion() {
        ejecutarEnBackground(() -> {
            try {
                // Obtener todas las variantes activas
                // Por ahora, usaremos listado de stock bajo como ejemplo
                var todasVariantes = varianteService.listarStockBajo();

                double totalInversion = 0;
                double valorPotencial = 0;

                for (Variante v : todasVariantes) {
                    totalInversion += v.getStock() * v.getPrecioCosto();
                    valorPotencial += v.getStock() * v.getPrecioMinorista();
                }

                double ganancia = valorPotencial - totalInversion;

                double finalTotalInversion = totalInversion;
                double finalValorPotencial = valorPotencial;
                double finalGanancia = ganancia;

                Platform.runLater(() -> {
                    variantes.setAll(todasVariantes);
                    lblTotalInversion.setText(String.format("$%.2f", finalTotalInversion));
                    lblValorPotencial.setText(String.format("$%.2f", finalValorPotencial));
                    lblGananciaPotencial.setText(String.format("$%.2f", finalGanancia));
                });

            } catch (DataAccessException e) {
                logger.error("Error calculando valorización", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudo calcular la valorización")
                );
            }
        });
    }

    @FXML
    private void exportarValorizacion() {
        AlertUtils.mostrarInfo("Exportar", "Exportación en desarrollo");
    }

    @FXML
    private void buscarMovimientos() {
        AlertUtils.mostrarInfo("Movimientos", 
            "Consulta de movimientos de stock en desarrollo");
    }

    @FXML
    private void exportarMovimientos() {
        AlertUtils.mostrarInfo("Exportar", "Exportación en desarrollo");
    }

    private void ejecutarEnBackground(Runnable tarea) {
        progressIndicator.setVisible(true);
        lblEstadoCarga.setText("Cargando...");
        new Thread(() -> {
            try {
                tarea.run();
            } finally {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    lblEstadoCarga.setText("");
                });
            }
        }).start();
    }
}