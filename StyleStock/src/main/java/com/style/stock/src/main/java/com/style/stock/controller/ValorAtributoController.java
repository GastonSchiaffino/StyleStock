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
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Controlador para gestionar valores de atributos
 */
public class ValorAtributoController {
    private static final Logger logger = LoggerFactory.getLogger(ValorAtributoController.class);

    @FXML private Label lblAtributoNombre;
    @FXML private TextField txtValor;
    @FXML private TextField txtCodigoHexField;
    @FXML private ColorPicker colorPickerControl;
    @FXML private TextField txtOrden;
    @FXML private Label lblCodigoHex;

    @FXML private TableView<ValorAtributo> tablaValores;
    @FXML private TableColumn<ValorAtributo, Integer> colId;
    @FXML private TableColumn<ValorAtributo, String> colValor;
    @FXML private TableColumn<ValorAtributo, String> colCodigoHex;
    @FXML private TableColumn<ValorAtributo, Integer> colOrden;
    @FXML private TableColumn<ValorAtributo, Boolean> colActivo;

    @FXML private ProgressIndicator progressIndicator;

    private final ValorAtributoService valorAtributoService;
    private final ObservableList<ValorAtributo> valores;
    private Atributo atributo;
    private ValorAtributo valorSeleccionado;

    public ValorAtributoController() {
        this.valorAtributoService = new ValorAtributoService();
        this.valores = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarEventos();
    }

    /**
     * Método público para inicializar con un atributo específico
     */
    public void setAtributo(Atributo atributo) {
        this.atributo = atributo;
        lblAtributoNombre.setText(atributo.getNombre());

        // Mostrar/ocultar campos según tipo de atributo
        boolean esColor = atributo.getTipo() == Atributo.TipoAtributo.COLOR;
        lblCodigoHex.setVisible(esColor);
        txtCodigoHexField.setVisible(esColor);
        colorPickerControl.setVisible(esColor);

        cargarValores();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colCodigoHex.setCellValueFactory(new PropertyValueFactory<>("codigoHex"));
        colOrden.setCellValueFactory(new PropertyValueFactory<>("orden"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        // Columna con preview de color
        colCodigoHex.setCellFactory(col -> new TableCell<ValorAtributo, String>() {
            @Override
            protected void updateItem(String codigoHex, boolean empty) {
                super.updateItem(codigoHex, empty);
                if (empty || codigoHex == null || codigoHex.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(codigoHex);
                    setStyle("-fx-background-color: " + codigoHex + "; -fx-text-fill: white;");
                }
            }
        });

        tablaValores.setItems(valores);
    }

    private void configurarEventos() {
        tablaValores.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> valorSeleccionado = newVal
        );

        // Validación numérica para orden
        txtOrden.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) txtOrden.setText(o);
        });

        // Sincronizar ColorPicker con TextField
        if (colorPickerControl != null) {
            colorPickerControl.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    String hex = String.format("#%02X%02X%02X",
                            (int) (newVal.getRed() * 255),
                            (int) (newVal.getGreen() * 255),
                            (int) (newVal.getBlue() * 255));
                    txtCodigoHexField.setText(hex);
                }
            });

            // Sincronizar TextField con ColorPicker
            txtCodigoHexField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.matches("#[0-9A-Fa-f]{6}")) {
                    try {
                        colorPickerControl.setValue(Color.web(newVal));
                    } catch (Exception e) {
                        logger.debug("Color inválido: {}", newVal);
                    }
                }
            });
        }
    }

    private void cargarValores() {
        if (atributo == null) return;

        ejecutarEnBackground(() -> {
            try {
                var lista = valorAtributoService.listarPorAtributo(atributo.getId());
                Platform.runLater(() -> {
                    valores.setAll(lista);
                    logger.debug("Cargados {} valores para atributo {}",
                            lista.size(), atributo.getNombre());
                });
            } catch (DataAccessException e) {
                logger.error("Error cargando valores", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudieron cargar los valores")
                );
            }
        });
    }

    @FXML
    private void agregarValor() {
        String valor = txtValor.getText().trim();

        if (valor.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Validación", "El valor es obligatorio");
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                ValorAtributo va = new ValorAtributo();
                va.setAtributoId(atributo.getId());
                va.setValor(valor);

                // Solo guardar código hex si es tipo COLOR
                if (atributo.getTipo() == Atributo.TipoAtributo.COLOR) {
                    String codigoHex = txtCodigoHexField.getText().trim();
                    if (!codigoHex.isEmpty()) {
                        va.setCodigoHex(codigoHex);
                    }
                }

                va.setOrden(Integer.parseInt(txtOrden.getText().trim()));

                valorAtributoService.crear(va);

                Platform.runLater(() -> {
                    AlertUtils.mostrarExito("Éxito", "Valor agregado correctamente");
                    cargarValores();
                    limpiarFormulario();
                });

            } catch (ValidationException e) {
                Platform.runLater(() ->
                        AlertUtils.mostrarAdvertencia("Validación", e.getMessage())
                );
            } catch (DataAccessException e) {
                logger.error("Error guardando valor", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudo guardar el valor")
                );
            }
        });
    }

    @FXML
    private void editarValor() {
        if (valorSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un valor");
            return;
        }

        txtValor.setText(valorSeleccionado.getValor());
        txtOrden.setText(String.valueOf(valorSeleccionado.getOrden()));

        if (atributo.getTipo() == Atributo.TipoAtributo.COLOR &&
                valorSeleccionado.getCodigoHex() != null) {
            txtCodigoHexField.setText(valorSeleccionado.getCodigoHex());
        }
    }

    @FXML
    private void eliminarValor() {
        if (valorSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un valor");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Eliminar el valor?",
                valorSeleccionado.getValor()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    valorAtributoService.eliminar(valorSeleccionado.getId());
                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Valor eliminado");
                        cargarValores();
                        limpiarFormulario();
                    });
                } catch (DataAccessException | NotFoundException e) {
                    logger.error("Error eliminando valor", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", "No se pudo eliminar el valor")
                    );
                }
            });
        }
    }

    @FXML
    private void limpiarFormulario() {
        txtValor.clear();
        txtCodigoHexField.clear();
        txtOrden.setText("0");
        if (colorPickerControl != null) {
            colorPickerControl.setValue(Color.WHITE);
        }
        valorSeleccionado = null;
        tablaValores.getSelectionModel().clearSelection();
    }

    @FXML
    private void cerrar() {
        Stage stage = (Stage) tablaValores.getScene().getWindow();
        stage.close();
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