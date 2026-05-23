package com.cozy.notebooks.domain;

import com.cozy.notebooks.exception.BadRequestException;

/**
 * Hardcoded plan definitions for MVP. Limits are not stored in the database yet.
 */
public enum UserPlan {

    FREE("free", 25, 3, 25, 75, true),
    PRO("pro", 300, 20, 100, 2000, true);

    private final String code;
    private final int maxStorageMb;
    private final int maxNotebooks;
    private final int maxPagesPerNotebook;
    private final int maxPagesTotal;
    private final boolean syncEnabled;

    UserPlan(String code,
             int maxStorageMb,
             int maxNotebooks,
             int maxPagesPerNotebook,
             int maxPagesTotal,
             boolean syncEnabled) {
        this.code = code;
        this.maxStorageMb = maxStorageMb;
        this.maxNotebooks = maxNotebooks;
        this.maxPagesPerNotebook = maxPagesPerNotebook;
        this.maxPagesTotal = maxPagesTotal;
        this.syncEnabled = syncEnabled;
    }

    public String code() {
        return code;
    }

    public int maxStorageMb() {
        return maxStorageMb;
    }

    public long maxStorageBytes() {
        return (long) maxStorageMb * 1024L * 1024L;
    }

    public int maxNotebooks() {
        return maxNotebooks;
    }

    public int maxPagesPerNotebook() {
        return maxPagesPerNotebook;
    }

    public int maxPagesTotal() {
        return maxPagesTotal;
    }

    public boolean syncEnabled() {
        return syncEnabled;
    }

    public static UserPlan fromCode(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return FREE;
        }
        for (UserPlan plan : values()) {
            if (plan.code.equalsIgnoreCase(planCode.trim())) {
                return plan;
            }
        }
        throw new BadRequestException("Unknown plan: " + planCode);
    }
}
