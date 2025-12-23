package com.accounting.model;

import com.google.gson.annotations.SerializedName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 预算实体类
 * 支持月度预算设置
 */
@Entity
@Table(name = "budgets")
public class Budget {
    @Id
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("categoryId")
    private String categoryId; // null表示总预算
    
    @SerializedName("amount")
    private double amount; // 预算金额
    
    @SerializedName("year")
    private int year;
    
    @SerializedName("month")
    private int month; // 1-12
    
    @SerializedName("createdAt")
    private LocalDateTime createdAt;
    
    @SerializedName("updatedAt")
    private LocalDateTime updatedAt;

    // 新增周期式预算支持（向后兼容现有 year/month 字段）
    @SerializedName("startDate")
    @Column(name = "start_date")
    private LocalDate startDate;

    @SerializedName("periodUnit")
    @Column(name = "period_unit")
    @Enumerated(EnumType.STRING)
    private PeriodUnit periodUnit;

    @SerializedName("periodCount")
    @Column(name = "period_count")
    private Integer periodCount;
    
    public Budget() {
        this.id = UUID.randomUUID().toString();
        LocalDate now = LocalDate.now();
        this.year = now.getYear();
        this.month = now.getMonthValue();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Budget(String userId, String categoryId, double amount, int year, int month) {
        this();
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.year = year;
        this.month = month;
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
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    public int getMonth() {
        return month;
    }
    
    public void setMonth(int month) {
        this.month = month;
    }
    
    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 检查是否为总预算（非分类预算）
     */
    public boolean isTotalBudget() {
        return categoryId == null || categoryId.isEmpty();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public PeriodUnit getPeriodUnit() {
        return periodUnit;
    }

    public void setPeriodUnit(PeriodUnit periodUnit) {
        this.periodUnit = periodUnit;
    }

    public Integer getPeriodCount() {
        return periodCount;
    }

    public void setPeriodCount(Integer periodCount) {
        this.periodCount = periodCount;
    }

    public enum PeriodUnit {
        DAYS, WEEKS, MONTHS, YEARS
    }

    /**
     * 预算结束日期（包含）
     */
    public LocalDate getEndDate() {
        if (startDate == null || periodUnit == null || periodCount == null || periodCount <= 0) return null;
        switch (periodUnit) {
            case DAYS:
                return startDate.plusDays(periodCount - 1);
            case WEEKS:
                return startDate.plusWeeks(periodCount).minusDays(1);
            case MONTHS:
                return startDate.plusMonths(periodCount).minusDays(1);
            case YEARS:
                return startDate.plusYears(periodCount).minusDays(1);
            default:
                return null;
        }
    }

    /**
     * 预算总天数（含首尾）
     */
    public long getTotalDays() {
        LocalDate end = getEndDate();
        if (startDate == null || end == null) return 0;
        return ChronoUnit.DAYS.between(startDate, end) + 1;
    }
    
    @Override
    public String toString() {
        return "Budget{" +
                "id='" + id + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", amount=" + amount +
                ", year=" + year +
                ", month=" + month +
                '}';
    }
}

