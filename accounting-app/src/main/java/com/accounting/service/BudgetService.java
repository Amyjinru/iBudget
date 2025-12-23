package com.accounting.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.accounting.model.Budget;
import com.accounting.model.Transaction;
import com.accounting.storage.StorageManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * 预算服务类
 * 提供预算的增删改查和超额提醒功能
 */
public class BudgetService {
    private static final String BUDGETS_FILE = "budgets.json";
    private StorageManager storageManager;
    private Gson gson;
    private List<Budget> budgets;
    private TransactionService transactionService;
    
    public BudgetService(StorageManager storageManager, TransactionService transactionService) {
        this.storageManager = storageManager;
        this.transactionService = transactionService;
        this.gson = new Gson();
        this.budgets = new ArrayList<>();
        loadBudgets();
    }
    
    /**
     * 添加预算
     */
    public Budget addBudget(Budget budget) {
        if (budget.getId() == null || budget.getId().isEmpty()) {
            budget.setId(java.util.UUID.randomUUID().toString());
        }
        budgets.add(budget);
        saveBudgets();
        return budget;
    }
    
    /**
     * 删除预算
     */
    public boolean deleteBudget(String budgetId) {
        boolean removed = budgets.removeIf(b -> b.getId().equals(budgetId));
        if (removed) {
            saveBudgets();
        }
        return removed;
    }
    
    /**
     * 更新预算
     */
    public Budget updateBudget(String budgetId, Budget updatedBudget) {
        for (int i = 0; i < budgets.size(); i++) {
            if (budgets.get(i).getId().equals(budgetId)) {
                updatedBudget.setId(budgetId);
                budgets.set(i, updatedBudget);
                saveBudgets();
                return updatedBudget;
            }
        }
        return null;
    }
    
