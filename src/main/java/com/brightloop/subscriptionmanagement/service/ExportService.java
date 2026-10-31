package com.brightloop.subscriptionmanagement.service;

import com.brightloop.subscriptionmanagement.dto.request.ExportFormat;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Writes reports as Excel (.xlsx) or PDF files in the LoopPay style.
 *
 * It knows nothing about invoices or payments: callers pass a {@link Report} made of tables
 * (headers + rows of plain values) and this service handles layout and formatting.
 *
 * Value types decide the formatting: BigDecimal = money (2 decimals, totalled), Integer/Long = counts,
 * LocalDate / LocalDateTime = dates, anything else is written as text.
 */
@Service
public class ExportService {

    /** Brand colours: aubergine header, silver stripe */
    private static final Color AUBERGINE = new Color(0x3A, 0x0F, 0x34);
    private static final Color SILVER = new Color(0xDD, 0xE3, 0xE8);
    private static final Color MUTED_TEXT = new Color(0x6B, 0x72, 0x80);

    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** SXSSF keeps this many rows in memory and streams the rest to a temp file */
    private static final int EXCEL_ROW_WINDOW = 200;
    /** Large PDF tables are written to the document in chunks so they never sit in memory whole */
    private static final int PDF_ROW_CHUNK = 500;

    /**
     * One table of a report: a sheet in Excel, a section in the PDF.
     *
     * @param totals add a "Total" row summing every money (BigDecimal) column
     */
    public record Table(String name, List<String> headers, List<List<Object>> rows, boolean totals) {
    }

    /**
     * A complete report.
     *
     * @param fileName      short name used in the download file name, e.g. "invoices"
     * @param title         heading, e.g. "Invoices"
     * @param filterSummary human-readable filters, e.g. "Invoice status: Overdue · Search: acme"
     */
    public record Report(String fileName, String title, String filterSummary, List<Table> tables) {

        /** True when no table has a single row */
        public boolean isEmpty() {
            return tables.stream().allMatch(table -> table.rows().isEmpty());
        }
    }

    /** Writes the report in the requested format to the stream (the stream is not closed). */
    public void write(Report report, ExportFormat format, OutputStream out) throws IOException {
        switch (format) {
            case XLSX -> toExcel(report, out);
            case PDF -> toPdf(report, out);
        }
    }

    // =========================================================================
    // EXCEL
    // =========================================================================

