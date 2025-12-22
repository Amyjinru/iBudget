package com.accounting.filter;

import com.accounting.model.Transaction;
import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * 筛选规则类
 * 使用策略模式实现多条件过滤
 */
public class FilterRule {
    private Predicate<Transaction> predicate;
    private String description;
    
    private FilterRule(Predicate<Transaction> predicate, String description) {
        this.predicate = predicate;
        this.description = description;
    }
    
    public boolean test(Transaction transaction) {
        return predicate.test(transaction);
    }
    
    public String getDescription() {
        return description;
    }
    
    // 按日期范围筛选
    public static FilterRule dateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return new FilterRule(
            t -> {
                if (t.getDate() == null) return false;
                return !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate);
            },
            "日期范围: " + startDate + " 至 " + endDate
        );
    }
    
    // 按分类筛选
    public static FilterRule byCategory(String categoryId) {
        return new FilterRule(
            t -> categoryId == null || categoryId.equals(t.getCategoryId()),
            "分类: " + categoryId
        );
    }
    
    // 按类型筛选（支出/收入）
    public static FilterRule byType(Transaction.TransactionType type) {
        return new FilterRule(
            t -> type == null || type.equals(t.getType()),
            "类型: " + (type != null ? type.getDisplayName() : "全部")
        );
    }
    
    // 按金额范围筛选
    public static FilterRule amountRange(double minAmount, double maxAmount) {
        return new FilterRule(
            t -> t.getAmount() >= minAmount && t.getAmount() <= maxAmount,
            "金额范围: " + minAmount + " - " + maxAmount
        );
    }
    
    // 按关键字筛选（描述、标签）
    public static FilterRule byKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new FilterRule(t -> true, "关键字: 无");
        }
        String lowerKeyword = keyword.toLowerCase();
        return new FilterRule(
            t -> {
                boolean matchDescription = t.getDescription() != null && 
                    t.getDescription().toLowerCase().contains(lowerKeyword);
                boolean matchTags = t.getTags() != null && 
                    t.getTags().toLowerCase().contains(lowerKeyword);
                return matchDescription || matchTags;
            },
            "关键字: " + keyword
        );
    }
    
    // 组合多个规则（AND逻辑）
    public FilterRule and(FilterRule other) {
        return new FilterRule(
            this.predicate.and(other.predicate),
            this.description + " AND " + other.description
        );
    }
    
    // 组合多个规则（OR逻辑）
    public FilterRule or(FilterRule other) {
        return new FilterRule(
            this.predicate.or(other.predicate),
            this.description + " OR " + other.description
        );
    }
    
    // 取反
    public FilterRule negate() {
        return new FilterRule(
            this.predicate.negate(),
            "NOT (" + this.description + ")"
        );
    }
}

