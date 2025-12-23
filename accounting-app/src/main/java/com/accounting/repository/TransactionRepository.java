package com.accounting.repository;

import com.accounting.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByUserId(String userId);
    List<Transaction> findByCategoryId(String categoryId);
    
    // 使用@Param确保JPA命名参数绑定正确
    @Query("SELECT t FROM Transaction t WHERE t.date BETWEEN :startDate AND :endDate")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 包含历史公共记录（userId为null）以兼容旧数据
    @Query("SELECT t FROM Transaction t WHERE t.userId IS NULL OR t.userId = :userId")
    List<Transaction> findVisibleForUser(@Param("userId") String userId);
    
    // 原子更新：仅当传入updatedAt比数据库中的更新时才更新
    @Modifying
    @Query("""
        UPDATE Transaction t SET 
            t.type = :type,
            t.amount = :amount,
            t.categoryId = :categoryId,
            t.description = :description,
            t.date = :date,
            t.updatedAt = :updatedAt,
            t.tags = :tags,
            t.userId = :userId
        WHERE t.id = :id AND (t.updatedAt IS NULL OR t.updatedAt < :updatedAt)
        """)
    int updateIfNewer(
            @Param("id") String id,
            @Param("userId") String userId,
            @Param("type") Transaction.TransactionType type,
            @Param("amount") double amount,
            @Param("categoryId") String categoryId,
            @Param("description") String description,
            @Param("date") LocalDateTime date,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("tags") String tags
    );
}
