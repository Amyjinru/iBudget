package com.accounting.model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * 预算实体类
 * 支持月度预算设置
 */
public class Budget {
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("categoryId")
    private String categoryId; // null表示总预算
    
    @SerializedName("amount")
    private double amount; // 预算金额（整个周期总额）

    /**
     * 预算周期类型：按日 / 周 / 月 / 年
     */
    public enum PeriodType {
        DAY, WEEK, MONTH, YEAR
    }

    @SerializedName("periodType")
    private PeriodType periodType = PeriodType.MONTH;

    /**
     * 周期的起始日期（包含）—— 使用公历 LocalDate
     */
    @SerializedName("startDate")
    private LocalDate startDate;

    /**
     * 兼容原有“按月份”预算的字段（如果存在则优先用于 UI 的按月展示）
     */
    @SerializedName("year")
    private int year;
    
    @SerializedName("month")
    private int month; // 1-12
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("updatedAt")
    private String updatedAt;
    
    public Budget() {
        this.id = UUID.randomUUID().toString();
        LocalDate now = LocalDate.now();
        this.year = now.getYear();
        this.month = now.getMonthValue();
        this.startDate = now.withDayOfMonth(1);
        this.periodType = PeriodType.MONTH;
    }
    
    public Budget(String userId, String categoryId, double amount, int year, int month) {
        this();
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.year = year;
        this.month = month;
        // 默认按月预算，以该月第一天为周期起点
        this.periodType = PeriodType.MONTH;
        this.startDate = YearMonth.of(year, month).atDay(1);
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

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 检查是否为总预算（非分类预算）
     */
    public boolean isTotalBudget() {
        return categoryId == null || categoryId.isEmpty();
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

