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
 * Controlador para gestión de categorías y atributos
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
    @FXML private ListView<String> lvAtributosAsignados;

    @FXML private ProgressIndicator progressIndicator;

    // Datos
    private final ObservableList<Categoria> categorias;
    private final ObservableList<Atributo> atributos;
    private Categoria categoriaSeleccionada;
    private Atributo atributoSeleccionado;

    public CategoriaController() {
        this.categoriaService = new CategoriaService();
        this.atributoService = new AtributoService();
        this.valorAtributoService = new ValorAtributoService();
        this.categorias = FXCollections.observableArrayList();
        this.atributos = FXCollections.observableArrayList();
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
    }

    private void configurarEventos() {
        tablaCategorias.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> categoriaSeleccionada = newVal
        );

        tablaAtributos.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> atributoSeleccionado = newVal
        );

        txtOrdenAtributo.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) txtOrdenAtributo.setText(o);
        });
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
                    // Actualizar
                    Platform.runLater(() -> 
                        AlertUtils.mostrarAdvertencia("En desarrollo", 
                            "Actualización de categorías en desarrollo")
                    );
                }

                cargarDatos();
                Platform.runLater(this::nuevaCategoria);

            } catch (ValidationException e) {
                Platform.runLater(() -> 
                    AlertUtils.mostrarAdvertencia("Validación", e.getMessage())
                );
            } catch (DataAccessException e) {
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
    }

    @FXML
    private void eliminarCategoria() {
        if (categoriaSeleccionada == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione una categoría");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
            "Confirmar eliminación",
            "¿Eliminar categoría?",
            categoriaSeleccionada.getNombre()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            AlertUtils.mostrarAdvertencia("En desarrollo", 
                "Eliminación de categorías en desarrollo");
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
        cbTipoAtributo.setValue(atributoSeleccionado.getTipo().getValor());
        txtOrdenAtributo.setText(String.valueOf(atributoSeleccionado.getOrden()));
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

        // Abrir diálogo para gestionar valores
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

        ejecutarEnBackground(() -> {
            try {
                var atributosAsignados = categoriaService.obtenerAtributosDeCategoria(categoria.getId());
                
                ObservableList<String> items = FXCollections.observableArrayList();
                for (Atributo a : atributosAsignados) {
                    items.add(a.getNombre());
                }

                Platform.runLater(() -> lvAtributosAsignados.setItems(items));

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

        AlertUtils.mostrarInfo("En desarrollo", 
            "Asignación de atributos en desarrollo.\n" +
            "Se implementará con CategoriaDAO.asociarAtributo()");
    }

    @FXML
    private void quitarAtributo() {
        String seleccionado = lvAtributosAsignados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", 
                "Seleccione un atributo de la lista");
            return;
        }

        AlertUtils.mostrarInfo("En desarrollo", 
            "Desasignación de atributos en desarrollo");
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