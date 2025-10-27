package com.style.stock.controller;

import com.style.stock.exception.*;
import com.style.stock.model.*;
import com.style.stock.service.*;
import com.style.stock.util.AlertUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Controlador mejorado para gestión de facturas
 */
public class FacturaController {
    private static final Logger logger = LoggerFactory.getLogger(FacturaController.class);

    @FXML private ComboBox<Cliente> cbClientes;
    @FXML private ComboBox<Producto> cbProductos;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtDescuento;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cbTipoFactura;
    @FXML private TextArea txtNotas;

    @FXML private TableView<DetalleFactura> tablaItems;
    @FXML private TableColumn<DetalleFactura, String> colCodigo;
    @FXML private TableColumn<DetalleFactura, String> colDescripcion;
    @FXML private TableColumn<DetalleFactura, Double> colPrecio;
    @FXML private TableColumn<DetalleFactura, Integer> colCantidad;
    @FXML private TableColumn<DetalleFactura, Double> colSubtotal;

    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuento;
    @FXML private Label lblTotal;

    @FXML private Button btnAgregar;
    @FXML private Button btnEliminarItem;
    @FXML private Button btnGuardar;
    @FXML private Button btnPDF;
    @FXML private Button btnNuevo;

    @FXML private ProgressIndicator progressIndicator;

    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final FacturaService facturaService;
    private final PDFService pdfService;

    private final ObservableList<Producto> productos;
    private final ObservableList<Cliente> clientes;
    private final ObservableList<DetalleFactura> items;

    private Factura facturaActual;