    /**
     * Excel workbook: one sheet per table, with title lines, a bold aubergine header row that stays
     * frozen while scrolling, number/date formats, auto-sized columns and an optional totals row.
     */
    public void toExcel(Report report, OutputStream out) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(EXCEL_ROW_WINDOW);
        try {
            ExcelStyles styles = new ExcelStyles(workbook);
            String generatedAt = "Generated " + LocalDateTime.now().format(GENERATED_AT);
            for (Table table : report.tables()) {
                writeSheet(workbook, styles, report, table, generatedAt);
            }
            workbook.write(out);
        } finally {
            // Deletes the temp files SXSSF streamed rows into
            workbook.dispose();
            workbook.close();
        }
    }

    private void writeSheet(SXSSFWorkbook workbook, ExcelStyles styles, Report report, Table table, String generatedAt) {
        SXSSFSheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(table.name()));
        sheet.trackAllColumnsForAutoSizing();
        int columnCount = table.headers().size();

        int rowIndex = 0;
        rowIndex = writeTitleLine(sheet, rowIndex, "LoopPay — " + report.title(), styles.title, columnCount);
        rowIndex = writeTitleLine(sheet, rowIndex, generatedAt, styles.note, columnCount);
        rowIndex = writeTitleLine(sheet, rowIndex, "Filters: " + report.filterSummary(), styles.note, columnCount);
        rowIndex++; // blank line before the table

        Row headerRow = sheet.createRow(rowIndex);
        for (int column = 0; column < columnCount; column++) {
            Cell cell = headerRow.createCell(column);
            cell.setCellValue(table.headers().get(column));
            cell.setCellStyle(styles.header);
        }
        // Keep the header row visible while scrolling
        sheet.createFreezePane(0, rowIndex + 1);
        rowIndex++;

        for (List<Object> values : table.rows()) {
            Row row = sheet.createRow(rowIndex++);
            for (int column = 0; column < columnCount; column++) {
                writeExcelCell(row.createCell(column), values.get(column), styles);
            }
        }

        if (table.totals() && !table.rows().isEmpty()) {
            BigDecimal[] totals = columnTotals(table);
            Row totalRow = sheet.createRow(rowIndex);
            for (int column = 0; column < columnCount; column++) {
                Cell cell = totalRow.createCell(column);
                if (column == 0) {
                    cell.setCellValue("Total");
                    cell.setCellStyle(styles.totalLabel);
                } else if (totals[column] != null) {
                    cell.setCellValue(totals[column].doubleValue());
                    cell.setCellStyle(styles.totalMoney);
                } else {
                    cell.setCellStyle(styles.totalLabel);
                }
            }
        }

        // Merged title lines are ignored, so only the table decides the widths; a little padding for the filter arrow
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column, false);
            sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 768, 255 * 256));
        }
    }

    private int writeTitleLine(SXSSFSheet sheet, int rowIndex, String text, CellStyle style, int columnCount) {
        Cell cell = sheet.createRow(rowIndex).createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        if (columnCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnCount - 1));
        }
        return rowIndex + 1;
    }

    private void writeExcelCell(Cell cell, Object value, ExcelStyles styles) {
        if (value == null) {
            cell.setCellStyle(styles.text);
        } else if (value instanceof BigDecimal amount) {
            cell.setCellValue(amount.doubleValue());
            cell.setCellStyle(styles.money);
        } else if (value instanceof Integer || value instanceof Long) {
            cell.setCellValue(((Number) value).doubleValue());
            cell.setCellStyle(styles.count);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
            cell.setCellStyle(styles.date);
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime);
            cell.setCellStyle(styles.dateTime);
        } else {
            cell.setCellValue(value.toString());
            cell.setCellStyle(styles.text);
        }
    }

    /** Cell styles are created once per workbook (Excel limits how many a file may hold). */
    private static final class ExcelStyles {
        final CellStyle title;
        final CellStyle note;
        final CellStyle header;
        final CellStyle text;
        final CellStyle money;
        final CellStyle count;
        final CellStyle date;
        final CellStyle dateTime;
        final CellStyle totalLabel;
        final CellStyle totalMoney;

        ExcelStyles(SXSSFWorkbook workbook) {
            XSSFFont titleFont = (XSSFFont) workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(new XSSFColor(AUBERGINE, null));
            title = workbook.createCellStyle();
            title.setFont(titleFont);

            XSSFFont noteFont = (XSSFFont) workbook.createFont();
            noteFont.setItalic(true);
            noteFont.setColor(new XSSFColor(MUTED_TEXT, null));
            note = workbook.createCellStyle();
            note.setFont(noteFont);

            XSSFFont headerFont = (XSSFFont) workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(AUBERGINE, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header = headerStyle;

            var format = workbook.createDataFormat();
            text = workbook.createCellStyle();
            money = workbook.createCellStyle();
            money.setDataFormat(format.getFormat("#,##0.00"));
            count = workbook.createCellStyle();
            count.setDataFormat(format.getFormat("#,##0"));
            date = workbook.createCellStyle();
            date.setDataFormat(format.getFormat("yyyy-mm-dd"));
            dateTime = workbook.createCellStyle();
            dateTime.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm"));

            XSSFFont boldFont = (XSSFFont) workbook.createFont();
            boldFont.setBold(true);
            totalLabel = workbook.createCellStyle();
            totalLabel.setFont(boldFont);
            totalLabel.setBorderTop(BorderStyle.MEDIUM);
            totalMoney = workbook.createCellStyle();
            totalMoney.cloneStyleFrom(totalLabel);
            totalMoney.setDataFormat(format.getFormat("#,##0.00"));
        }
    }

    // =========================================================================
    // PDF
    // =========================================================================

    /**
     * A4 landscape PDF: "LoopPay" title, generated date and time, applied filters, then each table
     * with an aubergine header (repeated on every page), silver striped rows and a totals row.
     * Every page has a "Page X of Y" footer.
     */
    public void toPdf(Report report, OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 48);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageFooter("LoopPay — " + report.title()));
            document.open();

            document.add(new Paragraph("LoopPay", new Font(Font.HELVETICA, 22, Font.BOLD, AUBERGINE)));
            document.add(new Paragraph(report.title(), new Font(Font.HELVETICA, 13, Font.BOLD, Color.BLACK)));
            Font noteFont = new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED_TEXT);
            document.add(new Paragraph("Generated " + LocalDateTime.now().format(GENERATED_AT), noteFont));
            document.add(new Paragraph("Filters: " + report.filterSummary(), noteFont));

            for (Table table : report.tables()) {
                writePdfTable(document, table, report.tables().size() > 1);
            }
        } catch (DocumentException ex) {
            throw new IOException("Could not build the PDF report", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void writePdfTable(Document document, Table table, boolean showName) throws DocumentException {
        if (showName) {
            Paragraph name = new Paragraph(table.name(), new Font(Font.HELVETICA, 11, Font.BOLD, AUBERGINE));
            name.setSpacingBefore(14);
            document.add(name);
        }

        int columnCount = table.headers().size();
        PdfPTable pdfTable = new PdfPTable(columnCount);
        pdfTable.setWidthPercentage(100);
        pdfTable.setWidths(relativeWidths(table));
        pdfTable.setSpacingBefore(8);
        pdfTable.setHeaderRows(1);
        // Lets the table be added in chunks; rows already written are released from memory
        pdfTable.setComplete(false);

        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        for (String header : table.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(AUBERGINE);
            cell.setPadding(5);
            cell.setBorderColor(AUBERGINE);
            pdfTable.addCell(cell);
        }

        Font bodyFont = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, Color.BLACK);
        int rowNumber = 0;
        for (List<Object> values : table.rows()) {
            Color background = rowNumber % 2 == 1 ? SILVER : Color.WHITE;
            for (Object value : values) {
                pdfTable.addCell(pdfCell(formatForPdf(value), bodyFont, background, isNumeric(value)));
            }
            rowNumber++;
            if (rowNumber % PDF_ROW_CHUNK == 0) {
                document.add(pdfTable);
            }
        }

        if (table.rows().isEmpty()) {
            PdfPCell empty = pdfCell("No records match these filters.", bodyFont, Color.WHITE, false);
            empty.setColspan(columnCount);
            pdfTable.addCell(empty);
        } else if (table.totals()) {
            Font totalFont = new Font(Font.HELVETICA, 9, Font.BOLD, AUBERGINE);
            BigDecimal[] totals = columnTotals(table);
            for (int column = 0; column < columnCount; column++) {
                String text = column == 0 ? "Total" : totals[column] != null ? formatForPdf(totals[column]) : "";
                PdfPCell cell = pdfCell(text, totalFont, Color.WHITE, column > 0);
                cell.setBorderWidthTop(1.5f);
                cell.setBorderColorTop(AUBERGINE);
                pdfTable.addCell(cell);
            }
        }

        pdfTable.setComplete(true);
        document.add(pdfTable);
    }

    private PdfPCell pdfCell(String text, Font font, Color background, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setPadding(4);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(SILVER);
        if (alignRight) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        return cell;
    }

    /** Column widths proportional to the longest text in the header and the first rows (clamped). */
    private float[] relativeWidths(Table table) {
        float[] widths = new float[table.headers().size()];
        for (int column = 0; column < widths.length; column++) {
            int longest = table.headers().get(column).length();
            for (List<Object> row : table.rows().subList(0, Math.min(200, table.rows().size()))) {
                longest = Math.max(longest, formatForPdf(row.get(column)).length());
            }
            widths[column] = Math.max(6, Math.min(longest, 30));
        }
        return widths;
    }

    private String formatForPdf(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal amount) {
            return new DecimalFormat("#,##0.00").format(amount);
        }
        if (value instanceof Integer || value instanceof Long) {
            return new DecimalFormat("#,##0").format(value);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME);
        }
        return value.toString();
    }

    private boolean isNumeric(Object value) {
        return value instanceof Number;
    }

    /** Sum of each BigDecimal column; null for columns that hold no money values. */
    private BigDecimal[] columnTotals(Table table) {
        BigDecimal[] totals = new BigDecimal[table.headers().size()];
        for (List<Object> row : table.rows()) {
            for (int column = 0; column < totals.length; column++) {
                if (row.get(column) instanceof BigDecimal amount) {
                    totals[column] = totals[column] == null ? amount : totals[column].add(amount);
                }
            }
        }
        return totals;
    }

    /**
     * Footer on every page: report name on the left, "Page X of Y" on the right.
     * Y is unknown until the end, so it is drawn into a template that is filled in when the document closes.
     */
    private static final class PageFooter extends PdfPageEventHelper {
        private static final float FONT_SIZE = 8;
        private final String label;
        private BaseFont font;
        private PdfTemplate totalPages;

        PageFooter(String label) {
            this.label = label;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (DocumentException | IOException ex) {
                throw new IllegalStateException("Built-in Helvetica font is unavailable", ex);
            }
            totalPages = writer.getDirectContent().createTemplate(40, FONT_SIZE + 2);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            float y = document.bottom() - 24;
            String pageText = "Page " + writer.getPageNumber() + " of ";
            float pageTextX = document.right() - 40 - font.getWidthPoint(pageText, FONT_SIZE);

            canvas.beginText();
            canvas.setFontAndSize(font, FONT_SIZE);
            canvas.setColorFill(MUTED_TEXT);
            canvas.setTextMatrix(document.left(), y);
            canvas.showText(label);
            canvas.setTextMatrix(pageTextX, y);
            canvas.showText(pageText);
            canvas.endText();
            canvas.addTemplate(totalPages, document.right() - 40, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPages.beginText();
            totalPages.setFontAndSize(font, FONT_SIZE);
            totalPages.setColorFill(MUTED_TEXT);
            totalPages.setTextMatrix(0, 0);
            totalPages.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPages.endText();
        }
    }
}
