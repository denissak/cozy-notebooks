package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {

    @Query("""
            SELECT COALESCE(SUM(a.byteSize), 0)
            FROM AssetEntity a
            WHERE a.userId = :userId AND a.deletedAt IS NULL
            """)
    long sumByteSizeByUserIdAndDeletedAtIsNull(@Param("userId") UUID userId);
}
