package com.style.stock.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Utilidades para mostrar alertas y diálogos
 */
public class AlertUtils {

    public static void mostrarError(String titulo, String mensaje) {
        mostrarError(titulo, mensaje, null);
    }

    public static void mostrarError(String titulo, String mensaje, String detalle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(mensaje);
        if (detalle != null && !detalle.isEmpty()) {
            alert.setContentText(detalle);
        }
        alert.showAndWait();
    }

    public static void mostrarAdvertencia(String titulo, String mensaje) {
        mostrarAdvertencia(titulo, mensaje, null);
    }

    public static void mostrarAdvertencia(String titulo, String mensaje, String detalle) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(mensaje);
        if (detalle != null && !detalle.isEmpty()) {
            alert.setContentText(detalle);
        }
        alert.showAndWait();
    }

    public static void mostrarExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(mensaje);
        alert.showAndWait();
    }

    public static void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static Optional<ButtonType> mostrarConfirmacion(String titulo, String mensaje) {
        return mostrarConfirmacion(titulo, mensaje, null);
    }

    public static Optional<ButtonType> mostrarConfirmacion(String titulo, String mensaje, String detalle) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(mensaje);
        if (detalle != null && !detalle.isEmpty()) {
            alert.setContentText(detalle);
        }
        return alert.showAndWait();
    }
}

