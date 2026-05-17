package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, UUID> {

    Optional<UserIdentityEntity> findByProviderAndProviderSubjectAndDeletedAtIsNull(String provider,
                                                                                     String providerSubject);
}
