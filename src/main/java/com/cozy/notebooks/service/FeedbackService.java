package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.FeedbackDtos.CreateFeedbackRequest;
import com.cozy.notebooks.api.dto.FeedbackDtos.FeedbackResponse;
import com.cozy.notebooks.domain.FeedbackEntity;
import com.cozy.notebooks.repository.FeedbackRepository;
import com.cozy.notebooks.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserActivityLogService activityLogService;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           CurrentUserProvider currentUserProvider,
                           UserActivityLogService activityLogService) {
        this.feedbackRepository = feedbackRepository;
        this.currentUserProvider = currentUserProvider;
        this.activityLogService = activityLogService;
    }

    public FeedbackResponse create(CreateFeedbackRequest request) {
        UUID userId = currentUserProvider.requireId();
        FeedbackEntity entity = FeedbackEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .feedbackType(request.type())
                .message(request.message())
                .status("new")
                .build();
        FeedbackEntity saved = feedbackRepository.save(entity);
        activityLogService.logSuccess(userId, UserActivityActions.FEEDBACK_CREATE, "feedback", saved.getId(),
                Map.of("type", saved.getFeedbackType()));
        return new FeedbackResponse(
                saved.getId(),
                saved.getFeedbackType(),
                saved.getMessage(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }
}
