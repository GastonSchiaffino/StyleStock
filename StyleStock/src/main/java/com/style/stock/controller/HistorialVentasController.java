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
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador para Historial de Ventas
 */
public class HistorialVentasController {
    private static final Logger logger = LoggerFactory.getLogger(HistorialVentasController.class);

    // Servicios
    private final VentaService ventaService;
    private final ClienteService clienteService;

    // Filtros
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private ComboBox<String> cbEstado;
    @FXML private ComboBox<String> cbTipoComprobante;
    @FXML private TextField txtBuscarNumero;

    // Tabla principal
    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, Integer> colId;
    @FXML private TableColumn<Venta, String> colNumero;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colCliente;
    @FXML private TableColumn<Venta, String> colTipo;
    @FXML private TableColumn<Venta, String> colComprobante;
    @FXML private TableColumn<Venta, Integer> colItems;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, String> colEstado;

    // Panel de detalles
    @FXML private VBox panelDetalles;
    @FXML private Label lblDetalleNumero;
    @FXML private Label lblDetalleFecha;
    @FXML private Label lblDetalleCliente;
    @FXML private Label lblDetalleTipo;
    @FXML private TableView<DetalleVenta> tablaDetalles;
    @FXML private TableColumn<DetalleVenta, String> colDetSku;
    @FXML private TableColumn<DetalleVenta, String> colDetDescripcion;
    @FXML private TableColumn<DetalleVenta, Integer> colDetCantidad;
    @FXML private TableColumn<DetalleVenta, Double> colDetPrecio;
    @FXML private TableColumn<DetalleVenta, Double> colDetSubtotal;
    @FXML private ListView<String> lvPagos;

    // Estadísticas
    @FXML private Label lblTotalVentas;
    @FXML private Label lblTotalFacturado;
    @FXML private Label lblTicketPromedio;
    @FXML private Label lblCompletadas;
    @FXML private Label lblAnuladas;

    // Botones
    @FXML private Button btnRegistrarPago;
    @FXML private Button btnVerDetalle;
    @FXML private Button btnAnular;
    @FXML private Button btnImprimir;
    @FXML private Button btnExportar;
    @FXML private ProgressIndicator progressIndicator;

    // Datos
    private final ObservableList<Venta> ventas;
    private final ObservableList<Cliente> clientes;
    private final ObservableList<DetalleVenta> detalles;
    private Venta ventaSeleccionada;

    public HistorialVentasController() {
        this.ventaService = new VentaService();
        this.clienteService = new ClienteService();
        this.ventas = FXCollections.observableArrayList();
        this.clientes = FXCollections.observableArrayList();
        this.detalles = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTablas();
        configurarFiltros();
        configurarEventos();
        cargarDatosIniciales();
        ocultarPanelDetalles();
    }

