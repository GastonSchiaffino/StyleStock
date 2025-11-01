package com.style.stock.controller;

import com.style.stock.exception.*;
import com.style.stock.model.MetodoPago;
import com.style.stock.service.ConfiguracionService;
import com.style.stock.service.MetodoPagoService;
import com.style.stock.util.AlertUtils;
import com.style.stock.util.AppConfig;
import com.style.stock.util.BackupUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controlador para configuración del sistema
 */
public class ConfiguracionController {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionController.class);

    // Servicios
    private final ConfiguracionService configuracionService;
    private final MetodoPagoService metodoPagoService;

    // Tab Empresa
    @FXML private TextField txtNombreEmpresa;
    @FXML private TextField txtDireccionEmpresa;
    @FXML private TextField txtTelefonoEmpresa;
    @FXML private TextField txtEmailEmpresa;
    @FXML private TextField txtCuitEmpresa;
    @FXML private TextArea txtPieComprobante;

    // Tab Stock
    @FXML private Spinner<Integer> spinStockMinimo;
    @FXML private CheckBox chkAlertasStockActivas;
    @FXML private CheckBox chkGenerarSkuAuto;
    @FXML private CheckBox chkRegistrarMovimientos;
    @FXML private Spinner<Integer> spinDiasMovimientos;

    // Tab Backup
    @FXML private CheckBox chkBackupAutomatico;
    @FXML private Spinner<Integer> spinDiasBackup;
    @FXML private Label lblUltimoBackup;
    @FXML private Label lblUbicacionBackup;
    @FXML private TableView<BackupInfo> tablaBackups;
    @FXML private TableColumn<BackupInfo, String> colBackupNombre;
    @FXML private TableColumn<BackupInfo, String> colBackupFecha;
    @FXML private TableColumn<BackupInfo, String> colBackupTamano;

    // Tab Métodos de Pago
    @FXML private TextField txtNombreMetodo;
    @FXML private TextField txtComisionMetodo;
    @FXML private CheckBox chkRequiereCuotas;
    @FXML private TableView<MetodoPago> tablaMetodosPago;
    @FXML private TableColumn<MetodoPago, Integer> colMetodoId;
    @FXML private TableColumn<MetodoPago, String> colMetodoNombre;
    @FXML private TableColumn<MetodoPago, Double> colMetodoComision;
    @FXML private TableColumn<MetodoPago, Boolean> colMetodoCuotas;
    @FXML private TableColumn<MetodoPago, Boolean> colMetodoActivo;

    // Tab Avanzado
    @FXML private Label lblUbicacionDB;
    @FXML private Label lblTamanoDB;
    @FXML private Label lblUbicacionLogs;
    @FXML private Label lblJavaVersion;
    @FXML private Label lblJavaFXVersion;
    @FXML private Label lblSistemaOperativo;

    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblEstado;

    // Datos
    private final ObservableList<MetodoPago> metodosPago;
    private final ObservableList<BackupInfo> backups;
    private MetodoPago metodoPagoSeleccionado;

    public ConfiguracionController() {
        this.configuracionService = new ConfiguracionService();
        this.metodoPagoService = new MetodoPagoService();
        this.metodosPago = FXCollections.observableArrayList();
        this.backups = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarComponentes();
        cargarConfiguracion();
        cargarMetodosPago();
        cargarBackups();
        mostrarInformacionSistema();
    }

    private void configurarComponentes() {
        // Spinners
        spinStockMinimo.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 5));
        spinDiasMovimientos.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 365, 90));
        spinDiasBackup.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 7));

        // Tabla Métodos de Pago
        colMetodoId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMetodoNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colMetodoComision.setCellValueFactory(new PropertyValueFactory<>("comisionPorcentaje"));
        colMetodoCuotas.setCellValueFactory(new PropertyValueFactory<>("requiereCuotas"));
        colMetodoActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));
        tablaMetodosPago.setItems(metodosPago);

        tablaMetodosPago.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> metodoPagoSeleccionado = newVal
        );

        // Tabla Backups
        colBackupNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colBackupFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colBackupTamano.setCellValueFactory(new PropertyValueFactory<>("tamano"));
        tablaBackups.setItems(backups);

        // Validación numérica
        txtComisionMetodo.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*\\.?\\d*")) txtComisionMetodo.setText(o);
        });
    }

    private void cargarConfiguracion() {
        ejecutarEnBackground(() -> {
            try {
                // Datos de empresa
                String nombreEmpresa = configuracionService.obtener("empresa_nombre", "StyleStock Showroom");
                String direccion = configuracionService.obtener("empresa_direccion", "");
                String telefono = configuracionService.obtener("empresa_telefono", "");
                String email = configuracionService.obtener("empresa_email", "");
                String cuit = configuracionService.obtener("empresa_cuit", "");
                String pie = configuracionService.obtener("factura_pie", "Gracias por su compra");

                // Stock
                Integer stockMin = configuracionService.obtenerInt("stock_alerta_global", 5);
                Boolean alertas = configuracionService.obtenerBoolean("stock_alerta_activa", true);
                Boolean skuAuto = configuracionService.obtenerBoolean("generar_sku_auto", true);

                // Backup
                Boolean backupAuto = configuracionService.obtenerBoolean("backup_auto", true);
                Integer diasBackup = configuracionService.obtenerInt("backup_dias", 7);
                String ultimoBackup = configuracionService.obtener("ultimo_backup", "Nunca");

                Platform.runLater(() -> {
                    txtNombreEmpresa.setText(nombreEmpresa);
                    txtDireccionEmpresa.setText(direccion);
                    txtTelefonoEmpresa.setText(telefono);
                    txtEmailEmpresa.setText(email);
                    txtCuitEmpresa.setText(cuit);
                    txtPieComprobante.setText(pie);

                    spinStockMinimo.getValueFactory().setValue(stockMin);
                    chkAlertasStockActivas.setSelected(alertas);
                    chkGenerarSkuAuto.setSelected(skuAuto);

                    chkBackupAutomatico.setSelected(backupAuto);
                    spinDiasBackup.getValueFactory().setValue(diasBackup);
                    lblUltimoBackup.setText(ultimoBackup);
                });

            } catch (DataAccessException e) {
                logger.error("Error cargando configuración", e);
            }
        });

        // Ubicaciones
        Path appDir = Paths.get(System.getProperty("user.home"), "style-stock");
        lblUbicacionBackup.setText(appDir.resolve("backups").toString());
        lblUbicacionDB.setText(appDir.resolve("style_stock.db").toString());
        lblUbicacionLogs.setText(appDir.resolve("logs").toString());
    }

    private void cargarMetodosPago() {
        ejecutarEnBackground(() -> {
            try {
                var lista = metodoPagoService.listarTodos();
                Platform.runLater(() -> metodosPago.setAll(lista));
            } catch (DataAccessException e) {
                logger.error("Error cargando métodos de pago", e);
            }
        });
    }

    private void cargarBackups() {
        ejecutarEnBackground(() -> {
            try {
                Path backupDir = Paths.get(System.getProperty("user.home"), "style-stock", "backups");
                if (!Files.exists(backupDir)) {
                    return;
                }

                ObservableList<BackupInfo> lista = FXCollections.observableArrayList();
                File[] archivos = backupDir.toFile().listFiles((dir, name) -> name.endsWith(".db"));
                
                if (archivos != null) {
                    for (File f : archivos) {
                        BackupInfo info = new BackupInfo();
                        info.setNombre(f.getName());
                        info.setTamano(String.format("%.2f MB", f.length() / 1024.0 / 1024.0));
                        // Fecha simplificada
                        info.setFecha(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        lista.add(info);
                    }
                }

                Platform.runLater(() -> backups.setAll(lista));

            } catch (Exception e) {
                logger.error("Error cargando backups", e);
            }
        });
    }

    private void mostrarInformacionSistema() {
        lblJavaVersion.setText(System.getProperty("java.version"));
        lblJavaFXVersion.setText(System.getProperty("javafx.version", "21.0.1"));
        lblSistemaOperativo.setText(System.getProperty("os.name") + " " + 
                                     System.getProperty("os.version"));

        // Calcular tamaño DB
        ejecutarEnBackground(() -> {
            try {
                Path dbPath = Paths.get(System.getProperty("user.home"), "style-stock", "style_stock.db");
                if (Files.exists(dbPath)) {
                    long bytes = Files.size(dbPath);
                    String tamano = String.format("%.2f MB", bytes / 1024.0 / 1024.0);
                    Platform.runLater(() -> lblTamanoDB.setText(tamano));
                }
            } catch (Exception e) {
                logger.error("Error calculando tamaño DB", e);
            }
        });
    }

    // ========== EMPRESA ==========

    @FXML
    private void guardarDatosEmpresa() {
        ejecutarEnBackground(() -> {
            try {
                configuracionService.guardar("empresa_nombre", txtNombreEmpresa.getText());
                configuracionService.guardar("empresa_direccion", txtDireccionEmpresa.getText());
                configuracionService.guardar("empresa_telefono", txtTelefonoEmpresa.getText());
                configuracionService.guardar("empresa_email", txtEmailEmpresa.getText());
                configuracionService.guardar("empresa_cuit", txtCuitEmpresa.getText());
                configuracionService.guardar("factura_pie", txtPieComprobante.getText());

                Platform.runLater(() -> 
                    AlertUtils.mostrarExito("Éxito", "Datos de empresa guardados correctamente")
                );
            } catch (DataAccessException e) {
                logger.error("Error guardando datos", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudieron guardar los datos")
                );
            }
        });
    }

    // ========== STOCK ==========

    @FXML
    private void guardarConfigStock() {
        ejecutarEnBackground(() -> {
            try {
                configuracionService.guardar("stock_alerta_global", spinStockMinimo.getValue());
                configuracionService.guardar("stock_alerta_activa", chkAlertasStockActivas.isSelected());
                configuracionService.guardar("generar_sku_auto", chkGenerarSkuAuto.isSelected());

                Platform.runLater(() -> 
                    AlertUtils.mostrarExito("Éxito", "Configuración de stock guardada")
                );
            } catch (DataAccessException e) {
                logger.error("Error guardando config stock", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudo guardar")
                );
            }
        });
    }

    // ========== BACKUP ==========

    @FXML
    private void realizarBackupManual() {
        ejecutarEnBackground(() -> {
            boolean exito = BackupUtils.realizarBackup();
            
            Platform.runLater(() -> {
                if (exito) {
                    AlertUtils.mostrarExito("Éxito", "Backup realizado correctamente");
                    cargarBackups();
                    lblUltimoBackup.setText(LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                } else {
                    AlertUtils.mostrarError("Error", "No se pudo realizar el backup");
                }
            });
        });
    }

    @FXML
    private void restaurarBackup() {
        BackupInfo seleccionado = tablaBackups.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", 
                "Seleccione un backup de la lista");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
            "⚠️ ADVERTENCIA",
            "¿Restaurar backup?\n\nEsto reemplazará TODOS los datos actuales",
            "Archivo: " + seleccionado.getNombre()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            AlertUtils.mostrarInfo("En desarrollo", 
                "Restauración de backup en desarrollo");
        }
    }

    @FXML
    private void abrirCarpetaBackup() {
        abrirCarpeta(Paths.get(System.getProperty("user.home"), "style-stock", "backups"));
    }

    @FXML
    private void eliminarBackup() {
        BackupInfo seleccionado = tablaBackups.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un backup");
            return;
        }

        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
            "Confirmar eliminación",
            "¿Eliminar backup?",
            seleccionado.getNombre()
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            AlertUtils.mostrarInfo("En desarrollo", "Eliminación en desarrollo");
        }
    }

    // ========== MÉTODOS DE PAGO ==========

    @FXML
    private void nuevoMetodoPago() {
        txtNombreMetodo.clear();
        txtComisionMetodo.setText("0");
        chkRequiereCuotas.setSelected(false);
        metodoPagoSeleccionado = null;
    }

    @FXML
    private void guardarMetodoPago() {
        String nombre = txtNombreMetodo.getText().trim();
        if (nombre.isEmpty()) {
            AlertUtils.mostrarAdvertencia("Validación", "El nombre es obligatorio");
            return;
        }

        ejecutarEnBackground(() -> {
            try {
                MetodoPago metodo = metodoPagoSeleccionado != null ? 
                    metodoPagoSeleccionado : new MetodoPago();
                
                metodo.setNombre(nombre);
                metodo.setComisionPorcentaje(Double.parseDouble(txtComisionMetodo.getText()));
                metodo.setRequiereCuotas(chkRequiereCuotas.isSelected());

                if (metodoPagoSeleccionado == null) {
                    metodoPagoService.crear(metodo);
                } else {
                    metodoPagoService.actualizar(metodo);
                }

                Platform.runLater(() -> {
                    AlertUtils.mostrarExito("Éxito", "Método de pago guardado");
                    cargarMetodosPago();
                    nuevoMetodoPago();
                });

            } catch (ValidationException e) {
                Platform.runLater(() -> 
                    AlertUtils.mostrarAdvertencia("Validación", e.getMessage())
                );
            } catch (DataAccessException | NotFoundException e) {
                logger.error("Error guardando método", e);
                Platform.runLater(() -> 
                    AlertUtils.mostrarError("Error", "No se pudo guardar")
                );
            }
        });
    }

    @FXML
    private void editarMetodoPago() {
        if (metodoPagoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un método");
            return;
        }

        txtNombreMetodo.setText(metodoPagoSeleccionado.getNombre());
        txtComisionMetodo.setText(String.valueOf(metodoPagoSeleccionado.getComisionPorcentaje()));
        chkRequiereCuotas.setSelected(metodoPagoSeleccionado.getRequiereCuotas());
    }

    @FXML
    private void desactivarMetodoPago() {
        if (metodoPagoSeleccionado == null) {
            AlertUtils.mostrarAdvertencia("Selección requerida", "Seleccione un método");
            return;
        }

        AlertUtils.mostrarInfo("En desarrollo", "Desactivación en desarrollo");
    }

    // ========== AVANZADO ==========

    @FXML
    private void optimizarBD() {
        AlertUtils.mostrarInfo("Optimizar BD", 
            "Optimización de base de datos en desarrollo");
    }

    @FXML
    private void verificarIntegridad() {
        AlertUtils.mostrarInfo("Verificar", 
            "Verificación de integridad en desarrollo");
    }

    @FXML
    private void abrirCarpetaLogs() {
        abrirCarpeta(Paths.get(System.getProperty("user.home"), "style-stock", "logs"));
    }

    @FXML
    private void verLogs() {
        AlertUtils.mostrarInfo("Ver Logs", "Visor de logs en desarrollo");
    }

    @FXML
    private void limpiarLogs() {
        AlertUtils.mostrarInfo("Limpiar Logs", "Limpieza de logs en desarrollo");
    }

    @FXML
    private void resetearConfiguracion() {
        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
            "⚠️ RESETEAR CONFIGURACIÓN",
            "Esto restaurará todas las configuraciones a sus valores por defecto",
            "¿Continuar?"
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            AlertUtils.mostrarInfo("En desarrollo", "Reset de configuración en desarrollo");
        }
    }

    @FXML
    private void limpiarBaseDatos() {
        Optional<ButtonType> result = AlertUtils.mostrarConfirmacion(
            "🚨 PELIGRO: ELIMINAR TODOS LOS DATOS",
            "ESTO ELIMINARÁ PERMANENTEMENTE:\n" +
            "• Todas las ventas\n" +
            "• Todos los productos\n" +
            "• Todos los clientes\n" +
            "• Todo el historial\n\n" +
            "Esta acción NO SE PUEDE DESHACER",
            "¿Está ABSOLUTAMENTE seguro?"
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            AlertUtils.mostrarError("Bloqueado por seguridad", 
                "Esta función está deshabilitada por seguridad.\n" +
                "Para limpiar la BD, elimine manualmente el archivo:\n" +
                lblUbicacionDB.getText());
        }
    }

    @FXML
    private void guardarTodo() {
        guardarDatosEmpresa();
        guardarConfigStock();
    }

    private void abrirCarpeta(Path path) {
        try {
            if (Files.exists(path)) {
                java.awt.Desktop.getDesktop().open(path.toFile());
            } else {
                AlertUtils.mostrarAdvertencia("No encontrado", 
                    "La carpeta no existe: " + path);
            }
        } catch (Exception e) {
            logger.error("Error abriendo carpeta", e);
            AlertUtils.mostrarError("Error", "No se pudo abrir la carpeta");
        }
    }

    private void ejecutarEnBackground(Runnable tarea) {
        progressIndicator.setVisible(true);
        lblEstado.setText("Procesando...");
        new Thread(() -> {
            try {
                tarea.run();
            } finally {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    lblEstado.setText("");
                });
            }
        }).start();
    }

    // Clase auxiliar para BackupInfo
    public static class BackupInfo {
        private String nombre;
        private String fecha;
        private String tamano;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getFecha() { return fecha; }
        public void setFecha(String fecha) { this.fecha = fecha; }
        public String getTamano() { return tamano; }
        public void setTamano(String tamano) { this.tamano = tamano; }
    }
}