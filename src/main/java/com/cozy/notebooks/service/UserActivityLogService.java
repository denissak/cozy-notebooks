package com.cozy.notebooks.service;

import com.cozy.notebooks.domain.UserActivityLogEntity;
import com.cozy.notebooks.logging.MdcKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Service
public class UserActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityLogService.class);

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILURE = "failure";

    private final UserActivityLogWriter writer;
    private final ObjectMapper objectMapper;

    public UserActivityLogService(UserActivityLogWriter writer, ObjectMapper objectMapper) {
        this.writer = writer;
        this.objectMapper = objectMapper;
    }

    public void logSuccess(UUID userId,
                           String action,
                           String entityType,
                           UUID entityId,
                           Map<String, ?> metadata) {
        write(userId, action, entityType, entityId, STATUS_SUCCESS, metadata, true);
    }

    public void logFailure(UUID userId,
                           String action,
                           String entityType,
                           UUID entityId,
                           Map<String, ?> metadata) {
        write(userId, action, entityType, entityId, STATUS_FAILURE, metadata, false);
    }

    /**
     * @param afterCommit when true, persist after the surrounding transaction commits (for FK safety);
     *                    when false, persist immediately (for failures that abort the transaction).
     */
    private void write(UUID userId,
                       String action,
                       String entityType,
                       UUID entityId,
                       String status,
                       Map<String, ?> metadata,
                       boolean afterCommit) {
        try {
            UserActivityLogEntity entry = buildEntry(userId, action, entityType, entityId, status, metadata);
            if (afterCommit && TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        saveEntry(entry, action, status);
                    }
                });
            } else {
                saveEntry(entry, action, status);
            }
        } catch (Exception ex) {
            log.warn("Failed to schedule user activity log action={} status={}", action, status, ex);
        }
    }

    private void saveEntry(UserActivityLogEntity entry, String action, String status) {
        try {
            writer.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to persist user activity log action={} status={}", action, status, ex);
        }
    }

    private UserActivityLogEntity buildEntry(UUID userId,
                                             String action,
                                             String entityType,
                                             UUID entityId,
                                             String status,
                                             Map<String, ?> metadata) {
        RequestContext context = resolveRequestContext();
        JsonNode metadataJson = metadata == null || metadata.isEmpty()
                ? null
                : objectMapper.valueToTree(metadata);

        return UserActivityLogEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .status(status)
                .metadataJson(metadataJson)
                .ipAddress(context.ipAddress())
                .userAgent(context.userAgent())
                .requestId(context.requestId())
                .build();
    }

    private RequestContext resolveRequestContext() {
        String requestId = MDC.get(MdcKeys.REQUEST_ID);
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new RequestContext(requestId, null, null);
        }
        HttpServletRequest request = attributes.getRequest();
        return new RequestContext(
                requestId,
                truncate(request.getRemoteAddr(), 64),
                truncate(request.getHeader("User-Agent"), 512));
    }

    private static String truncate(String value, int maxLen) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private record RequestContext(String requestId, String ipAddress, String userAgent) {
    }
}
