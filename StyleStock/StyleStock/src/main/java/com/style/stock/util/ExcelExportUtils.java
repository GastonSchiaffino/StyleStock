package com.style.stock.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utilidades para exportar datos a Excel
 */
public class ExcelExportUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExcelExportUtils.class);

    /**
     * Exporta una lista de objetos a Excel
     */
    public static <T> boolean exportarAExcel(List<T> datos, String[] columnas, 
            String[] propiedades, String nombreArchivo) {
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Datos");

            // Estilo para encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Crear fila de encabezados
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Crear filas de datos
            int rowNum = 1;
            for (T item : datos) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < propiedades.length; i++) {
                    Cell cell = row.createCell(i);
                    Object valor = obtenerValorPropiedad(item, propiedades[i]);
                    setCellValue(cell, valor);
                }
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Guardar archivo
            Path exportDir = Paths.get(System.getProperty("user.home"), "style-stock", "exports");
            if (!exportDir.toFile().exists()) {
                exportDir.toFile().mkdirs();
            }

            Path filePath = exportDir.resolve(nombreArchivo);
            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }

            logger.info("Datos exportados a Excel: {}", filePath);
            return true;

        } catch (IOException e) {
            logger.error("Error exportando a Excel", e);
            return false;
        }
    }

    private static Object obtenerValorPropiedad(Object objeto, String propiedad) {
        try {
            String metodo = "get" + propiedad.substring(0, 1).toUpperCase() + propiedad.substring(1);
            return objeto.getClass().getMethod(metodo).invoke(objeto);
        } catch (Exception e) {
            logger.warn("No se pudo obtener valor de propiedad: {}", propiedad);
            return "";
        }
    }

    private static void setCellValue(Cell cell, Object valor) {
        if (valor == null) {
            cell.setCellValue("");
        } else if (valor instanceof Number) {
            cell.setCellValue(((Number) valor).doubleValue());
        } else if (valor instanceof Boolean) {
            cell.setCellValue((Boolean) valor);
        } else {
            cell.setCellValue(valor.toString());
        }
    }
}

