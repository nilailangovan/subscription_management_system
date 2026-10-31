package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.request.ExportFormat;
import com.brightloop.subscriptionmanagement.dto.request.RecordPaymentRequestDto;
import com.brightloop.subscriptionmanagement.dto.request.ReportFilterDto;
import com.brightloop.subscriptionmanagement.dto.response.ApiResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.PaymentResponseDto;
import com.brightloop.subscriptionmanagement.service.ExportService;
import com.brightloop.subscriptionmanagement.service.PaymentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Payments for the admin console (ADMIN and STAFF): record, list and export.
 * List and export accept the same filters; the date range applies to the paid date.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Payments", description = "Record, list and export payments (ADMIN & STAFF)")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentService paymentService;
    private final ReportService reportService;
    private final ExportService exportService;

    @PostMapping
    @Operation(summary = "Record a full payment for a sent invoice; the invoice becomes PAID")
    public ResponseEntity<ApiResponseDto<PaymentResponseDto>> recordPayment(@Valid @RequestBody RecordPaymentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Payment recorded successfully", paymentService.recordPayment(request)));
    }

    @GetMapping
    @Operation(summary = "List payments with filters, newest first")
    public ResponseEntity<ApiResponseDto<Page<PaymentResponseDto>>> getPayments(
            @Valid ReportFilterDto filter,
            @PageableDefault(size = 10, sort = "paidDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto.success("Payments retrieved successfully",
                reportService.listPayments(filter, pageable)));
    }

    @GetMapping("/export")
    @Operation(summary = "Download the filtered payments as xlsx or pdf (204 when nothing matches, 400 above 50,000 rows)")
    public void exportPayments(
            @Valid ReportFilterDto filter,
            @RequestParam(defaultValue = "xlsx") String format,
            @SortDefault(sort = "paidDate", direction = Sort.Direction.DESC) Sort sort,
            HttpServletResponse response) throws IOException {
        ExportFormat exportFormat = ExportFormat.from(format);
        ExportDownloads.send(response, exportService, reportService.paymentsReport(filter, sort), exportFormat);
    }
}
