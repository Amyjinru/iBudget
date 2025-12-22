package com.accounting.service;

import com.accounting.model.Budget;
import com.accounting.model.Transaction;
import com.accounting.storage.StorageManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 预算周期统计结果（不涉及 UI，仅供上层使用）
     */
    public static class BudgetStats {
        public final Budget budget;
        public final double usedAmount;
        public final double remainingAmount;
        public final LocalDate startDate;
        public final LocalDate endDate;
        public final long totalDays;
        public final long elapsedDays;
        public final long remainingDays;
        public final double avgPerDayPlanned;  // 计划中每日可花费
        public final double avgPerDayActual;   // 实际已发生平均每日支出（已过天数内）

        public BudgetStats(Budget budget,
                           double usedAmount,
                           double remainingAmount,
                           LocalDate startDate,
                           LocalDate endDate,
                           long totalDays,
                           long elapsedDays,
                           long remainingDays,
                           double avgPerDayPlanned,
                           double avgPerDayActual) {
            this.budget = budget;
            this.usedAmount = usedAmount;
            this.remainingAmount = remainingAmount;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalDays = totalDays;
            this.elapsedDays = elapsedDays;
            this.remainingDays = remainingDays;
            this.avgPerDayPlanned = avgPerDayPlanned;
            this.avgPerDayActual = avgPerDayActual;
        }
    }

    /**
     * 计算指定 Budget 的周期区间（开始和结束日期）
     */
    private LocalDate[] resolveBudgetRange(Budget budget) {
        LocalDate start = budget.getStartDate() != null
                ? budget.getStartDate()
                : YearMonth.of(budget.getYear(), budget.getMonth()).atDay(1);

        LocalDate end;
        switch (budget.getPeriodType()) {
            case DAY -> end = start;
            case WEEK -> end = start.plusDays(6);
            case MONTH -> end = YearMonth.of(start.getYear(), start.getMonth()).atEndOfMonth();
            case YEAR -> end = LocalDate.of(start.getYear(), 12, 31);
            default -> end = start;
        }
        return new LocalDate[]{start, end};
    }

    /**
     * 计算预算统计：已用金额、剩余金额、剩余时间等（按 Budget 自身的周期）
     */
    public BudgetStats calculateBudgetStats(Budget budget) {
        if (budget == null) {
            return null;
        }
        LocalDate[] range = resolveBudgetRange(budget);
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        List<Transaction> tx = transactionService.getTransactionsByUserId(budget.getUserId());
        double used = tx.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> budget.getCategoryId() == null || budget.getCategoryId().equals(t.getCategoryId()))
                .filter(t -> t.getDate() != null)
                .filter(t -> {
                    LocalDate d = t.getDate().toLocalDate();
                    return !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .mapToDouble(Transaction::getAmount)
                .sum();

        double remaining = Math.max(0, budget.getAmount() - used);

        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) {
            today = startDate;
        }
        if (today.isAfter(endDate)) {
            today = endDate;
        }
        long totalDays = Math.max(1, Period.between(startDate, endDate).getDays() + 1L);
        long elapsedDays = Math.max(1, Period.between(startDate, today).getDays() + 1L);
        long remainingDays = Math.max(0, totalDays - elapsedDays);

        double avgPlanned = budget.getAmount() / totalDays;
        double avgActual = used / elapsedDays;

        return new BudgetStats(
                budget,
                used,
                remaining,
                startDate,
                endDate,
                totalDays,
                elapsedDays,
                remainingDays,
                avgPlanned,
                avgActual
        );
    }

    /**
     * 针对某个用户 + 分类 + 年/月，返回对应 Budget 的统计信息
     */
    public BudgetStats calculateBudgetStats(String userId, String categoryId, int year, int month) {
        Budget budget = categoryId == null
                ? getTotalBudget(userId, year, month)
                : getCategoryBudget(userId, categoryId, year, month);
        return calculateBudgetStats(budget);
    }

    /**
     * 预算提醒：返回所有达到指定阈值的预算使用情况
     * thresholds 例：0.5, 0.75, 0.9
     */
    public Map<Budget, Double> checkBudgetAlerts(String userId, int year, int month, double... thresholds) {
        List<Budget> list = getBudgetsByMonth(userId, year, month);
        Map<Budget, Double> result = new HashMap<>();
        if (list.isEmpty()) return result;

        for (Budget b : list) {
            BudgetStats stats = calculateBudgetStats(b);
            if (stats == null || b.getAmount() <= 0) continue;
            double rate = stats.usedAmount / b.getAmount();
            for (double th : thresholds) {
                if (rate >= th) {
                    result.put(b, rate);
                    break; // 已满足最高优先级阈值即可
                }
            }
        }
        return result;
    }

    /**
     * 基于当前平均花费预测最终是否超支：true = 预计会超支
     * - 日/周/月预算：按每日平均支出 * 总天数 与预算额比较
     * - 年度预算：按每月平均支出 * 12 与预算额比较（不足一个月的不计入）
     */
    public boolean willOverSpend(Budget budget) {
        if (budget == null || budget.getAmount() <= 0) return false;

        BudgetStats stats = calculateBudgetStats(budget);
        if (stats == null || stats.elapsedDays <= 0) return false;

        double predictedTotal;
        if (budget.getPeriodType() == Budget.PeriodType.YEAR) {
            // 按“完整月”的平均月支出预测全年
            double[] monthly = new double[13]; // 1-12
            int[] monthCount = new int[13];

            List<Transaction> tx = transactionService.getTransactionsByUserId(budget.getUserId());
            for (Transaction t : tx) {
                if (t.getDate() == null) continue;
                LocalDate d = t.getDate().toLocalDate();
                if (d.isBefore(stats.startDate) || d.isAfter(stats.endDate)) continue;
                if (t.getType() != Transaction.TransactionType.EXPENSE) continue;
                if (budget.getCategoryId() != null && !budget.getCategoryId().equals(t.getCategoryId())) continue;

                int m = d.getMonthValue();
                monthly[m] += t.getAmount();
                monthCount[m] = 1;
            }
            int fullMonths = 0;
            double sumMonth = 0;
            for (int m = 1; m <= 12; m++) {
                if (monthCount[m] > 0) {
                    fullMonths++;
                    sumMonth += monthly[m];
                }
            }
            if (fullMonths == 0) {
                return false;
            }
            double avgPerMonth = sumMonth / fullMonths;
            predictedTotal = avgPerMonth * 12.0;
        } else {
            // 其它周期：基于每日实际平均支出 * 总天数
            predictedTotal = stats.avgPerDayActual * stats.totalDays;
        }
        return predictedTotal > budget.getAmount();
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
}

