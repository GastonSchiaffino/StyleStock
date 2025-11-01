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
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Controlador para gestión de ventas - CORREGIDO
 * Incluye búsqueda optimizada y validaciones mejoradas
 */
public class VentaController {
    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    // Servicios
    private final VentaService ventaService;
    private final ClienteService clienteService;
    private final VarianteService varianteService;
    private final ProductoService productoService;
    private final MetodoPagoService metodoPagoService;

    // Componentes UI - Datos del cliente
    @FXML private ComboBox<Cliente> cbClientes;
    @FXML private Button btnNuevoCliente;
    @FXML private ComboBox<String> cbTipoVenta;
    @FXML private ComboBox<String> cbTipoComprobante;
    @FXML private DatePicker dpFecha;
    @FXML private TextArea txtNotas;

    // Componentes UI - Agregar productos
    @FXML private TextField txtBuscarProducto;
    @FXML private ListView<Variante> lvVariantes;
    @FXML private TextField txtCantidad;
    @FXML private Button btnAgregarProducto;

    // Tabla de items en la venta
    @FXML private TableView<DetalleVenta> tablaItems;
    @FXML private TableColumn<DetalleVenta, String> colSKU;
    @FXML private TableColumn<DetalleVenta, String> colDescripcion;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML private TableColumn<DetalleVenta, Double> colPrecio;
    @FXML private TableColumn<DetalleVenta, Double> colSubtotal;
    @FXML private Button btnQuitarItem;

    // Totales
    @FXML private TextField txtDescuento;
    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuento;
    @FXML private Label lblTotal;

    // Pagos
    @FXML private ComboBox<MetodoPago> cbMetodoPago;
    @FXML private TextField txtMontoPago;
    @FXML private TextField txtCuotas;
    @FXML private Button btnAgregarPago;
    @FXML private ListView<String> lvPagos;
    @FXML private Label lblTotalPagado;
    @FXML private Label lblSaldo;

    // Botones de acción
    @FXML private Button btnCompletarVenta;
    @FXML private Button btnCancelar;
    @FXML private ProgressIndicator progressIndicator;

    // Datos en memoria
    private final ObservableList<Cliente> clientes;
    private final ObservableList<Variante> variantesDisponibles;
    private final ObservableList<DetalleVenta> items;
    private final ObservableList<MetodoPago> metodosPago;
    private final ObservableList<PagoVenta> pagos;

    public VentaController() {
        this.ventaService = new VentaService();
        this.clienteService = new ClienteService();
        this.varianteService = new VarianteService();
        this.productoService = new ProductoService();
        this.metodoPagoService = new MetodoPagoService();

        this.clientes = FXCollections.observableArrayList();
        this.variantesDisponibles = FXCollections.observableArrayList();
        this.items = FXCollections.observableArrayList();
        this.metodosPago = FXCollections.observableArrayList();
        this.pagos = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        cargarDatosIniciales();
        configurarListaVariantes();
        nuevaVenta();
    }

