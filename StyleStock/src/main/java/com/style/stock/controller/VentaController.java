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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controlador para gestión de ventas
 */
public class VentaController {
    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    // Servicios
    private final VentaService ventaService;
    private final ClienteService clienteService;
    private final VarianteService varianteService;
    private final MetodoPagoService metodoPagoService;
    private final TipoClienteService tipoClienteService;

    // Componentes UI - Datos del cliente
    @FXML private ComboBox<Cliente> cbClientes;
    @FXML private Button btnNuevoCliente;
    @FXML private ComboBox<String> cbTipoVenta; // MINORISTA / MAYORISTA
    @FXML private ComboBox<String> cbTipoComprobante; // TICKET / FACTURA_A / FACTURA_B / FACTURA_C
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
        this.metodoPagoService = new MetodoPagoService();
        this.tipoClienteService = new TipoClienteService();

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

        // Formatear precio y subtotal
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

    private void configurarCombos() {
        cbClientes.setItems(clientes);
        lvVariantes.setItems(variantesDisponibles);
        cbMetodoPago.setItems(metodosPago);

        // Tipos de venta
        cbTipoVenta.setItems(FXCollections.observableArrayList("MINORISTA", "MAYORISTA"));
        cbTipoVenta.setValue("MINORISTA");

        // Tipos de comprobante
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

        // Listener para actualizar totales cuando cambian items
        items.addListener((javafx.collections.ListChangeListener<DetalleVenta>) c -> {
            actualizarTotales();
        });

        // Listener para actualizar saldo cuando cambian pagos
        pagos.addListener((javafx.collections.ListChangeListener<PagoVenta>) c -> {
            actualizarSaldoPago();
        });

        // Búsqueda de productos en tiempo real
        txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> {
            buscarVariantes(newVal);
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

        // Validar CUIT si se selecciona Factura A o B
        cbTipoComprobante.valueProperty().addListener((obs, oldVal, newVal) -> {
            validarRequisitosCuit(newVal);
        });
    }

    private void cargarDatosIniciales() {
        ejecutarEnBackground(() -> {
            try {
                // Cargar clientes
                var listaClientes = clienteService.listarTodos();
                Platform.runLater(() -> clientes.setAll(listaClientes));

                // Cargar métodos de pago
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

    /**
     * Búsqueda de variantes por texto (SKU o descripción del producto)
     * @param termino Término a buscar
     * @return Lista de variantes coincidentes
     */
    @FXML
    private void buscarVariantes(String termino) {
        ejecutarEnBackground(() -> {
            try {
                // Buscar variantes por SKU o descripción
                List<Variante> resultado = varianteService.buscarPorTexto(termino);
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

            // Crear pago
            PagoVenta pago = new PagoVenta();
            pago.setMetodoPago(metodo);
            pago.setMonto(monto);
            pago.setCuotas(cuotas);
            pago.setComision(metodo.calcularComision(monto));

            pagos.add(pago);

            // Agregar a lista visual
            String textoPago = String.format("%s: $%.2f%s", 
                metodo.getNombre(), 
                monto,
                cuotas > 1 ? String.format(" (%d cuotas)", cuotas) : ""
            );
            lvPagos.getItems().add(textoPago);

            // Limpiar campos
            txtMontoPago.clear();
            txtCuotas.setText("1");

        } catch (NumberFormatException e) {
            AlertUtils.mostrarAdvertencia("Datos inválidos", "Ingrese valores válidos");
        }
    }

    // Reemplazar el método completarVenta() en VentaController.java

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

                // CRÍTICO: Setear cliente primero para que se propague el ID
                venta.setCliente(cliente);

                // Asegurar que los IDs estén presentes
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

                // Agregar detalles
                for (DetalleVenta item : items) {
                    venta.agregarDetalle(item);
                }

                // Agregar pagos
                for (PagoVenta pago : pagos) {
                    venta.agregarPago(pago);
                }

                // Log para debug
                logger.debug("Venta a guardar - ClienteId: {}, TipoClienteId: {}, Items: {}, Total: {}",
                        venta.getClienteId(), venta.getTipoClienteId(),
                        venta.getDetalles().size(), venta.getTotal());

                Venta guardada = ventaService.crear(venta);

                Platform.runLater(() -> {
                    AlertUtils.mostrarExito("Éxito",
                            "Venta completada correctamente\nComprobante: " + guardada.getNumeroComprobante());

                    // Preguntar si desea imprimir/generar PDF
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
            } catch (BusinessException | DataAccessException | NotFoundException e) {
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

        // Cambiar color del saldo
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

    private void validarRequisitosCuit(String tipoComprobante) {
        if ("FACTURA_A".equals(tipoComprobante) || "FACTURA_B".equals(tipoComprobante)) {
            Cliente cliente = cbClientes.getValue();
            if (cliente != null && (cliente.getCuit() == null || cliente.getCuit().isEmpty())) {
                AlertUtils.mostrarAdvertencia("CUIT requerido", 
                    "Las facturas A y B requieren que el cliente tenga CUIT registrado");
            }
        }
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
        // TODO: Implementar generación de PDF o impresión
        logger.info("Generando comprobante para venta: {}", venta.getNumeroComprobante());
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