    public FacturaController() {
        this.productoService = new ProductoService();
        this.clienteService = new ClienteService();
        this.facturaService = new FacturaService();
        this.pdfService = new PDFService();
        
        this.productos = FXCollections.observableArrayList();
        this.clientes = FXCollections.observableArrayList();
        this.items = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        configurarEventos();
        cargarDatos();
        nuevaFactura();
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProducto().getCodigo()));
        colDescripcion.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProducto().getDescripcion()));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
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
        cbProductos.setItems(productos);
        
        cbTipoFactura.setItems(FXCollections.observableArrayList("A", "B", "C"));
        cbTipoFactura.setValue("A");

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
        });

        // Actualizar precio al seleccionar producto
        cbProductos.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtCantidad.setText("1");
            }
        });

        // Listener para actualizar totales cuando cambian items
        items.addListener((javafx.collections.ListChangeListener<DetalleFactura>) c -> {
            actualizarTotales();
        });
    }

    private void cargarDatos() {
        ejecutarEnBackground(() -> {
            try {
                var listaProductos = productoService.listarTodos();
                var listaClientes = clienteService.listarTodos();

                Platform.runLater(() -> {
                    productos.setAll(listaProductos);
                    clientes.setAll(listaClientes);
                });

            } catch (DataAccessException e) {
                logger.error("Error cargando datos", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudieron cargar los datos")
                );
            }
        });
    }

    @FXML
    private void agregarItem() {
        Producto producto = cbProductos.getValue();
        if (producto == null) {
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
            if (producto.getStock() < cantidad) {
                AlertUtils.mostrarAdvertencia("Stock insuficiente", 
                    String.format("Stock disponible: %d unidades", producto.getStock()));
                return;
            }

            // Crear detalle
            DetalleFactura detalle = new DetalleFactura(producto, cantidad, producto.getPrecio());
            items.add(detalle);

            // Limpiar selección
            cbProductos.setValue(null);
            txtCantidad.clear();

            logger.debug("Item agregado: {} x {}", producto.getDescripcion(), cantidad);

        } catch (NumberFormatException e) {
            AlertUtils.mostrarAdvertencia("Cantidad inválida", "Ingrese una cantidad válida");
        }
    }

    @FXML
    private void eliminarItem() {
        DetalleFactura seleccionado = tablaItems.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un ítem para eliminar");
            return;
        }

        items.remove(seleccionado);
    }

    @FXML
    private void guardarFactura() {
        Cliente cliente = cbClientes.getValue();
        if (cliente == null) {
            AlertUtils.mostrarAdvertencia("Cliente requerido", "Seleccione un cliente");
            return;
        }

        if (items.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Items requeridos", "Agregue al menos un ítem a la factura");
            return;
        }

        // Confirmar
        Optional<ButtonType> confirmacion = AlertUtils.mostrarConfirmacion(
            "Guardar Factura",
            "¿Confirma que desea guardar esta factura?",
            String.format("Total: $%.2f", calcularTotal())
        );

        if (confirmacion.isEmpty() || confirmacion.get() != ButtonType.OK) {
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                Factura factura = new Factura();
                factura.setClienteId(cliente.getId());
                factura.setFecha(dpFecha.getValue());
                factura.setTipo(Factura.TipoFactura.valueOf(cbTipoFactura.getValue()));
                factura.setNotas(txtNotas.getText());
                
                double descuento = 0.0;
                if (!txtDescuento.getText().trim().isEmpty()) {
                    descuento = Double.parseDouble(txtDescuento.getText().trim());
                }
                factura.setDescuento(descuento);

                // Agregar detalles
                for (DetalleFactura item : items) {
                    factura.agregarDetalle(item);
                }

                Factura guardada = facturaService.crear(factura);

                Platform.runLater(() -> {
                    AlertUtils.mostrarExito("Éxito", 
                        "Factura guardada correctamente\nNúmero: " + guardada.getNumeroFactura());
                    
                    // Preguntar si desea generar PDF
                    Optional<ButtonType> generarPdf = AlertUtils.mostrarConfirmacion(
                        "Generar PDF",
                        "¿Desea generar el PDF de la factura?"
                    );

                    if (generarPdf.isPresent() && generarPdf.get() == ButtonType.OK) {
                        generarPDF(guardada);
                    }

                    nuevaFactura();
                    cargarDatos(); // Recargar para actualizar stock
                });

            } catch (InsufficientStockException e) {
                logger.warn("Stock insuficiente", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarAdvertencia("Stock Insuficiente", e.getMessage())
                );
            } catch (ValidationException e) {
                Platform.runLater(() -> 
                    AlertUtils.mostrarAdvertencia("Validación", e.getMessage())
                );
            } catch (BusinessException | DataAccessException e) {
                logger.error("Error guardando factura", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudo guardar la factura", e.getMessage())
                );
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @FXML
    private void generarPDFActual() {
        Cliente cliente = cbClientes.getValue();
        if (cliente == null || items.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Datos incompletos", 
                "Complete los datos de la factura antes de generar el PDF");
            return;
        }

        // Crear factura temporal para preview
        Factura facturaTemp = new Factura();
        facturaTemp.setCliente(cliente);
        facturaTemp.setFecha(dpFecha.getValue());
        facturaTemp.setTipo(Factura.TipoFactura.valueOf(cbTipoFactura.getValue()));
        facturaTemp.setNotas(txtNotas.getText());
        
        double descuento = 0.0;
        if (!txtDescuento.getText().trim().isEmpty()) {
            descuento = Double.parseDouble(txtDescuento.getText().trim());
        }
        facturaTemp.setDescuento(descuento);
        
        for (DetalleFactura item : items) {
            facturaTemp.agregarDetalle(item);
        }

        generarPDF(facturaTemp);
    }

    private void generarPDF(Factura factura) {
        ejecutarEnBackground(() -> {
            try {
                String rutaPdf = pdfService.generarFacturaPDF(factura);
                Platform.runLater(() -> {
                    AlertUtils.mostrarExito("PDF Generado", 
                        "El PDF se generó correctamente en:\n" + rutaPdf);
                });
            } catch (Exception e) {
                logger.error("Error generando PDF", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudo generar el PDF", e.getMessage())
                );
            }
        });
    }

    @FXML
    private void nuevaFactura() {
        cbClientes.setValue(null);
        cbProductos.setValue(null);
        cbTipoFactura.setValue("A");
        dpFecha.setValue(LocalDate.now());
        txtCantidad.clear();
        txtDescuento.clear();
        txtNotas.clear();
        items.clear();
        actualizarTotales();
    }

    private void actualizarTotales() {
        double subtotal = items.stream().mapToDouble(DetalleFactura::getSubtotal).sum();
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
    }

    private double calcularTotal() {
        double subtotal = items.stream().mapToDouble(DetalleFactura::getSubtotal).sum();
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