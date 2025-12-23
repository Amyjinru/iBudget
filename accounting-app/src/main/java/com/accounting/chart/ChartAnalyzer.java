package com.accounting.chart;

import com.accounting.service.local.LocalStatisticService;
import java.time.YearMonth;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.accounting.model.Transaction;

public class ChartAnalyzer {
    private final LocalStatisticService statisticService;
    public ChartAnalyzer(LocalStatisticService statisticService) {
        this.statisticService = statisticService;
    }
    
    public Map<String, Double> categoryExpense(String userId, YearMonth ym) {
        return statisticService.getExpensesByCategory(userId, ym);
    }
    
    public Map<String, Double> categoryIncome(String userId, YearMonth ym) {
        return statisticService.getIncomesByCategory(userId, ym);
    }
    
    public List<Double> monthlyExpensesSeries(String userId, int months) {
        Map<YearMonth, Double> m = statisticService.getMonthlyExpenses(userId, months);
        return m.keySet().stream().sorted().map(m::get).collect(Collectors.toList());
    }
    
    public List<Double> monthlyIncomeSeries(String userId, int months) {
        Map<YearMonth, Double> m = statisticService.getMonthlyIncome(userId, months);
        return m.keySet().stream().sorted().map(m::get).collect(Collectors.toList());
    }
    
    public List<Double> monthlyNetSeries(String userId, int months) {
        Map<YearMonth, Double> expenses = statisticService.getMonthlyExpenses(userId, months);
        Map<YearMonth, Double> incomes = statisticService.getMonthlyIncome(userId, months);
        return expenses.keySet().stream().sorted()
            .map(ym -> incomes.getOrDefault(ym, 0.0) - expenses.getOrDefault(ym, 0.0))
            .collect(Collectors.toList());
    }
    
    public List<Double> yearlyExpensesSeries(String userId, int years) {
        Map<Integer, Double> yearlyData = statisticService.getYearlyExpenses(userId, years);
        return yearlyData.keySet().stream().sorted().map(yearlyData::get).collect(Collectors.toList());
    }
    
    public List<Double> yearlyIncomeSeries(String userId, int years) {
        Map<Integer, Double> yearlyData = statisticService.getYearlyIncome(userId, years);
        return yearlyData.keySet().stream().sorted().map(yearlyData::get).collect(Collectors.toList());
    }
    
    public List<Double> yearlyNetSeries(String userId, int years) {
        Map<Integer, Double> expenses = statisticService.getYearlyExpenses(userId, years);
        Map<Integer, Double> incomes = statisticService.getYearlyIncome(userId, years);
        return expenses.keySet().stream().sorted()
            .map(y -> incomes.getOrDefault(y, 0.0) - expenses.getOrDefault(y, 0.0))
            .collect(Collectors.toList());
    }
}
