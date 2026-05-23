package com.cozy.notebooks.exception;

public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }

    public static QuotaExceededException notebookLimit(String planCode, int maxNotebooks) {
        return new QuotaExceededException(
                "Notebook limit reached for plan %s (max %d)".formatted(planCode, maxNotebooks));
    }

    public static QuotaExceededException pageTotalLimit(String planCode, int maxPagesTotal) {
        return new QuotaExceededException(
                "Total page limit reached for plan %s (max %d)".formatted(planCode, maxPagesTotal));
    }

    public static QuotaExceededException pagePerNotebookLimit(String planCode, int maxPagesPerNotebook) {
        return new QuotaExceededException(
                "Page limit per notebook reached for plan %s (max %d)".formatted(planCode, maxPagesPerNotebook));
    }

    public static QuotaExceededException storageLimit(String planCode, long maxStorageBytes) {
        return new QuotaExceededException(
                "Storage limit reached for plan %s (max %d bytes)".formatted(planCode, maxStorageBytes));
    }
}