    /**
     * 根据ID查询预算
     */
    public Budget getBudgetById(String budgetId) {
        return budgets.stream()
            .filter(b -> b.getId().equals(budgetId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取指定月份的总预算
     */
    public Budget getTotalBudget(String userId, int year, int month) {
        return budgets.stream()
            .filter(b -> b.getUserId().equals(userId))
            .filter(b -> b.getYear() == year && b.getMonth() == month)
            .filter(Budget::isTotalBudget)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取指定月份的分类预算
     */
    public Budget getCategoryBudget(String userId, String categoryId, int year, int month) {
        return budgets.stream()
            .filter(b -> b.getUserId().equals(userId))
            .filter(b -> b.getCategoryId() != null && b.getCategoryId().equals(categoryId))
            .filter(b -> b.getYear() == year && b.getMonth() == month)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取用户的所有预算
     */
    public List<Budget> getBudgetsByUserId(String userId) {
        return budgets.stream()
            .filter(b -> b.getUserId().equals(userId))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取指定月份的预算列表
     */
    public List<Budget> getBudgetsByMonth(String userId, int year, int month) {
        return budgets.stream()
            .filter(b -> b.getUserId().equals(userId))
            .filter(b -> b.getYear() == year && b.getMonth() == month)
            .collect(Collectors.toList());
    }
    
    /**
     * 计算已用金额
     */
    public double calculateUsedAmount(String userId, String categoryId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        
        return transactions.stream()
            .filter(t -> {
                if (t.getDate() == null) return false;
                LocalDate transactionDate = t.getDate().toLocalDate();
                return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
            })
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> categoryId == null || categoryId.equals(t.getCategoryId()))
            .mapToDouble(Transaction::getAmount)
            .sum();
    }
    
    /**
     * 检查是否超额
     */
    public boolean isOverBudget(String userId, String categoryId, int year, int month) {
        Budget budget = categoryId == null ? 
            getTotalBudget(userId, year, month) : 
            getCategoryBudget(userId, categoryId, year, month);
        
        if (budget == null) {
            return false;
        }
        
        double usedAmount = calculateUsedAmount(userId, categoryId, year, month);
        return usedAmount > budget.getAmount();
    }
    
    /**
     * 获取超额金额
     */
    public double getOverBudgetAmount(String userId, String categoryId, int year, int month) {
        Budget budget = categoryId == null ? 
            getTotalBudget(userId, year, month) : 
            getCategoryBudget(userId, categoryId, year, month);
        
        if (budget == null) {
            return 0;
        }
        
        double usedAmount = calculateUsedAmount(userId, categoryId, year, month);
        double overAmount = usedAmount - budget.getAmount();
        return Math.max(0, overAmount);
    }
    
    /**
     * 获取预算使用率（0-1之间）
     */
    public double getBudgetUsageRate(String userId, String categoryId, int year, int month) {
        Budget budget = categoryId == null ? 
            getTotalBudget(userId, year, month) : 
            getCategoryBudget(userId, categoryId, year, month);
        
        if (budget == null || budget.getAmount() == 0) {
            return 0;
        }
        
        double usedAmount = calculateUsedAmount(userId, categoryId, year, month);
        return Math.min(1.0, usedAmount / budget.getAmount());
    }
    
    /**
     * 设置月度预算
     */
    public Budget setMonthlyBudget(String userId, String categoryId, double amount, int year, int month) {
        Budget existingBudget = categoryId == null ? 
            getTotalBudget(userId, year, month) : 
            getCategoryBudget(userId, categoryId, year, month);
        
        if (existingBudget != null) {
            existingBudget.setAmount(amount);
            saveBudgets();
            return existingBudget;
        } else {
            Budget newBudget = new Budget(userId, categoryId, amount, year, month);
            return addBudget(newBudget);
        }
    }
    
    /**
     * 计算某条预算当前的统计信息
     */
    public BudgetStats calculateStats(Budget b) {
        BudgetStats s = new BudgetStats();
        s.budget = b;
        if (b.getStartDate() == null) return s;
        LocalDate now = LocalDate.now();
        LocalDate end = b.getEndDate();
        LocalDate last = end == null ? now : (now.isBefore(end) ? now : end);

        long daysElapsed = Math.max(1, ChronoUnit.DAYS.between(b.getStartDate(), last) + 1);
        long totalDays = Math.max(1, b.getTotalDays());
        double amountSpent = calculateAmountSpentForBudget(b);
        double remaining = b.getAmount() - amountSpent;

        s.daysElapsed = daysElapsed;
        s.totalDays = totalDays;
        s.amountSpent = amountSpent;
        s.remaining = remaining;
        s.avgPerDayBudget = b.getAmount() / (double) totalDays;
        s.avgPerDayActual = amountSpent / (double) daysElapsed;
        s.projectedTotalByAvgSoFar = s.avgPerDayActual * (double) totalDays;
        s.projectedRemainingByAvgSoFar = b.getAmount() - s.projectedTotalByAvgSoFar;
        s.willBeOverspentByAvg = s.projectedTotalByAvgSoFar > b.getAmount();

        if (daysElapsed >= 7) {
            LocalDate from7 = now.minusDays(6);
            s.last7DaysSpent = calculateAmountSpentInRange(b, from7, now);
        }
        if (daysElapsed >= 30) {
            LocalDate from30 = now.minusDays(29);
            s.last30DaysSpent = calculateAmountSpentInRange(b, from30, now);
        }
        return s;
    }
    
    /**
     * 计算预算期间内已花费的金额
     */
    private double calculateAmountSpentForBudget(Budget b) {
        try {
            String json = storageManager.readFile("transactions.json");
            if (json == null || json.isBlank()) return 0.0;
            List<Transaction> txs = gson.fromJson(json, new TypeToken<List<Transaction>>(){}.getType());
            if (txs == null) return 0.0;
            LocalDate start = b.getStartDate();
            LocalDate end = b.getEndDate();
            return txs.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> b.getUserId().equals(t.getUserId()))
                .filter(t -> b.getCategoryId() != null && b.getCategoryId().equals(t.getCategoryId()))
                .filter(t -> {
                    LocalDate ld = t.getDate() == null ? null : t.getDate().toLocalDate();
                    if (ld == null) return false;
                    return (!ld.isBefore(start) && !ld.isAfter(end));
                })
                .mapToDouble(Transaction::getAmount)
                .sum();
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 计算指定日期范围内的花费
     */
    private double calculateAmountSpentInRange(Budget b, LocalDate startInclusive, LocalDate endInclusive) {
        try {
            String json = storageManager.readFile("transactions.json");
            if (json == null || json.isBlank()) return 0.0;
            List<Transaction> txs = gson.fromJson(json, new TypeToken<List<Transaction>>(){}.getType());
            if (txs == null) return 0.0;
            return txs.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> b.getUserId().equals(t.getUserId()))
                .filter(t -> b.getCategoryId() != null && b.getCategoryId().equals(t.getCategoryId()))
                .filter(t -> {
                    LocalDate ld = t.getDate() == null ? null : t.getDate().toLocalDate();
                    if (ld == null) return false;
                    return (!ld.isBefore(startInclusive) && !ld.isAfter(endInclusive));
                })
                .mapToDouble(Transaction::getAmount)
                .sum();
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 查找在指定日期有效的预算
     */
    public List<Budget> findActiveBudgets(String userId, String categoryId, LocalDate atDate) {
        final LocalDate effectiveAt = (atDate == null) ? LocalDate.now() : atDate;
        String uid = userId;
        return budgets.stream()
            .filter(b -> uid.equals(b.getUserId()) && (categoryId == null || categoryId.equals(b.getCategoryId())))
            .filter(b -> {
                LocalDate start = b.getStartDate();
                LocalDate end = b.getEndDate();
                if (start == null || end == null) return false;
                return (!effectiveAt.isBefore(start) && !effectiveAt.isAfter(end));
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 检查是否能消费（预算检查）
     * 当添加交易时，如果存在有效的预算且余额足够，则允许
     */
    public boolean canConsume(Transaction t) {
        if (t == null || t.getType() != Transaction.TransactionType.EXPENSE) return true;
        String cat = t.getCategoryId();
        String uid = t.getUserId();
        LocalDate at = t.getDate() == null ? LocalDate.now() : t.getDate().toLocalDate();
        if (cat == null || cat.isBlank()) return true;
        
        List<Budget> actives = findActiveBudgets(uid, cat, at);
        if (actives.isEmpty()) return true;
        
        for (Budget b : actives) {
            BudgetStats s = calculateStats(b);
            if (s.remaining >= t.getAmount()) return true;
        }
        return false;
    }
    
    /**
     * 加载预算数据
     */
    private void loadBudgets() {
        try {
            String json = storageManager.readFile(BUDGETS_FILE);
            if (json != null && !json.trim().isEmpty()) {
                budgets = gson.fromJson(json, new TypeToken<List<Budget>>(){}.getType());
                if (budgets == null) {
                    budgets = new ArrayList<>();
                }
            }
        } catch (Exception e) {
            System.err.println("加载预算数据失败: " + e.getMessage());
            budgets = new ArrayList<>();
        }
    }
    
    /**
     * 保存预算数据
     */
    private void saveBudgets() {
        try {
            String json = gson.toJson(budgets);
            storageManager.writeFile(BUDGETS_FILE, json);
        } catch (Exception e) {
            System.err.println("保存预算数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 预算统计信息类
     */
    public static class BudgetStats {
        public Budget budget;
        public long daysElapsed;
        public long totalDays;
        public double amountSpent;
        public double remaining;
        public double avgPerDayBudget;
        public double avgPerDayActual;
        public double projectedTotalByAvgSoFar;
        public double projectedRemainingByAvgSoFar;
        public boolean willBeOverspentByAvg;
        public double last7DaysSpent;
        public double last30DaysSpent;
    }
}