    private void configurarTablas() {
        // Tabla principal
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroComprobante"));

        colFecha.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                " " + cellData.getValue().getHora().format(DateTimeFormatter.ofPattern("HH:mm"))
                )
        );

        colCliente.setCellValueFactory(cellData -> {
            Cliente c = cellData.getValue().getCliente();
            return new SimpleStringProperty(c != null ? c.getNombreCompleto() : "-");
        });

        colTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTipoVenta().getValor())
        );

        colComprobante.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTipoComprobante().getValor())
        );

        colItems.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        cellData.getValue().getDetalles() != null ?
                                cellData.getValue().getDetalles().size() : 0
                )
        );

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        colEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEstado().getValor())
        );

        // Formatear columnas
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%.2f", total));
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(estado);
                    if ("COMPLETADA".equals(estado)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else if ("ANULADA".equals(estado)) {
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tablaVentas.setItems(ventas);

        // Tabla de detalles
        colDetSku.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getVariante() != null ?
                                cellData.getValue().getVariante().getSku() : ""
                )
        );

        colDetDescripcion.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getVariante() != null ?
                                cellData.getValue().getVariante().getDescripcionCompleta() : ""
                )
        );

        colDetCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colDetPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colDetSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        colDetPrecio.setCellFactory(col -> createMoneyCell());
        colDetSubtotal.setCellFactory(col -> createMoneyCell());

        tablaDetalles.setItems(detalles);
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
        // Fechas por defecto: últimos 30 días
        dpHasta.setValue(LocalDate.now());
        dpDesde.setValue(LocalDate.now().minusDays(30));

        // ComboBox de estados
        cbEstado.setItems(FXCollections.observableArrayList(
                "TODAS", "COMPLETADA", "ANULADA", "PENDIENTE"
        ));
        cbEstado.setValue("TODAS");

        // ComboBox de comprobantes
        cbTipoComprobante.setItems(FXCollections.observableArrayList(
                "TODOS", "TICKET", "FACTURA_A", "FACTURA_B", "FACTURA_C"
        ));
        cbTipoComprobante.setValue("TODOS");

        // ComboBox de clientes
        cbCliente.setItems(clientes);
        cbCliente.setPromptText("Todos los clientes");
    }

    private void configurarEventos() {
        tablaVentas.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    ventaSeleccionada = newVal;
                    actualizarEstadoBotones();
                    if (newVal != null) {
                        mostrarDetalleVenta(newVal);
                    }
                }
        );

        // Doble click para ver detalle
        tablaVentas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && ventaSeleccionada != null) {
                verDetalle();
            }
        });
    }

    private void cargarDatosIniciales() {
        ejecutarEnBackground(() -> {
            try {
                // Cargar clientes
                var listaClientes = clienteService.listarTodos();
                Platform.runLater(() -> clientes.setAll(listaClientes));

                // Cargar ventas iniciales
                buscarVentas();

            } catch (DataAccessException e) {
                logger.error("Error cargando datos iniciales", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudieron cargar los datos iniciales")
                );
            }
        });
    }

    @FXML
    private void buscarVentas() {
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

        ejecutarEnBackground(() -> {
            try {
                // Buscar por número si hay texto
                String numeroComprobante = txtBuscarNumero.getText().trim();
                if (!numeroComprobante.isEmpty()) {
                    try {
                        Venta venta = ventaService.buscarPorNumeroComprobante(numeroComprobante);
                        Platform.runLater(() -> {
                            ventas.clear();
                            ventas.add(venta);
                            actualizarEstadisticas();
                        });
                        return;
                    } catch (NotFoundException e) {
                        Platform.runLater(() ->
                                AlertUtils.mostrarAdvertencia("No encontrado",
                                        "No existe venta con ese número")
                        );
                        return;
                    }
                }

                // Buscar por rango
                var lista = ventaService.buscarPorRangoFechas(desde, hasta);

                // Aplicar filtros adicionales
                String estadoFiltro = cbEstado.getValue();
                if (!"TODAS".equals(estadoFiltro)) {
                    lista = lista.stream()
                            .filter(v -> v.getEstado().getValor().equals(estadoFiltro))
                            .toList();
                }

                String comprobanteFiltro = cbTipoComprobante.getValue();
                if (!"TODOS".equals(comprobanteFiltro)) {
                    lista = lista.stream()
                            .filter(v -> v.getTipoComprobante().getValor().equals(comprobanteFiltro))
                            .toList();
                }

                Cliente clienteFiltro = cbCliente.getValue();
                if (clienteFiltro != null) {
                    lista = lista.stream()
                            .filter(v -> v.getClienteId().equals(clienteFiltro.getId()))
                            .toList();
                }

                var listaFinal = lista;
                Platform.runLater(() -> {
                    ventas.setAll(listaFinal);
                    actualizarEstadisticas();
                });

            } catch (DataAccessException e) {
                logger.error("Error buscando ventas", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudieron buscar las ventas")
                );
            }
        });
    }

    @FXML
    private void limpiarFiltros() {
        dpDesde.setValue(LocalDate.now().minusDays(30));
        dpHasta.setValue(LocalDate.now());
        cbEstado.setValue("TODAS");
        cbTipoComprobante.setValue("TODOS");
        cbCliente.setValue(null);
        txtBuscarNumero.clear();
        buscarVentas();
    }

    private void actualizarEstadisticas() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null) return;

        ejecutarEnBackground(() -> {
            try {
                Map<String, Object> stats = ventaService.obtenerEstadisticasPeriodo(desde, hasta);

                Platform.runLater(() -> {
                    lblTotalVentas.setText(String.valueOf(stats.get("totalVentas")));
                    lblTotalFacturado.setText(String.format("$%.2f", stats.get("totalFacturado")));
                    lblTicketPromedio.setText(String.format("$%.2f", stats.get("ticketPromedio")));
                    lblCompletadas.setText(String.valueOf(stats.get("completadas")));
                    lblAnuladas.setText(String.valueOf(stats.get("anuladas")));
                });

            } catch (DataAccessException e) {
                logger.error("Error obteniendo estadísticas", e);
            }
        });
    }

    private void mostrarDetalleVenta(Venta venta) {
        if (venta == null) {
            ocultarPanelDetalles();
            return;
        }

        panelDetalles.setVisible(true);

        lblDetalleNumero.setText(venta.getNumeroComprobante());
        lblDetalleFecha.setText(venta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lblDetalleCliente.setText(venta.getCliente() != null ? venta.getCliente().getNombreCompleto() : "-");
        lblDetalleTipo.setText(venta.getTipoComprobante().getValor() + " - " + venta.getTipoVenta().getValor());

        if (venta.getDetalles() != null) {
            detalles.setAll(venta.getDetalles());
        } else {
            detalles.clear();
        }

        if (venta.getPagos() != null) {
            lvPagos.getItems().clear();
            for (PagoVenta pago : venta.getPagos()) {
                String texto = String.format("%s: $%.2f %s",
                        pago.getMetodoPago() != null ? pago.getMetodoPago().getNombre() : "N/A",
                        pago.getMonto(),
                        pago.getCuotas() > 1 ? "(" + pago.getCuotas() + " cuotas)" : ""
                );
                lvPagos.getItems().add(texto);
            }
        }
    }

    private void ocultarPanelDetalles() {
        panelDetalles.setVisible(false);
        detalles.clear();
        lvPagos.getItems().clear();
    }

    @FXML
    private void verDetalle() {
        if (ventaSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una venta");
            return;
        }

        // Mostrar diálogo con detalle completo
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Detalle de Venta");
        dialog.setHeaderText("Venta: " + ventaSeleccionada.getNumeroComprobante());

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        content.getChildren().addAll(
                new Label("Cliente: " + (ventaSeleccionada.getCliente() != null ?
                        ventaSeleccionada.getCliente().getNombreCompleto() : "-")),
                new Label("Fecha: " + ventaSeleccionada.getFecha().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
                new Label("Tipo: " + ventaSeleccionada.getTipoComprobante().getValor()),
                new Label("Estado: " + ventaSeleccionada.getEstado().getValor()),
                new Separator(),
                new Label("Subtotal: $" + String.format("%.2f", ventaSeleccionada.getSubtotal())),
                new Label("Descuento: $" + String.format("%.2f", ventaSeleccionada.getDescuento())),
                new Label("TOTAL: $" + String.format("%.2f", ventaSeleccionada.getTotal()))
        );

        if (ventaSeleccionada.getNotas() != null && !ventaSeleccionada.getNotas().isEmpty()) {
            content.getChildren().addAll(
                    new Separator(),
                    new Label("Notas: " + ventaSeleccionada.getNotas())
            );
        }

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    @FXML
    private void anularVenta() {
        if (ventaSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una venta");
            return;
        }

        if (ventaSeleccionada.getEstado() == Venta.EstadoVenta.ANULADA) {
            AlertUtils.mostrarAdvertencia("Ya anulada", "Esta venta ya está anulada");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
                "⚠️ Anular Venta",
                "¿Está seguro de anular esta venta?",
                "Venta: " + ventaSeleccionada.getNumeroComprobante() + "\n" +
                        "Total: $" + String.format("%.2f", ventaSeleccionada.getTotal()) + "\n\n" +
                        "Esto revertirá el stock de los productos"
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    ventaService.anular(ventaSeleccionada.getId());
                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Venta anulada correctamente");
                        buscarVentas();
                    });
                } catch (Exception e) {
                    logger.error("Error anulando venta", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", "No se pudo anular la venta: " + e.getMessage())
                    );
                }
            });
        }
    }

    @FXML
    private void imprimirVenta() {
        if (ventaSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una venta");
            return;
        }

        AlertUtils.mostrarInfo("En desarrollo",
                "Funcionalidad de impresión en desarrollo.\n" +
                        "Venta: " + ventaSeleccionada.getNumeroComprobante());
    }

    @FXML
    private void exportarExcel() {
        AlertUtils.mostrarInfo("En desarrollo",
                "Funcionalidad de exportación en desarrollo");
    }

    private void actualizarEstadoBotones() {
        boolean haySeleccion = ventaSeleccionada != null;
        btnVerDetalle.setDisable(!haySeleccion);
        btnAnular.setDisable(!haySeleccion ||
                (ventaSeleccionada != null && ventaSeleccionada.getEstado() == Venta.EstadoVenta.ANULADA));
        btnImprimir.setDisable(!haySeleccion);

        // ✅ NUEVO: Habilitar registrar pago solo si está pendiente
        if (btnRegistrarPago != null) {
            btnRegistrarPago.setDisable(!haySeleccion ||
                    (ventaSeleccionada != null && ventaSeleccionada.getEstado() != Venta.EstadoVenta.PENDIENTE));
        }
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
    private void registrarPago() {
        if (ventaSeleccionada == null || ventaSeleccionada.getEstado() != Venta.EstadoVenta.PENDIENTE) {
            AlertUtils.mostrarAdvertencia("Selección inválida",
                    "Seleccione una venta PENDIENTE");
            return;
        }

        // Diálogo para registrar pago
        Dialog<PagoVenta> dialog = new Dialog<>();
        dialog.setTitle("Registrar Pago");
        dialog.setHeaderText(String.format("Venta: %s\nSaldo pendiente: $%.2f",
                ventaSeleccionada.getNumeroComprobante(),
                ventaSeleccionada.getSaldoPendiente()));

        ButtonType btnGuardar = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<MetodoPago> cbMetodo = new ComboBox<>();
        TextField txtMonto = new TextField();
        TextField txtCuotas = new TextField("1");
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(3);

        // Cargar métodos de pago
        try {
            cbMetodo.setItems(FXCollections.observableArrayList(
                    new MetodoPagoService().listarTodos()
            ));
        } catch (DataAccessException e) {
            logger.error("Error cargando métodos", e);
        }

        grid.add(new Label("Método de pago:"), 0, 0);
        grid.add(cbMetodo, 1, 0);
        grid.add(new Label("Monto:"), 0, 1);
        grid.add(txtMonto, 1, 1);
        grid.add(new Label("Cuotas:"), 0, 2);
        grid.add(txtCuotas, 1, 2);
        grid.add(new Label("Observaciones:"), 0, 3);
        grid.add(txtObs, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                try {
                    PagoVenta pago = new PagoVenta();
                    pago.setMetodoPago(cbMetodo.getValue());
                    pago.setMetodoPagoId(cbMetodo.getValue().getId());
                    pago.setMonto(Double.parseDouble(txtMonto.getText()));
                    pago.setCuotas(Integer.parseInt(txtCuotas.getText()));
                    pago.setObservaciones(txtObs.getText());
                    pago.setComision(cbMetodo.getValue().calcularComision(pago.getMonto()));
                    return pago;
                } catch (Exception e) {
                    Platform.runLater(() ->
                            AlertUtils.mostrarAdvertencia("Datos inválidos",
                                    "Verifique los datos ingresados")
                    );
                }
            }
            return null;
        });

        Optional<PagoVenta> result = dialog.showAndWait();
        result.ifPresent(pago -> {
            ejecutarEnBackground(() -> {
                try {
                    Venta actualizada = ventaService.registrarPago(
                            ventaSeleccionada.getId(), pago
                    );

                    Platform.runLater(() -> {
                        String mensaje;
                        if (actualizada.getEstado() == Venta.EstadoVenta.COMPLETADA) {
                            mensaje = "✅ Pago registrado - Venta COMPLETADA\n\n" +
                                    String.format("Monto: $%.2f\nSaldo: $0.00", pago.getMonto());
                        } else {
                            mensaje = "Pago registrado correctamente\n\n" +
                                    String.format("Monto: $%.2f\nSaldo restante: $%.2f",
                                            pago.getMonto(), actualizada.getSaldoPendiente());
                        }
                        AlertUtils.mostrarExito("Éxito", mensaje);
                        buscarVentas(); // Recargar
                    });

                } catch (BusinessException | DataAccessException | NotFoundException e) {
                    logger.error("Error registrando pago", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", e.getMessage())
                    );
                }
            });
        });
    }
}