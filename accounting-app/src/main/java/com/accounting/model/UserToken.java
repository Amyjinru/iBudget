package com.accounting.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_tokens")
public class UserToken {
    @Id
    private String id;
    
    private String userId;
    private String token; // The refresh token string
    private String deviceId;
    private LocalDateTime expiryDate;
    
    public UserToken() {
        this.id = UUID.randomUUID().toString();
    }
    
    public UserToken(String userId, String token, String deviceId, LocalDateTime expiryDate) {
        this();
        this.userId = userId;
        this.token = token;
        this.deviceId = deviceId;
        this.expiryDate = expiryDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
}
