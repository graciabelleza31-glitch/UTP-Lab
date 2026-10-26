package com.integrador.lab.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.integrador.lab.modelo.Producto;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteService {

    /**
     * Genera un PDF con las alertas de stock (productos con stock crítico o bajo).
     */
    public ByteArrayInputStream generarPdfAlertasStock(List<Producto> productos) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // ===== Encabezado =====
            Font tituloFont = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(30, 41, 59));
            Paragraph titulo = new Paragraph("UTP +Lab", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Font subtituloFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(37, 99, 235));
            Paragraph subtitulo = new Paragraph("Reporte de Alertas de Stock", subtituloFont);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(10);
            document.add(subtitulo);

            Font fechaFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(100, 116, 139));
            Paragraph fecha = new Paragraph(
                "Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                fechaFont
            );
            fecha.setAlignment(Element.ALIGN_CENTER);
            fecha.setSpacingAfter(25);
            document.add(fecha);

            // ===== Resumen =====
            Font resumenFont = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(30, 41, 59));
            Paragraph resumen = new Paragraph(
                "Total de insumos con stock bajo o crítico: " + productos.size(),
                resumenFont
            );
            resumen.setSpacingAfter(15);
            document.add(resumen);

            // ===== Tabla =====
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 5, 2, 2, 2});

            // Encabezados
            String[] headers = {"Código", "Insumo / Material", "Stock", "Mínimo", "Estado"};
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(37, 99, 235));
                cell.setPadding(10);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Filas
            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            for (Producto p : productos) {
                int stock = p.getStockActual() != null ? p.getStockActual() : 0;

                String estado;
                Color colorEstado;
                if (stock <= 0) {
                    estado = "AGOTADO";
                    colorEstado = new Color(153, 27, 27);
                } else if (stock <= 10) {
                    estado = "CRÍTICO";
                    colorEstado = new Color(220, 38, 38);
                } else {
                    estado = "BAJO";
                    colorEstado = new Color(217, 119, 6);
                }

                // Código
                PdfPCell c1 = new PdfPCell(new Phrase(p.getIdProducto(), bodyFont));
                c1.setPadding(8);
                table.addCell(c1);

                // Nombre
                PdfPCell c2 = new PdfPCell(new Phrase(p.getNombre(), bodyFont));
                c2.setPadding(8);
                table.addCell(c2);

                // Stock
                PdfPCell c3 = new PdfPCell(new Phrase(String.valueOf(stock), bodyFont));
                c3.setPadding(8);
                c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c3);

                // Mínimo
                PdfPCell c4 = new PdfPCell(new Phrase("10", bodyFont));
                c4.setPadding(8);
                c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c4);

                // Estado
                Font estadoFont = new Font(Font.HELVETICA, 10, Font.BOLD, colorEstado);
                PdfPCell c5 = new PdfPCell(new Phrase(estado, estadoFont));
                c5.setPadding(8);
                c5.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c5);
            }

            document.add(table);

            // ===== Pie de página =====
            Font pieFont = new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(148, 163, 184));
            Paragraph pie = new Paragraph(
                "\nDocumento generado automáticamente por el sistema UTP +Lab.",
                pieFont
            );
            pie.setAlignment(Element.ALIGN_CENTER);
            pie.setSpacingBefore(20);
            document.add(pie);

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