    private void configurarTabla() {
        colSKU.setCellValueFactory(cellData -> {
            DetalleVenta detalle = cellData.getValue();
            if (detalle != null && detalle.getVariante() != null) {
                return new SimpleStringProperty(detalle.getVariante().getSku());
            }
            return new SimpleStringProperty("");
        });

        colDescripcion.setCellValueFactory(cellData -> {
            DetalleVenta detalle = cellData.getValue();
            if (detalle != null && detalle.getVariante() != null) {
                return new SimpleStringProperty(detalle.getVariante().getDescripcionCompleta());
            }
            return new SimpleStringProperty("");
        });

        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        colPrecio.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double precio, boolean empty) {
                super.updateItem(precio, empty);
                setText(empty || precio == null ? null : String.format("$%.2f", precio));
            }
        });

        colSubtotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                setText(empty || subtotal == null ? null : String.format("$%.2f", subtotal));
            }
        });

        tablaItems.setItems(items);
    }

    // CORREGIDO: Configurar ComboBoxes con StringConverters
    private void configurarCombos() {
        cbClientes.setItems(clientes);

        // NUEVO: Converter para mostrar correctamente los clientes
        cbClientes.setConverter(new StringConverter<Cliente>() {
            @Override
            public String toString(Cliente cliente) {
                return cliente == null ? "" : cliente.getNombreCompleto();
            }

            @Override
            public Cliente fromString(String string) {
                return clientes.stream()
                        .filter(c -> c.getNombreCompleto().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        // NUEVO: Hacer el ComboBox de clientes editable para búsqueda
        cbClientes.setEditable(true);

        lvVariantes.setItems(variantesDisponibles);

        cbMetodoPago.setItems(metodosPago);

        // NUEVO: Converter para métodos de pago
        cbMetodoPago.setConverter(new StringConverter<MetodoPago>() {
            @Override
            public String toString(MetodoPago metodo) {
                return metodo == null ? "" : metodo.getNombre();
            }

            @Override
            public MetodoPago fromString(String string) {
                return metodosPago.stream()
                        .filter(m -> m.getNombre().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        cbTipoVenta.setItems(FXCollections.observableArrayList("MINORISTA", "MAYORISTA"));
        cbTipoVenta.setValue("MINORISTA");

        cbTipoComprobante.setItems(FXCollections.observableArrayList(
                "TICKET", "FACTURA_A", "FACTURA_B", "FACTURA_C"
        ));
        cbTipoComprobante.setValue("TICKET");

        dpFecha.setValue(LocalDate.now());
    }

    private void configurarEventos() {
        // Validación de cantidad
        txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtCantidad.setText(oldVal);
            }
        });

        // Validación de descuento
        txtDescuento.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                txtDescuento.setText(oldVal);
            }
            actualizarTotales();
        });

        // Validación de monto de pago
        txtMontoPago.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                txtMontoPago.setText(oldVal);
            }
        });

        // Validación de cuotas
        txtCuotas.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtCuotas.setText(oldVal);
            }
        });

        items.addListener((javafx.collections.ListChangeListener<DetalleVenta>) c -> {
            actualizarTotales();
        });

        pagos.addListener((javafx.collections.ListChangeListener<PagoVenta>) c -> {
            actualizarSaldoPago();
        });

        // CORREGIDO: Búsqueda en tiempo real al escribir
        txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                buscarVariantes(newVal);
            } else {
                variantesDisponibles.clear();
            }
        });

        // Al cambiar tipo de venta, actualizar precios
        cbTipoVenta.valueProperty().addListener((obs, oldVal, newVal) -> {
            actualizarPreciosItems();
        });

        // Habilitar/deshabilitar cuotas según método de pago
        cbMetodoPago.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtCuotas.setDisable(!newVal.getRequiereCuotas());
                if (!newVal.getRequiereCuotas()) {
                    txtCuotas.setText("1");
                }
            }
        });

        // MEJORADO: Validar CUIT antes de cambiar comprobante
        cbTipoComprobante.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !validarRequisitosCuit(newVal)) {
                Platform.runLater(() -> cbTipoComprobante.setValue(oldVal));
            }
        });
    }

    private void configurarListaVariantes() {
        lvVariantes.setCellFactory(lv -> new ListCell<Variante>() {
            @Override
            protected void updateItem(Variante variante, boolean empty) {
                super.updateItem(variante, empty);

                if (empty || variante == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    VBox container = new VBox(5);
                    container.setStyle("-fx-padding: 8; -fx-background-radius: 5;");

                    // Línea 1: SKU + Producto
                    HBox linea1 = new HBox(10);
                    Label lblSku = new Label(variante.getSku());
                    lblSku.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                    Label lblProducto = new Label(variante.getProducto() != null ?
                            variante.getProducto().getNombre() : "");
                    lblProducto.setStyle("-fx-text-fill: #666;");

                    linea1.getChildren().addAll(lblSku, new Label("-"), lblProducto);

                    // Línea 2: Atributos
                    HBox linea2 = new HBox(8);
                    if (variante.getAtributos() != null && !variante.getAtributos().isEmpty()) {
                        for (VarianteAtributo va : variante.getAtributos()) {
                            Label lblAttr = new Label(va.getValor());
                            lblAttr.setStyle(
                                    "-fx-background-color: #E3F2FD; " +
                                            "-fx-padding: 2 8; " +
                                            "-fx-background-radius: 10; " +
                                            "-fx-font-size: 11px; " +
                                            "-fx-text-fill: #1976D2;"
                            );
                            linea2.getChildren().add(lblAttr);
                        }
                    }

                    // Línea 3: Precios y Stock
                    HBox linea3 = new HBox(15);

                    Label lblPrecio = new Label(String.format("💰 $%.2f",
                            variante.getPrecioMinorista()));
                    lblPrecio.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");

                    Label lblStock = new Label(String.format("📦 Stock: %d",
                            variante.getStock()));
                    lblStock.setStyle(variante.isStockBajo() ?
                            "-fx-text-fill: #D32F2F; -fx-font-weight: bold;" :
                            "-fx-text-fill: #666;");

                    linea3.getChildren().addAll(lblPrecio, new Separator(Orientation.VERTICAL), lblStock);

                    container.getChildren().addAll(linea1, linea2, linea3);

                    if (variante.isStockBajo()) {
                        container.setStyle(
                                "-fx-padding: 8; " +
                                        "-fx-background-color: #FFEBEE; " +
                                        "-fx-border-color: #F44336; " +
                                        "-fx-border-width: 1; " +
                                        "-fx-background-radius: 5; " +
                                        "-fx-border-radius: 5;"
                        );
                    }

                    setGraphic(container);
                    setText(null);
                }
            }
        });
    }

    private void cargarDatosIniciales() {
        ejecutarEnBackground(() -> {
            try {
                var listaClientes = clienteService.listarTodos();
                Platform.runLater(() -> clientes.setAll(listaClientes));

                var listaMetodos = metodoPagoService.listarTodos();
                Platform.runLater(() -> metodosPago.setAll(listaMetodos));

            } catch (DataAccessException e) {
                logger.error("Error cargando datos iniciales", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudieron cargar los datos iniciales")
                );
            }
        });
    }

    // CORREGIDO: Cargar productos completos para cada variante
    @FXML
    private void buscarVariantes(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            variantesDisponibles.clear();
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                var resultado = varianteService.buscarPorTexto(termino);

                // CRÍTICO: Cargar el producto completo para cada variante
                for (Variante v : resultado) {
                    if (v.getProductoId() != null && v.getProducto() == null) {
                        try {
                            Producto producto = productoService.buscarPorId(v.getProductoId());
                            v.setProducto(producto);
                        } catch (NotFoundException e) {
                            logger.warn("Producto no encontrado para variante: {}", v.getSku());
                        }
                    }
                }

                Platform.runLater(() -> variantesDisponibles.setAll(resultado));

            } catch (DataAccessException e) {
                logger.error("Error buscando variantes", e);
            }
        });
    }

    @FXML
    private void agregarProducto() {
        Variante variante = lvVariantes.getSelectionModel().getSelectedItem();
        if (variante == null) {
            AlertUtils.mostrarAdvertencia("Producto requerido", "Seleccione un producto");
            return;
        }

        try {
            int cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) {
                AlertUtils.mostrarAdvertencia("Cantidad inválida", "La cantidad debe ser mayor a 0");
                return;
            }

            // Verificar stock
            if (variante.getStock() < cantidad) {
                AlertUtils.mostrarAdvertencia("Stock insuficiente",
                        String.format("Stock disponible: %d unidades", variante.getStock()));
                return;
            }

            // Determinar precio según tipo de venta
            boolean esMayorista = "MAYORISTA".equals(cbTipoVenta.getValue());
            double precio = esMayorista ? variante.getPrecioMayorista() : variante.getPrecioMinorista();
            String precioTipo = esMayorista ? "MAYORISTA" : "MINORISTA";

            // Crear detalle
            DetalleVenta detalle = new DetalleVenta(variante, cantidad, precio, precioTipo);
            items.add(detalle);

            // Limpiar selección
            txtCantidad.clear();
            txtBuscarProducto.clear();

            logger.debug("Item agregado: {} x {}", variante.getSku(), cantidad);

        } catch (NumberFormatException e) {
            AlertUtils.mostrarAdvertencia("Cantidad inválida", "Ingrese una cantidad válida");
        }
    }

    @FXML
    private void quitarItem() {
        DetalleVenta seleccionado = tablaItems.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un ítem para quitar");
            return;
        }

        items.remove(seleccionado);
    }

    @FXML
    private void agregarPago() {
        MetodoPago metodo = cbMetodoPago.getValue();
        if (metodo == null) {
            AlertUtils.mostrarAdvertencia("Método requerido", "Seleccione un método de pago");
            return;
        }

        try {
            double monto = Double.parseDouble(txtMontoPago.getText().trim());
            if (monto <= 0) {
                AlertUtils.mostrarAdvertencia("Monto inválido", "El monto debe ser mayor a 0");
                return;
            }

            int cuotas = 1;
            if (metodo.getRequiereCuotas() && !txtCuotas.getText().trim().isEmpty()) {
                cuotas = Integer.parseInt(txtCuotas.getText().trim());
            }

            PagoVenta pago = new PagoVenta();
            pago.setMetodoPago(metodo);
            pago.setMonto(monto);
            pago.setCuotas(cuotas);
            pago.setComision(metodo.calcularComision(monto));

            pagos.add(pago);

            String textoPago = String.format("%s: $%.2f%s",
                    metodo.getNombre(),
                    monto,
                    cuotas > 1 ? String.format(" (%d cuotas)", cuotas) : ""
            );
            lvPagos.getItems().add(textoPago);

            txtMontoPago.clear();
            txtCuotas.setText("1");

        } catch (NumberFormatException e) {
            AlertUtils.mostrarAdvertencia("Datos inválidos", "Ingrese valores válidos");
        }
    }

    @FXML
    private void completarVenta() {
        Cliente cliente = cbClientes.getValue();
        if (cliente == null) {
            AlertUtils.mostrarAdvertencia("Cliente requerido", "Seleccione un cliente");
            return;
        }

        if (items.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Items requeridos", "Agregue al menos un producto a la venta");
            return;
        }

        if (pagos.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Pago requerido", "Agregue al menos un método de pago");
            return;
        }

        // Verificar que el pago cubra el total
        double totalPagado = pagos.stream().mapToDouble(PagoVenta::getMonto).sum();
        double total = calcularTotal();

        if (totalPagado < total) {
            AlertUtils.mostrarAdvertencia("Pago insuficiente",
                    String.format("Total: $%.2f - Pagado: $%.2f - Falta: $%.2f",
                            total, totalPagado, total - totalPagado));
            return;
        }

        // Confirmar
        Optional<ButtonType> confirmacion = AlertUtils.mostrarConfirmacion(
                "Completar Venta",
                "¿Confirma que desea completar esta venta?",
                String.format("Total: $%.2f\nPagado: $%.2f", total, totalPagado)
        );

        if (confirmacion.isEmpty() || confirmacion.get() != ButtonType.OK) {
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                Venta venta = new Venta();

                venta.setCliente(cliente);
                venta.setClienteId(cliente.getId());
                venta.setTipoClienteId(cliente.getTipoClienteId());

                venta.setFecha(dpFecha.getValue());
                venta.setTipoComprobante(Venta.TipoComprobante.valueOf(cbTipoComprobante.getValue()));
                venta.setTipoVenta(Venta.TipoVenta.valueOf(cbTipoVenta.getValue()));
                venta.setNotas(txtNotas.getText());

                double descuento = 0.0;
                if (!txtDescuento.getText().trim().isEmpty()) {
                    descuento = Double.parseDouble(txtDescuento.getText().trim());
                }
                venta.setDescuento(descuento);

                for (DetalleVenta item : items) {
                    venta.agregarDetalle(item);
                }

                for (PagoVenta pago : pagos) {
                    venta.agregarPago(pago);
                }

                logger.debug("Venta a guardar - ClienteId: {}, TipoClienteId: {}, Items: {}, Total: {}",
                        venta.getClienteId(), venta.getTipoClienteId(),
                        venta.getDetalles().size(), venta.getTotal());

                Venta guardada = ventaService.crear(venta);

                Platform.runLater(() -> {
                    AlertUtils.mostrarExito("Éxito",
                            "Venta completada correctamente\nComprobante: " + guardada.getNumeroComprobante());

                    Optional<ButtonType> imprimir = AlertUtils.mostrarConfirmacion(
                            "Imprimir",
                            "¿Desea imprimir el comprobante?"
                    );

                    if (imprimir.isPresent() && imprimir.get() == ButtonType.OK) {
                        imprimirComprobante(guardada);
                    }

                    nuevaVenta();
                });

            } catch (InsufficientStockException e) {
                logger.warn("Stock insuficiente", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarAdvertencia("Stock Insuficiente", e.getMessage())
                );
            } catch (ValidationException e) {
                logger.error("Error de validación", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarAdvertencia("Validación", e.getMessage())
                );
            } catch (Exception e) {
                logger.error("Error completando venta", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudo completar la venta", e.getMessage())
                );
            }
        });
    }

    @FXML
    private void nuevaVenta() {
        cbClientes.setValue(null);
        cbTipoVenta.setValue("MINORISTA");
        cbTipoComprobante.setValue("TICKET");
        dpFecha.setValue(LocalDate.now());
        txtDescuento.clear();
        txtNotas.clear();
        txtBuscarProducto.clear();
        txtCantidad.clear();
        txtMontoPago.clear();
        txtCuotas.setText("1");

        items.clear();
        pagos.clear();
        lvPagos.getItems().clear();
        variantesDisponibles.clear();

        actualizarTotales();
        actualizarSaldoPago();
    }

    private void actualizarTotales() {
        double subtotal = items.stream().mapToDouble(DetalleVenta::getSubtotal).sum();
        double descuento = 0.0;

        try {
            if (!txtDescuento.getText().trim().isEmpty()) {
                descuento = Double.parseDouble(txtDescuento.getText().trim());
            }
        } catch (NumberFormatException e) {
            // Ignorar
        }

        double total = subtotal - descuento;

        lblSubtotal.setText(String.format("$%.2f", subtotal));
        lblDescuento.setText(String.format("$%.2f", descuento));
        lblTotal.setText(String.format("$%.2f", total));

        actualizarSaldoPago();
    }

    private void actualizarSaldoPago() {
        double totalPagado = pagos.stream().mapToDouble(PagoVenta::getMonto).sum();
        double total = calcularTotal();
        double saldo = total - totalPagado;

        lblTotalPagado.setText(String.format("$%.2f", totalPagado));
        lblSaldo.setText(String.format("$%.2f", saldo));

        if (saldo <= 0) {
            lblSaldo.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            btnCompletarVenta.setDisable(false);
        } else {
            lblSaldo.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            btnCompletarVenta.setDisable(true);
        }
    }

    private void actualizarPreciosItems() {
        boolean esMayorista = "MAYORISTA".equals(cbTipoVenta.getValue());

        for (DetalleVenta detalle : items) {
            double nuevoPrecio = esMayorista
                    ? detalle.getVariante().getPrecioMayorista()
                    : detalle.getVariante().getPrecioMinorista();

            detalle.setPrecioUnitario(nuevoPrecio);
            detalle.setPrecioTipo(esMayorista ? "MAYORISTA" : "MINORISTA");
            detalle.calcularSubtotal();
        }

        tablaItems.refresh();
        actualizarTotales();
    }

    // MEJORADO: Validación CUIT con bloqueo
    private boolean validarRequisitosCuit(String tipoComprobante) {
        if ("FACTURA_A".equals(tipoComprobante) || "FACTURA_B".equals(tipoComprobante)) {
            Cliente cliente = cbClientes.getValue();
            if (cliente == null) {
                AlertUtils.mostrarAdvertencia("Cliente requerido",
                        "Debe seleccionar un cliente antes de elegir tipo de comprobante");
                return false;
            }

            if (cliente.getCuit() == null || cliente.getCuit().trim().isEmpty()) {
                AlertUtils.mostrarAdvertencia("CUIT requerido",
                        "Las facturas A y B requieren que el cliente tenga CUIT registrado.\n\n" +
                                "Por favor:\n" +
                                "1. Cancele esta venta\n" +
                                "2. Agregue el CUIT al cliente\n" +
                                "3. Intente nuevamente");
                return false;
            }
        }
        return true;
    }

    private double calcularTotal() {
        double subtotal = items.stream().mapToDouble(DetalleVenta::getSubtotal).sum();
        double descuento = 0.0;
        try {
            if (!txtDescuento.getText().trim().isEmpty()) {
                descuento = Double.parseDouble(txtDescuento.getText().trim());
            }
        } catch (NumberFormatException e) {
            // Ignorar
        }
        return subtotal - descuento;
    }

    private void imprimirComprobante(Venta venta) {
        logger.info("Generando comprobante para venta: {}", venta.getNumeroComprobante());
        AlertUtils.mostrarInfo("Imprimir",
                "Funcionalidad de impresión en desarrollo.\n" +
                        "Comprobante: " + venta.getNumeroComprobante());
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