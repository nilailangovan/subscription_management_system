package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.request.ExportFormat;
import com.brightloop.subscriptionmanagement.dto.request.ReportFilterDto;
import com.brightloop.subscriptionmanagement.dto.response.ApiResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.InvoiceResponseDto;
import com.brightloop.subscriptionmanagement.service.ExportService;
import com.brightloop.subscriptionmanagement.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Invoice list and export for the admin console (ADMIN and STAFF).
 * Both accept the same filters (from, to, search, customerId, invoiceStatus) and sort;
 * the export ignores pagination.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Invoices", description = "List and export invoices (ADMIN & STAFF)")
@SecurityRequirement(name = "Bearer Authentication")
public class InvoiceController {

    private final ReportService reportService;
    private final ExportService exportService;

    @GetMapping
    @Operation(summary = "List invoices with filters; the date range applies to the issue date")
    public ResponseEntity<ApiResponseDto<Page<InvoiceResponseDto>>> getInvoices(
            @Valid ReportFilterDto filter,
            @PageableDefault(size = 10, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto.success("Invoices retrieved successfully",
                reportService.listInvoices(filter, pageable)));
    }

    @GetMapping("/export")
    @Operation(summary = "Download the filtered invoices as xlsx or pdf (204 when nothing matches, 400 above 50,000 rows)")
    public void exportInvoices(
            @Valid ReportFilterDto filter,
            @RequestParam(defaultValue = "xlsx") String format,
            @SortDefault(sort = "issueDate", direction = Sort.Direction.DESC) Sort sort,
            HttpServletResponse response) throws IOException {
        ExportFormat exportFormat = ExportFormat.from(format);
        ExportDownloads.send(response, exportService, reportService.invoicesReport(filter, sort), exportFormat);
    }
}
