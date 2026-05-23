package com.cozy.notebooks.service;

import com.cozy.notebooks.domain.UserActivityLogEntity;
import com.cozy.notebooks.repository.UserActivityLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class UserActivityLogWriter {

    private final UserActivityLogRepository repository;

    UserActivityLogWriter(UserActivityLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void save(UserActivityLogEntity entry) {
        repository.save(entry);
    }
}
