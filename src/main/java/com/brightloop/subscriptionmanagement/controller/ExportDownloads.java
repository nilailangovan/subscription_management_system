package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.request.ExportFormat;
import com.brightloop.subscriptionmanagement.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Sends a report as a file download, shared by every export endpoint.
 */
final class ExportDownloads {

    private ExportDownloads() {
    }

    /**
     * Streams the file straight into the HTTP response with its content type and an attachment
     * file name such as looppay-invoices-2026-10-31.xlsx. Answers 204 No Content when the report has no rows.
     */
    static void send(HttpServletResponse response, ExportService exportService,
                     ExportService.Report report, ExportFormat format) throws IOException {
        if (report.isEmpty()) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }
        String fileName = "looppay-" + report.fileName() + "-" + LocalDate.now() + "." + format.extension();
        response.setContentType(format.contentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString());
        exportService.write(report, format, response.getOutputStream());
    }
}
