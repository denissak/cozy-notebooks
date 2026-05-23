package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.AccountDtos.AccountLimitsResponse;
import com.cozy.notebooks.api.dto.AccountDtos.PlanLimitsResponse;
import com.cozy.notebooks.api.dto.AccountDtos.PlanUsageResponse;
import com.cozy.notebooks.domain.UserEntity;
import com.cozy.notebooks.domain.UserPlan;
import com.cozy.notebooks.exception.NotFoundException;
import com.cozy.notebooks.exception.QuotaExceededException;
import com.cozy.notebooks.repository.AssetRepository;
import com.cozy.notebooks.repository.NotebookRepository;
import com.cozy.notebooks.repository.PageRepository;
import com.cozy.notebooks.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserPlanLimitsService {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final UserRepository userRepository;
    private final NotebookRepository notebookRepository;
    private final PageRepository pageRepository;
    private final AssetRepository assetRepository;

    public UserPlanLimitsService(UserRepository userRepository,
                                 NotebookRepository notebookRepository,
                                 PageRepository pageRepository,
                                 AssetRepository assetRepository) {
        this.userRepository = userRepository;
        this.notebookRepository = notebookRepository;
        this.pageRepository = pageRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public AccountLimitsResponse getAccountLimits(UUID userId) {
        UserEntity user = requireUser(userId);
        UserPlan plan = UserPlan.fromCode(user.getPlanCode());
        return new AccountLimitsResponse(
                plan.code(),
                toLimitsResponse(plan),
                computeUsage(userId)
        );
    }

    @Transactional(readOnly = true)
    public PlanLimitsResponse limitsForUser(UUID userId) {
        UserEntity user = requireUser(userId);
        return toLimitsResponse(UserPlan.fromCode(user.getPlanCode()));
    }

    @Transactional(readOnly = true)
    public void validateCanCreateNotebook(UUID userId) {
        UserEntity user = requireUser(userId);
        UserPlan plan = UserPlan.fromCode(user.getPlanCode());
        long notebooksUsed = notebookRepository.countByUserIdAndDeletedAtIsNull(userId);
        if (notebooksUsed >= plan.maxNotebooks()) {
            throw QuotaExceededException.notebookLimit(plan.code(), plan.maxNotebooks());
        }
    }

    @Transactional(readOnly = true)
    public void validateCanCreatePage(UUID userId, UUID notebookId) {
        UserEntity user = requireUser(userId);
        UserPlan plan = UserPlan.fromCode(user.getPlanCode());

        long pagesTotal = pageRepository.countByUserIdAndDeletedAtIsNull(userId);
        if (pagesTotal >= plan.maxPagesTotal()) {
            throw QuotaExceededException.pageTotalLimit(plan.code(), plan.maxPagesTotal());
        }

        long pagesInNotebook = pageRepository.countByUserIdAndNotebookIdAndDeletedAtIsNull(userId, notebookId);
        if (pagesInNotebook >= plan.maxPagesPerNotebook()) {
            throw QuotaExceededException.pagePerNotebookLimit(plan.code(), plan.maxPagesPerNotebook());
        }
    }

    @Transactional(readOnly = true)
    public void validateCanCreateAsset(UUID userId, long additionalBytes) {
        UserEntity user = requireUser(userId);
        UserPlan plan = UserPlan.fromCode(user.getPlanCode());
        long storageUsed = assetRepository.sumByteSizeByUserIdAndDeletedAtIsNull(userId);
        if (storageUsed + additionalBytes > plan.maxStorageBytes()) {
            throw QuotaExceededException.storageLimit(plan.code(), plan.maxStorageBytes());
        }
    }

    private PlanUsageResponse computeUsage(UUID userId) {
        long storageBytesUsed = assetRepository.sumByteSizeByUserIdAndDeletedAtIsNull(userId);
        long storageMbUsed = storageBytesUsed / BYTES_PER_MB;
        long notebooksUsed = notebookRepository.countByUserIdAndDeletedAtIsNull(userId);
        long pagesUsedTotal = pageRepository.countByUserIdAndDeletedAtIsNull(userId);
        return new PlanUsageResponse(storageBytesUsed, storageMbUsed, notebooksUsed, pagesUsedTotal);
    }

    private static PlanLimitsResponse toLimitsResponse(UserPlan plan) {
        return new PlanLimitsResponse(
                plan.maxStorageMb(),
                plan.maxStorageBytes(),
                plan.maxNotebooks(),
                plan.maxPagesPerNotebook(),
                plan.maxPagesTotal(),
                plan.syncEnabled()
        );
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> NotFoundException.of("User", userId));
    }
}
