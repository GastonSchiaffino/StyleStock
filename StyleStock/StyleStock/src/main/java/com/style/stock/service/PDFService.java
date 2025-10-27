package com.style.stock.service;

import com.style.stock.model.*;
import com.style.stock.util.AppConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio para generación de PDFs mejorado
 */
public class PDFService {
    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 12;
    private static final float TITLE_SIZE = 16;

    /**
     * Genera un PDF de factura
     */
    public String generarFacturaPDF(Factura factura) throws IOException {
        Path pdfDir = crearDirectorioPDF();
        String nombreArchivo = generarNombreArchivo(factura);
        Path pdfPath = pdfDir.resolve(nombreArchivo);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = page.getMediaBox().getHeight() - MARGIN;

                // Encabezado
                yPosition = dibujarEncabezado(contentStream, factura, yPosition);
                yPosition -= 20;

                // Datos del cliente
                yPosition = dibujarDatosCliente(contentStream, factura, yPosition);
                yPosition -= 20;

                // Línea separadora
                dibujarLinea(contentStream, MARGIN, yPosition, 
                    page.getMediaBox().getWidth() - MARGIN, yPosition);
                yPosition -= 20;

                // Tabla de productos
                yPosition = dibujarTablaProductos(contentStream, factura, yPosition);
                yPosition -= 20;

                // Totales
                yPosition = dibujarTotales(contentStream, factura, yPosition, page.getMediaBox().getWidth());
                
                // Pie de página
                dibujarPiePagina(contentStream, page.getMediaBox().getWidth());
            }

            document.save(pdfPath.toFile());
            logger.info("PDF generado: {}", pdfPath);
        }

        return pdfPath.toString();
    }

    private Path crearDirectorioPDF() throws IOException {
        Path pdfDir = Paths.get(System.getProperty("user.home"), "style-stock", "facturas");
        if (!Files.exists(pdfDir)) {
            Files.createDirectories(pdfDir);
        }
        return pdfDir;
    }

    private String generarNombreArchivo(Factura factura) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String numeroFactura = factura.getNumeroFactura() != null 
            ? factura.getNumeroFactura().replace("/", "-")
            : "PREVIEW";
        return String.format("Factura_%s_%s.pdf", numeroFactura, timestamp);
    }

    private float dibujarEncabezado(PDPageContentStream cs, Factura factura, float y) throws IOException {
        // Título
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TITLE_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("FACTURA " + (factura.getTipo() != null ? factura.getTipo().getValor() : "A"));
        cs.endText();

        y -= 20;

        // Número de factura
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        String numeroFactura = factura.getNumeroFactura() != null 
            ? factura.getNumeroFactura() 
            : "PREVIEW";
        cs.showText("Número: " + numeroFactura);
        cs.endText();

        y -= 15;

        // Fecha
        cs.beginText();
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Fecha: " + factura.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        cs.endText();

        return y - 15;
    }

    private float dibujarDatosCliente(PDPageContentStream cs, Factura factura, float y) throws IOException {
        Cliente cliente = factura.getCliente();
        
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Cliente:");
        cs.endText();

        y -= 15;

        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(cliente.getNombre());
        cs.endText();

        if (cliente.getCuit() != null && !cliente.getCuit().isEmpty()) {
            y -= 15;
            cs.beginText();
            cs.newLineAtOffset(MARGIN, y);
            cs.showText("CUIT: " + cliente.getCuit());
            cs.endText();
        }

        if (cliente.getDireccion() != null && !cliente.getDireccion().isEmpty()) {
            y -= 15;
            cs.beginText();
            cs.newLineAtOffset(MARGIN, y);
            cs.showText("Dirección: " + cliente.getDireccion());
            cs.endText();
        }

        return y - 15;
    }

    private float dibujarTablaProductos(PDPageContentStream cs, Factura factura, float y) throws IOException {
        // Encabezados de tabla
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Cant.");
        cs.newLineAtOffset(50, 0);
        cs.showText("Descripción");
        cs.newLineAtOffset(250, 0);
        cs.showText("P. Unit.");
        cs.newLineAtOffset(80, 0);
        cs.showText("Subtotal");
        cs.endText();

        y -= 15;

        // Línea debajo de encabezados
        dibujarLinea(cs, MARGIN, y, 500, y);
        y -= 10;

        // Productos
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
        for (DetalleFactura detalle : factura.getDetalles()) {
            cs.beginText();
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(String.valueOf(detalle.getCantidad()));
            cs.newLineAtOffset(50, 0);
            
            String descripcion = detalle.getProducto().getDescripcion();
            if (descripcion.length() > 35) {
                descripcion = descripcion.substring(0, 32) + "...";
            }
            cs.showText(descripcion);
            
            cs.newLineAtOffset(250, 0);
            cs.showText(String.format("$%.2f", detalle.getPrecioUnitario()));
            cs.newLineAtOffset(80, 0);
            cs.showText(String.format("$%.2f", detalle.getSubtotal()));
            cs.endText();

            y -= 15;

            // Nueva página si es necesario
            if (y < 100) {
                break; // Por simplicidad, limitamos a una página
            }
        }

        return y;
    }

    private float dibujarTotales(PDPageContentStream cs, Factura factura, float y, float pageWidth) throws IOException {
        float xPos = pageWidth - 200;

        // Línea antes de totales
        dibujarLinea(cs, xPos - 20, y, pageWidth - MARGIN, y);
        y -= 15;

        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE);
        cs.newLineAtOffset(xPos, y);
        cs.showText("Subtotal:");
        cs.newLineAtOffset(80, 0);
        cs.showText(String.format("$%.2f", factura.getSubtotal()));
        cs.endText();

        if (factura.getDescuento() != null && factura.getDescuento() > 0) {
            y -= 15;
            cs.beginText();
            cs.newLineAtOffset(xPos, y);
            cs.showText("Descuento:");
            cs.newLineAtOffset(80, 0);
            cs.showText(String.format("-$%.2f", factura.getDescuento()));
            cs.endText();
        }

        y -= 15;
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE + 2);
        cs.newLineAtOffset(xPos, y);
        cs.showText("TOTAL:");
        cs.newLineAtOffset(80, 0);
        cs.showText(String.format("$%.2f", factura.getTotal()));
        cs.endText();

        return y;
    }

    private void dibujarPiePagina(PDPageContentStream cs, float pageWidth) throws IOException {
        float y = MARGIN;
        
        String piePagina = AppConfig.get("factura_pie", "Gracias por su compra");
        
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.newLineAtOffset((pageWidth - 200) / 2, y);
        cs.showText(piePagina);
        cs.endText();
    }

    private void dibujarLinea(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }
}