package com.accounting.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
// 使用v2表名并采用UUID主键，解决SQLite自增插入兼容问题
@Table(name = "sync_log_v2")
public class SyncLog {
    @Id
    // 主键改为UUID字符串，避免数据库自增冲突
    private String id;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action; // ADD, UPDATE, DELETE

    @Column(nullable = false)
    private String entityType; // "Transaction", "User", etc.

    @Column(columnDefinition = "TEXT")
    private String payload; // JSON representation of the change

    @Column(nullable = false)
    private Long version; // 版本游标，用于增量同步

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public enum Action {
        ADD, UPDATE, DELETE
    }

    public SyncLog() {}

    public SyncLog(String entityId, String userId, Action action, String entityType, String payload, Long version) {
        // 构造时生成UUID主键，并记录当前时间戳
        this.id = java.util.UUID.randomUUID().toString();
        this.entityId = entityId;
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.payload = payload;
        this.version = version;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
