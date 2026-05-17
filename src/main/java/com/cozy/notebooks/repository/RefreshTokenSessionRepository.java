package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.RefreshTokenSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSessionEntity, UUID> {

    Optional<RefreshTokenSessionEntity> findByRefreshTokenHash(String refreshTokenHash);
}
