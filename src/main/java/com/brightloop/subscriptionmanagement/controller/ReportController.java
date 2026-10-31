package com.brightloop.subscriptionmanagement.controller;

import com.brightloop.subscriptionmanagement.dto.request.ExportFormat;
import com.brightloop.subscriptionmanagement.dto.request.ReportFilterDto;
import com.brightloop.subscriptionmanagement.dto.response.ApiResponseDto;
import com.brightloop.subscriptionmanagement.dto.response.DashboardSummaryDto;
import com.brightloop.subscriptionmanagement.dto.response.SubscriptionResponseDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Dashboard KPIs, the filtered subscription list and their exports (ADMIN and STAFF).
 * Paths are absolute because the subscription export sits under /api/subscriptions.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Reports", description = "Dashboard KPIs, filtered lists and exports (ADMIN & STAFF)")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;

    @GetMapping("/api/reports/dashboard")
    @Operation(summary = "Dashboard KPIs: collected revenue in the date range; receivables and subscriptions as of today")
    public ResponseEntity<ApiResponseDto<DashboardSummaryDto>> getDashboard(@Valid ReportFilterDto filter) {
        return ResponseEntity.ok(ApiResponseDto.success("Dashboard retrieved successfully", reportService.dashboardSummary(filter)));
    }

    @GetMapping("/api/reports/export/dashboard")
    @Operation(summary = "Download the dashboard KPIs and its invoice table (overdue by default) as xlsx or pdf")
    public void exportDashboard(
            @Valid ReportFilterDto filter,
            @RequestParam(defaultValue = "xlsx") String format,
            HttpServletResponse response) throws IOException {
        ExportFormat exportFormat = ExportFormat.from(format);
        ExportDownloads.send(response, exportService, reportService.dashboardReport(filter), exportFormat);
    }

    @GetMapping("/api/reports/subscriptions")
    @Operation(summary = "List subscriptions with filters (search also matches customer name and email); date range = last updated")
    public ResponseEntity<ApiResponseDto<Page<SubscriptionResponseDto>>> getSubscriptions(
            @Valid ReportFilterDto filter,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto.success("Subscriptions retrieved successfully",
                reportService.listSubscriptions(filter, pageable)));
    }

    @GetMapping("/api/subscriptions/export")
    @Operation(summary = "Download the filtered subscriptions as xlsx or pdf (204 when nothing matches, 400 above 50,000 rows)")
    public void exportSubscriptions(
            @Valid ReportFilterDto filter,
            @RequestParam(defaultValue = "xlsx") String format,
            @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Sort sort,
            HttpServletResponse response) throws IOException {
        ExportFormat exportFormat = ExportFormat.from(format);
        ExportDownloads.send(response, exportService, reportService.subscriptionsReport(filter, sort), exportFormat);
    }
}
