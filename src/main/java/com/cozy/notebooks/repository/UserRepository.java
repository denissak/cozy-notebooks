package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<UserEntity> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}
