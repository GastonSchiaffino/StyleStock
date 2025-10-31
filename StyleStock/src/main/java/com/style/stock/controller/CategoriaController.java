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

import java.util.Optional;

/**
 * Controlador COMPLETO para gestión de categorías y atributos
 */
public class CategoriaController {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaController.class);

    // Servicios
    private final CategoriaService categoriaService;
    private final AtributoService atributoService;
    private final ValorAtributoService valorAtributoService;

    // Componentes UI - Categorías
    @FXML private TextField txtNombreCategoria;
    @FXML private TextField txtDescripcionCategoria;
    @FXML private CheckBox chkRequiereVariantes;
    @FXML private TableView<Categoria> tablaCategorias;
    @FXML private TableColumn<Categoria, Integer> colIdCategoria;
    @FXML private TableColumn<Categoria, String> colNombreCategoria;
    @FXML private TableColumn<Categoria, Boolean> colRequiereVar;

    // Componentes UI - Atributos
    @FXML private TextField txtNombreAtributo;
    @FXML private ComboBox<String> cbTipoAtributo;
    @FXML private TextField txtOrdenAtributo;
    @FXML private TableView<Atributo> tablaAtributos;
    @FXML private TableColumn<Atributo, Integer> colIdAtributo;
    @FXML private TableColumn<Atributo, String> colNombreAtributo;
    @FXML private TableColumn<Atributo, String> colTipoAtributo;
    @FXML private TableColumn<Atributo, Integer> colOrdenAtributo;

    // Componentes UI - Asignación
    @FXML private ComboBox<Categoria> cbCategoriaAsignar;
    @FXML private ComboBox<Atributo> cbAtributoAsignar;
    @FXML private CheckBox chkRequerido;
    @FXML private TextField txtOrdenAsignacion;
    @FXML private ListView<AtributoAsignado> lvAtributosAsignados;

    @FXML private ProgressIndicator progressIndicator;

    // Datos
    private final ObservableList<Categoria> categorias;
    private final ObservableList<Atributo> atributos;
    private final ObservableList<AtributoAsignado> atributosAsignados;
    private Categoria categoriaSeleccionada;
    private Atributo atributoSeleccionado;

    public CategoriaController() {
        this.categoriaService = new CategoriaService();
        this.atributoService = new AtributoService();
        this.valorAtributoService = new ValorAtributoService();
        this.categorias = FXCollections.observableArrayList();
        this.atributos = FXCollections.observableArrayList();
        this.atributosAsignados = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTablas();
        configurarEventos();
        cargarDatos();
    }

    private void configurarTablas() {
        // Tabla Categorías
        colIdCategoria.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombreCategoria.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colRequiereVar.setCellValueFactory(new PropertyValueFactory<>("requiereVariantes"));
        tablaCategorias.setItems(categorias);

        // Tabla Atributos
        colIdAtributo.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombreAtributo.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTipoAtributo.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTipo().getValor()
                )
        );
        colOrdenAtributo.setCellValueFactory(new PropertyValueFactory<>("orden"));
        tablaAtributos.setItems(atributos);

        // ListView de atributos asignados
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
        tablaCategorias.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    categoriaSeleccionada = newVal;
                    if (newVal != null) {
                        cargarAtributosAsignados(newVal.getId());
                    }
                }
        );

        tablaAtributos.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> atributoSeleccionado = newVal
        );

        txtOrdenAtributo.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) txtOrdenAtributo.setText(o);
        });

        if (txtOrdenAsignacion != null) {
            txtOrdenAsignacion.textProperty().addListener((obs, o, n) -> {
                if (!n.matches("\\d*")) txtOrdenAsignacion.setText(o);
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

    // ========== CATEGORÍAS ==========

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

    // ========== ATRIBUTOS ==========

    @FXML
    private void nuevoAtributo() {
        txtNombreAtributo.clear();
        cbTipoAtributo.setValue("LISTA");
        txtOrdenAtributo.setText("0");
        atributoSeleccionado = null;
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

    @FXML
    private void gestionarValores() {
        if (atributoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un atributo");
            return;
        }

        if (atributoSeleccionado.getTipo() != Atributo.TipoAtributo.LISTA &&
                atributoSeleccionado.getTipo() != Atributo.TipoAtributo.COLOR) {
            AlertUtils.mostrarInfo("Información",
                    "Solo los atributos tipo LISTA o COLOR tienen valores predefinidos");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Agregar Valor");
        dialog.setHeaderText("Agregar valor para: " + atributoSeleccionado.getNombre());
        dialog.setContentText("Valor:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(valor -> {
            ejecutarEnBackground(() -> {
                try {
                    ValorAtributo va = new ValorAtributo();
                    va.setAtributoId(atributoSeleccionado.getId());
                    va.setValor(valor.trim());
                    valorAtributoService.crear(va);

                    Platform.runLater(() ->
                            AlertUtils.mostrarExito("Éxito", "Valor agregado correctamente")
                    );
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
        });
    }

    // ========== ASIGNACIÓN ==========

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

    // Clase auxiliar
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