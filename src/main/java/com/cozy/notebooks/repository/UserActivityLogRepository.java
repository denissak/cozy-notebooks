package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.UserActivityLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLogEntity, UUID> {

    List<UserActivityLogEntity> findByActionAndUserIdOrderByCreatedAtDesc(String action, UUID userId);

    List<UserActivityLogEntity> findByActionOrderByCreatedAtDesc(String action);
}
