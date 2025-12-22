package com.accounting.service;

import com.accounting.model.Transaction;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计服务类
 * 提供消费趋势预测和数据分析功能
 */
public class StatisticService {
    private TransactionService transactionService;
    
    public StatisticService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    /**
     * 按月统计支出
     */
    public Map<YearMonth, Double> getMonthlyExpenses(String userId, int months) {
        Map<YearMonth, Double> monthlyData = new HashMap<>();
        YearMonth currentMonth = YearMonth.now();
        
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        
        for (int i = 0; i < months; i++) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDate startDate = month.atDay(1);
            LocalDate endDate = month.atEndOfMonth();
            
            double total = transactions.stream()
                .filter(t -> {
                    if (t.getDate() == null) return false;
                    LocalDate transactionDate = t.getDate().toLocalDate();
                    return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                })
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
            
            monthlyData.put(month, total);
        }
        
        return monthlyData;
    }
    
    /**
     * 按月统计收入
     */
    public Map<YearMonth, Double> getMonthlyIncome(String userId, int months) {
        Map<YearMonth, Double> monthlyData = new HashMap<>();
        YearMonth currentMonth = YearMonth.now();
        
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        
        for (int i = 0; i < months; i++) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDate startDate = month.atDay(1);
            LocalDate endDate = month.atEndOfMonth();
            
            double total = transactions.stream()
                .filter(t -> {
                    if (t.getDate() == null) return false;
                    LocalDate transactionDate = t.getDate().toLocalDate();
                    return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                })
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
            
            monthlyData.put(month, total);
        }
        
        return monthlyData;
    }
    
    /**
     * 按分类统计支出
     */
    public Map<String, Double> getExpensesByCategory(String userId, YearMonth yearMonth) {
        Map<String, Double> categoryData = new HashMap<>();
        
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        
        Map<String, List<Transaction>> grouped = transactions.stream()
            .filter(t -> {
                if (t.getDate() == null) return false;
                LocalDate transactionDate = t.getDate().toLocalDate();
                return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
            })
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(t -> 
                t.getCategoryId() != null ? t.getCategoryId() : "未分类"
            ));
        
        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            double total = entry.getValue().stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
            categoryData.put(entry.getKey(), total);
        }
        
        return categoryData;
    }
    
    /**
     * 简单线性回归预测下月支出
     * 使用最小二乘法
     */
    public double predictNextMonthExpense(String userId, int months) {
        Map<YearMonth, Double> monthlyData = getMonthlyExpenses(userId, months);
        
        if (monthlyData.size() < 2) {
            // 数据不足，返回平均值
            return monthlyData.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
        }
        
        List<YearMonth> sortedMonths = monthlyData.keySet().stream()
            .sorted()
            .collect(Collectors.toList());
        
        List<Double> values = sortedMonths.stream()
            .map(monthlyData::get)
            .collect(Collectors.toList());
        
        // 线性回归: y = a + b*x
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double b = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double a = (sumY - b * sumX) / n;
        
        // 预测下一个月的值（x = n）
        return a + b * n;
    }
    
    /**
     * 计算平均月支出
     */
    public double getAverageMonthlyExpense(String userId, int months) {
        Map<YearMonth, Double> monthlyData = getMonthlyExpenses(userId, months);
        return monthlyData.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0);
    }
    
    /**
     * 计算支出趋势（增长/下降百分比）
     */
    public double getExpenseTrend(String userId, int months) {
        Map<YearMonth, Double> monthlyData = getMonthlyExpenses(userId, months);
        
        if (monthlyData.size() < 2) {
            return 0;
        }
        
        List<YearMonth> sortedMonths = monthlyData.keySet().stream()
            .sorted()
            .collect(Collectors.toList());
        
        double firstMonth = monthlyData.get(sortedMonths.get(0));
        double lastMonth = monthlyData.get(sortedMonths.get(sortedMonths.size() - 1));
        
        if (firstMonth == 0) {
            return lastMonth > 0 ? 100 : 0;
        }
        
        return ((lastMonth - firstMonth) / firstMonth) * 100;
    }
    
    /**
     * 获取年度统计
     */
    public Map<String, Object> getYearlyStatistics(String userId, int year) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        
        double totalIncome = transactions.stream()
            .filter(t -> {
                if (t.getDate() == null) return false;
                return t.getDate().getYear() == year;
            })
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double totalExpense = transactions.stream()
            .filter(t -> {
                if (t.getDate() == null) return false;
                return t.getDate().getYear() == year;
            })
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        stats.put("totalIncome", totalIncome);
        stats.put("totalExpense", totalExpense);
        stats.put("netAmount", totalIncome - totalExpense);
        stats.put("transactionCount", transactions.size());
        
        return stats;
    }
    
    /**
     * 获取月度统计
     */
    public Map<String, Object> getMonthlyStatistics(String userId, int year, int month) {
        Map<String, Object> stats = new HashMap<>();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        
        List<Transaction> monthTransactions = transactions.stream()
            .filter(t -> {
                if (t.getDate() == null) return false;
                LocalDate transactionDate = t.getDate().toLocalDate();
                return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
            })
            .collect(Collectors.toList());
        
        double totalIncome = monthTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double totalExpense = monthTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        stats.put("totalIncome", totalIncome);
        stats.put("totalExpense", totalExpense);
        stats.put("netAmount", totalIncome - totalExpense);
        stats.put("transactionCount", monthTransactions.size());
        
        return stats;
    }
}

