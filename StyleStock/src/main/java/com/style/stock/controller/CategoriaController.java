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
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Controlador COMPLETO Y MEJORADO para gestión de:
 * - Categorías
 * - Atributos
 * - Valores de Atributos (integrado en la misma vista)
 * - Asignación Categoría-Atributos
 */
public class CategoriaController {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaController.class);

    // Servicios
    private final CategoriaService categoriaService;
    private final AtributoService atributoService;
    private final ValorAtributoService valorAtributoService;

    // ========== TAB 1: CATEGORÍAS ==========
    @FXML private TextField txtNombreCategoria;
    @FXML private TextField txtDescripcionCategoria;
    @FXML private CheckBox chkRequiereVariantes;
    @FXML private TableView<Categoria> tablaCategorias;
    //@FXML private TableColumn<Categoria, Integer> colIdCategoria;
    @FXML private TableColumn<Categoria, String> colNombreCategoria;
    @FXML private TableColumn<Categoria, Boolean> colRequiereVar;

    // ========== TAB 2: ATRIBUTOS ==========
    @FXML private TextField txtNombreAtributo;
    @FXML private ComboBox<String> cbTipoAtributo;
    @FXML private TextField txtOrdenAtributo;
    @FXML private TableView<Atributo> tablaAtributos;
    @FXML private TableColumn<Atributo, Integer> colIdAtributo;
    @FXML private TableColumn<Atributo, String> colNombreAtributo;
    @FXML private TableColumn<Atributo, String> colTipoAtributo;
    @FXML private TableColumn<Atributo, Integer> colOrdenAtributo;

    // ========== TAB 2: VALORES DE ATRIBUTOS (NUEVO) ==========
    @FXML private Label lblAtributoSeleccionado;
    @FXML private TextField txtValor;
    @FXML private TextField txtCodigoHexField;
    @FXML private ColorPicker colorPickerControl;
    @FXML private TextField txtOrdenValor;
    @FXML private Label lblCodigoHex;
    @FXML private HBox hboxColor;
    @FXML private TableView<ValorAtributo> tablaValores;
    @FXML private TableColumn<ValorAtributo, Integer> colIdValor;
    @FXML private TableColumn<ValorAtributo, String> colValor;
    @FXML private TableColumn<ValorAtributo, String> colCodigoHexValor;
    @FXML private TableColumn<ValorAtributo, Integer> colOrdenValor;
    @FXML private TableColumn<ValorAtributo, Boolean> colActivoValor;

    // ========== TAB 3: ASIGNACIÓN ==========
    @FXML private ComboBox<Categoria> cbCategoriaAsignar;
    @FXML private ComboBox<Atributo> cbAtributoAsignar;
    @FXML private CheckBox chkRequerido;
    @FXML private TextField txtOrdenAsignacion;
    @FXML private ListView<AtributoAsignado> lvAtributosAsignados;

    @FXML private ProgressIndicator progressIndicator;

    // Datos
    private final ObservableList<Categoria> categorias;
    private final ObservableList<Atributo> atributos;
    private final ObservableList<ValorAtributo> valoresAtributo;
    private final ObservableList<AtributoAsignado> atributosAsignados;

    private Categoria categoriaSeleccionada;
    private Atributo atributoSeleccionado;
    private ValorAtributo valorSeleccionado;

    public CategoriaController() {
        this.categoriaService = new CategoriaService();
        this.atributoService = new AtributoService();
        this.valorAtributoService = new ValorAtributoService();
        this.categorias = FXCollections.observableArrayList();
        this.atributos = FXCollections.observableArrayList();
        this.valoresAtributo = FXCollections.observableArrayList();
        this.atributosAsignados = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTablas();
        configurarEventos();
        cargarDatos();
    }

    private void configurarTablas() {
        // ========== TABLA CATEGORÍAS ==========
        //colIdCategoria.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombreCategoria.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRequiereVar.setCellValueFactory(new PropertyValueFactory<>("requiereVariantes"));
        tablaCategorias.setItems(categorias);

        // ========== TABLA ATRIBUTOS ==========
        colIdAtributo.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombreAtributo.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTipoAtributo.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTipo().getValor()
                )
        );
        colOrdenAtributo.setCellValueFactory(new PropertyValueFactory<>("orden"));
        tablaAtributos.setItems(atributos);

        // ========== TABLA VALORES (NUEVO) ==========
        colIdValor.setCellValueFactory(new PropertyValueFactory<>("id"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colCodigoHexValor.setCellValueFactory(new PropertyValueFactory<>("codigoHex"));
        colOrdenValor.setCellValueFactory(new PropertyValueFactory<>("orden"));
        colActivoValor.setCellValueFactory(new PropertyValueFactory<>("activo"));

        // Columna con preview de color
        colCodigoHexValor.setCellFactory(col -> new TableCell<ValorAtributo, String>() {
            @Override
            protected void updateItem(String codigoHex, boolean empty) {
                super.updateItem(codigoHex, empty);
                if (empty || codigoHex == null || codigoHex.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(codigoHex);
                    setStyle("-fx-background-color: " + codigoHex + "; -fx-text-fill: white; -fx-font-weight: bold;");
                }
            }
        });

        tablaValores.setItems(valoresAtributo);

        // ========== LISTVIEW ASIGNACIÓN ==========
        lvAtributosAsignados.setItems(atributosAsignados);
        lvAtributosAsignados.setCellFactory(lv -> new ListCell<AtributoAsignado>() {
            @Override
            protected void updateItem(AtributoAsignado item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s %s (Orden: %d)",
                            item.atributo.getNombre(),
                            item.atributo.getTipo().getValor(),
                            item.requerido ? "[REQUERIDO]" : "[OPCIONAL]",
                            item.orden
                    ));
                }
            }
        });
    }

    private void configurarEventos() {
        // Selección de categoría
        tablaCategorias.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    categoriaSeleccionada = newVal;
                }
        );

        // Selección de atributo
        tablaAtributos.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    atributoSeleccionado = newVal;
                    if (newVal != null) {
                        cargarValoresAtributo(newVal);
                        actualizarVisibilidadCamposColor(newVal.getTipo());
                    } else {
                        valoresAtributo.clear();
                        lblAtributoSeleccionado.setText("Seleccione un atributo de tipo LISTA o COLOR");
                    }
                }
        );

        // Selección de valor
        tablaValores.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> valorSeleccionado = newVal
        );

        // Validación numérica
        txtOrdenAtributo.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) txtOrdenAtributo.setText(o);
        });

        txtOrdenValor.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) txtOrdenValor.setText(o);
        });

        if (txtOrdenAsignacion != null) {
            txtOrdenAsignacion.textProperty().addListener((obs, o, n) -> {
                if (!n.matches("\\d*")) txtOrdenAsignacion.setText(o);
            });
        }

        // Sincronizar ColorPicker con TextField
        if (colorPickerControl != null && txtCodigoHexField != null) {
            colorPickerControl.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    String hex = String.format("#%02X%02X%02X",
                            (int) (newVal.getRed() * 255),
                            (int) (newVal.getGreen() * 255),
                            (int) (newVal.getBlue() * 255));
                    txtCodigoHexField.setText(hex);
                }
            });

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

    private void cargarDatos() {
        ejecutarEnBackground(() -> {
            try {
                var listaCategorias = categoriaService.listarTodas();
                var listaAtributos = atributoService.listarTodos();

                Platform.runLater(() -> {
                    categorias.setAll(listaCategorias);
                    atributos.setAll(listaAtributos);
                    cbCategoriaAsignar.setItems(categorias);
                    cbAtributoAsignar.setItems(atributos);
                });

            } catch (DataAccessException e) {
                logger.error("Error cargando datos", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudieron cargar los datos")
                );
            }
        });
    }

    // ============================================
    // CATEGORÍAS
    // ============================================

    @FXML
    private void nuevaCategoria() {
        txtNombreCategoria.clear();
        txtDescripcionCategoria.clear();
        chkRequiereVariantes.setSelected(true);
        categoriaSeleccionada = null;
        txtNombreCategoria.requestFocus();
    }

    @FXML
    private void guardarCategoria() {
        String nombre = txtNombreCategoria.getText().trim();
        if (nombre.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Validación", "El nombre es obligatorio");
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                Categoria categoria = categoriaSeleccionada != null ?
                        categoriaSeleccionada : new Categoria();

                categoria.setNombre(nombre);
                categoria.setDescripcion(txtDescripcionCategoria.getText().trim());
                categoria.setRequiereVariantes(chkRequiereVariantes.isSelected());

                if (categoriaSeleccionada == null) {
                    categoriaService.crear(categoria);
                    Platform.runLater(() ->
                            AlertUtils.mostrarExito("Éxito", "Categoría creada correctamente")
                    );
                } else {
                    categoriaService.actualizar(categoria);
                    Platform.runLater(() ->
                            AlertUtils.mostrarExito("Éxito", "Categoría actualizada correctamente")
                    );
                }

                cargarDatos();
                Platform.runLater(this::nuevaCategoria);

            } catch (ValidationException e) {
                Platform.runLater(() ->
                        AlertUtils.mostrarAdvertencia("Validación", e.getMessage())
                );
            } catch (DataAccessException | NotFoundException e) {
                logger.error("Error guardando categoría", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudo guardar la categoría")
                );
            }
        });
    }

    @FXML
    private void editarCategoria() {
        if (categoriaSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una categoría");
            return;
        }

        txtNombreCategoria.setText(categoriaSeleccionada.getNombre());
        txtDescripcionCategoria.setText(categoriaSeleccionada.getDescripcion());
        chkRequiereVariantes.setSelected(categoriaSeleccionada.getRequiereVariantes());
        txtNombreCategoria.requestFocus();
    }

    @FXML
    private void eliminarCategoria() {
        if (categoriaSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una categoría");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Está seguro de eliminar la categoría?",
                categoriaSeleccionada.getNombre() + "\n\n⚠️ Esto puede afectar productos asociados"
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    categoriaService.eliminar(categoriaSeleccionada.getId());
                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Categoría eliminada");
                        cargarDatos();
                        nuevaCategoria();
                    });
                } catch (BusinessException e) {
                    Platform.runLater(() ->
                            AlertUtils.mostrarAdvertencia("No se puede eliminar", e.getMessage())
                    );
                } catch (DataAccessException | NotFoundException e) {
                    logger.error("Error eliminando categoría", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", "No se pudo eliminar la categoría")
                    );
                }
            });
        }
    }

    @FXML
    private void cancelarCategoria() {
        nuevaCategoria();
    }

    // ============================================
    // ATRIBUTOS
    // ============================================

    @FXML
    private void nuevoAtributo() {
        txtNombreAtributo.clear();
        cbTipoAtributo.setValue("LISTA");
        txtOrdenAtributo.setText("0");
        atributoSeleccionado = null;
        valoresAtributo.clear();
        lblAtributoSeleccionado.setText("Seleccione un atributo de tipo LISTA o COLOR");
        txtNombreAtributo.requestFocus();
    }

    @FXML
    private void guardarAtributo() {
        String nombre = txtNombreAtributo.getText().trim();
        String tipo = cbTipoAtributo.getValue();

        if (nombre.isEmpty() || tipo == null) {
            AlertUtils.mostrarAdvertencia("Validación", "Complete todos los campos");
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                Atributo atributo = atributoSeleccionado != null ?
                        atributoSeleccionado : new Atributo();

                atributo.setNombre(nombre);
                atributo.setTipo(Atributo.TipoAtributo.valueOf(tipo));
                atributo.setOrden(Integer.parseInt(txtOrdenAtributo.getText().trim()));

                if (atributoSeleccionado == null) {
                    atributoService.crear(atributo);
                    Platform.runLater(() ->
                            AlertUtils.mostrarExito("Éxito", "Atributo creado correctamente")
                    );
                } else {
                    atributoService.actualizar(atributo);
                    Platform.runLater(() ->
                            AlertUtils.mostrarExito("Éxito", "Atributo actualizado")
                    );
                }

                cargarDatos();
                Platform.runLater(this::nuevoAtributo);

            } catch (ValidationException e) {
                Platform.runLater(() ->
                        AlertUtils.mostrarAdvertencia("Validación", e.getMessage())
                );
            } catch (DataAccessException | NotFoundException e) {
                logger.error("Error guardando atributo", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudo guardar el atributo")
                );
            }
        });
    }

    @FXML
    private void editarAtributo() {
        if (atributoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un atributo");
            return;
        }

        txtNombreAtributo.setText(atributoSeleccionado.getNombre());
        cbTipoAtributo.setValue(atributoSeleccionado.getTipo().name());
        txtOrdenAtributo.setText(String.valueOf(atributoSeleccionado.getOrden()));
        txtNombreAtributo.requestFocus();
    }

    @FXML
    private void eliminarAtributo() {
        if (atributoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un atributo");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
                "Confirmar eliminación",
                "¿Eliminar atributo?",
                atributoSeleccionado.getNombre()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    atributoService.eliminar(atributoSeleccionado.getId());
                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Atributo eliminado");
                        cargarDatos();
                        nuevoAtributo();
                    });
                } catch (DataAccessException | NotFoundException e) {
                    logger.error("Error eliminando atributo", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", "No se pudo eliminar el atributo")
                    );
                }
            });
        }
    }

    // ============================================
    // VALORES DE ATRIBUTOS (NUEVO)
    // ============================================

    private void cargarValoresAtributo(Atributo atributo) {
        if (atributo == null) {
            valoresAtributo.clear();
            return;
        }

        // Solo mostrar valores para LISTA y COLOR
        if (atributo.getTipo() != Atributo.TipoAtributo.LISTA &&
                atributo.getTipo() != Atributo.TipoAtributo.COLOR) {
            valoresAtributo.clear();
            lblAtributoSeleccionado.setText(
                    "Los atributos tipo " + atributo.getTipo().getValor() + " no necesitan valores predefinidos"
            );
            return;
        }

        lblAtributoSeleccionado.setText(
                "Valores de: " + atributo.getNombre() + " (" + atributo.getTipo().getValor() + ")"
        );

        ejecutarEnBackground(() -> {
            try {
                var valores = valorAtributoService.listarPorAtributo(atributo.getId());
                Platform.runLater(() -> valoresAtributo.setAll(valores));
            } catch (DataAccessException e) {
                logger.error("Error cargando valores", e);
            }
        });
    }

    private void actualizarVisibilidadCamposColor(Atributo.TipoAtributo tipo) {
        boolean esColor = tipo == Atributo.TipoAtributo.COLOR;
        if (lblCodigoHex != null) lblCodigoHex.setVisible(esColor);
        if (hboxColor != null) hboxColor.setVisible(esColor);
    }

    @FXML
    private void agregarValor() {
        if (atributoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Atributo requerido", "Seleccione un atributo primero");
            return;
        }

        if (atributoSeleccionado.getTipo() != Atributo.TipoAtributo.LISTA &&
                atributoSeleccionado.getTipo() != Atributo.TipoAtributo.COLOR) {
            AlertUtils.mostrarInfo("Información",
                    "Solo los atributos tipo LISTA o COLOR tienen valores predefinidos");
            return;
        }

        String valor = txtValor.getText().trim();
        if (valor.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Validación", "El valor es obligatorio");
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                ValorAtributo va = new ValorAtributo();
                va.setAtributoId(atributoSeleccionado.getId());
                va.setValor(valor);

                if (atributoSeleccionado.getTipo() == Atributo.TipoAtributo.COLOR &&
                        txtCodigoHexField != null && !txtCodigoHexField.getText().trim().isEmpty()) {
                    va.setCodigoHex(txtCodigoHexField.getText().trim());
                }

                va.setOrden(Integer.parseInt(txtOrdenValor.getText().trim()));

                valorAtributoService.crear(va);

                Platform.runLater(() -> {
                    AlertUtils.mostrarExito("Éxito", "Valor agregado correctamente");
                    cargarValoresAtributo(atributoSeleccionado);
                    limpiarFormularioValor();
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
        txtOrdenValor.setText(String.valueOf(valorSeleccionado.getOrden()));

        if (atributoSeleccionado != null &&
                atributoSeleccionado.getTipo() == Atributo.TipoAtributo.COLOR &&
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
                        cargarValoresAtributo(atributoSeleccionado);
                        limpiarFormularioValor();
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
    private void limpiarFormularioValor() {
        txtValor.clear();
        if (txtCodigoHexField != null) txtCodigoHexField.clear();
        txtOrdenValor.setText("0");
        if (colorPickerControl != null) colorPickerControl.setValue(Color.WHITE);
        valorSeleccionado = null;
        tablaValores.getSelectionModel().clearSelection();
    }

    // ============================================
    // ASIGNACIÓN CATEGORÍA-ATRIBUTOS
    // ============================================

    @FXML
    private void cargarAtributosCategoria() {
        Categoria categoria = cbCategoriaAsignar.getValue();
        if (categoria == null) return;
        cargarAtributosAsignados(categoria.getId());
    }

    private void cargarAtributosAsignados(Integer categoriaId) {
        ejecutarEnBackground(() -> {
            try {
                var atributosAsig = categoriaService.obtenerAtributosDeCategoria(categoriaId);

                ObservableList<AtributoAsignado> items = FXCollections.observableArrayList();
                for (Atributo a : atributosAsig) {
                    items.add(new AtributoAsignado(a, true, a.getOrden()));
                }

                Platform.runLater(() -> atributosAsignados.setAll(items));

            } catch (DataAccessException e) {
                logger.error("Error cargando atributos de categoría", e);
            }
        });
    }

    @FXML
    private void asignarAtributo() {
        Categoria categoria = cbCategoriaAsignar.getValue();
        Atributo atributo = cbAtributoAsignar.getValue();

        if (categoria == null || atributo == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida",
                    "Seleccione categoría y atributo");
            return;
        }

        boolean yaAsignado = atributosAsignados.stream()
                .anyMatch(aa -> aa.atributo.getId().equals(atributo.getId()));

        if (yaAsignado) {
            AlertUtils.mostrarAdvertencia("Ya asignado",
                    "Este atributo ya está asignado a la categoría");
            return;
        }

        boolean requerido = chkRequerido.isSelected();
        int orden = txtOrdenAsignacion != null && !txtOrdenAsignacion.getText().isEmpty()
                ? Integer.parseInt(txtOrdenAsignacion.getText())
                : 0;

        ejecutarEnBackground(() -> {
            try {
                categoriaService.asociarAtributo(categoria.getId(), atributo.getId(), requerido, orden);

                Platform.runLater(() -> {
                    AlertUtils.mostrarExito("Éxito", "Atributo asignado correctamente");
                    cargarAtributosAsignados(categoria.getId());
                });

            } catch (DataAccessException e) {
                logger.error("Error asignando atributo", e);
                Platform.runLater(() ->
                        AlertUtils.mostrarError("Error", "No se pudo asignar el atributo")
                );
            }
        });
    }

    @FXML
    private void quitarAtributo() {
        AtributoAsignado seleccionado = lvAtributosAsignados.getSelectionModel().getSelectedItem();
        Categoria categoria = cbCategoriaAsignar.getValue();

        if (seleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida",
                    "Seleccione un atributo de la lista");
            return;
        }

        if (categoria == null) {
            AlertUtils.mostrarAdvertencia("Error", "Seleccione una categoría");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
                "Confirmar",
                "¿Desasociar atributo de la categoría?",
                seleccionado.atributo.getNombre()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ejecutarEnBackground(() -> {
                try {
                    categoriaService.desasociarAtributo(categoria.getId(), seleccionado.atributo.getId());

                    Platform.runLater(() -> {
                        AlertUtils.mostrarExito("Éxito", "Atributo desasociado");
                        cargarAtributosAsignados(categoria.getId());
                    });

                } catch (DataAccessException e) {
                    logger.error("Error desasociando atributo", e);
                    Platform.runLater(() ->
                            AlertUtils.mostrarError("Error", "No se pudo desasociar el atributo")
                    );
                }
            });
        }
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

    // ============================================
    // CLASE AUXILIAR
    // ============================================

    private static class AtributoAsignado {
        final Atributo atributo;
        final boolean requerido;
        final int orden;

        AtributoAsignado(Atributo atributo, boolean requerido, int orden) {
            this.atributo = atributo;
            this.requerido = requerido;
            this.orden = orden;
        }
    }
}