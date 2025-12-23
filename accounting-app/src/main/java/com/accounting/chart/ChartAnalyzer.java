package com.accounting.chart;

import com.accounting.service.local.LocalStatisticService;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChartAnalyzer {
    private final LocalStatisticService statisticService;
    public ChartAnalyzer(LocalStatisticService statisticService) {
        this.statisticService = statisticService;
    }
    public Map<String, Double> categoryExpense(String userId, YearMonth ym) {
        return statisticService.getExpensesByCategory(userId, ym);
    }
    public List<Double> monthlyExpensesSeries(String userId, int months) {
        Map<YearMonth, Double> m = statisticService.getMonthlyExpenses(userId, months);
        return m.keySet().stream().sorted().map(m::get).collect(Collectors.toList());
    }
}
