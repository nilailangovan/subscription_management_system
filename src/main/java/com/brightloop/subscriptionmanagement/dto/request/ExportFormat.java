package com.brightloop.subscriptionmanagement.dto.request;

import com.brightloop.subscriptionmanagement.exception.BusinessRuleException;

/**
 * File formats a report can be exported in (the ?format= query parameter).
 */
public enum ExportFormat {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("pdf", "application/pdf");

    private final String extension;
    private final String contentType;

    ExportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /**
     * Parses the query parameter case-insensitively ("xlsx", "PDF", ...).
     *
     * @throws BusinessRuleException (400) for anything other than xlsx or pdf
     */
    public static ExportFormat from(String value) {
        for (ExportFormat format : values()) {
            if (format.extension.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new BusinessRuleException("Unsupported export format '" + value + "'. Use xlsx or pdf.");
    }
}
