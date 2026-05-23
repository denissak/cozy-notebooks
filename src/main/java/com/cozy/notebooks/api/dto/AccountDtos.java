package com.cozy.notebooks.api.dto;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record AccountLimitsResponse(
            String plan,
            PlanLimitsResponse limits,
            PlanUsageResponse usage
    ) {
    }

    public record PlanLimitsResponse(
            int maxStorageMb,
            long maxStorageBytes,
            int maxNotebooks,
            int maxPagesPerNotebook,
            int maxPagesTotal,
            boolean syncEnabled
    ) {
    }

    public record PlanUsageResponse(
            long storageBytesUsed,
            long storageMbUsed,
            long notebooksUsed,
            long pagesUsedTotal
    ) {
    }
}
