package com.accounting.repository;

import com.accounting.model.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, String> {
    
    @Query("SELECT s FROM SyncLog s WHERE s.userId = :userId AND s.version > :lastVersion ORDER BY s.version ASC")
    List<SyncLog> findChanges(@Param("userId") String userId, @Param("lastVersion") Long lastVersion);

    @Query("SELECT COALESCE(MAX(s.version), 0) FROM SyncLog s WHERE s.userId = :userId")
    Long getMaxVersion(@Param("userId") String userId);
}
