package com.accounting.api;

import com.accounting.service.StatisticService;
import com.accounting.service.TransactionService;
import com.accounting.storage.StorageManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final StatisticService statisticService;

    public StatsController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> monthly(@RequestParam(defaultValue = "12") int months,
                                                       Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        Map<YearMonth, Double> expenses = statisticService.getMonthlyExpenses(user, months);
        Map<YearMonth, Double> income = statisticService.getMonthlyIncome(user, months);
        List<YearMonth> order = expenses.keySet().stream().sorted().toList();
        return ResponseEntity.ok(Map.of(
                "months", order.stream().map(ym -> ym.getYear()+"-"+String.format("%02d", ym.getMonthValue())).toList(),
                "expenses", order.stream().map(expenses::get).toList(),
                "income", order.stream().map(income::get).toList()
        ));
    }

    @GetMapping("/category")
    public ResponseEntity<Map<String, Double>> byCategory(@RequestParam int year, @RequestParam int month,
                                                          Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        Map<String, Double> m = statisticService.getExpensesByCategory(user, YearMonth.of(year, month));
        return ResponseEntity.ok(m);
    }

    @GetMapping("/predict")
    public ResponseEntity<Map<String, Object>> predict(@RequestParam(defaultValue = "12") int months,
                                                       Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        double v = statisticService.predictNextMonthExpense(user, months);
        return ResponseEntity.ok(Map.of("months", months, "nextExpense", v));
    }

    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> trend(@RequestParam(defaultValue = "12") int months,
                                                     Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        double t = statisticService.getExpenseTrend(user, months);
        double avg = statisticService.getAverageMonthlyExpense(user, months);
        return ResponseEntity.ok(Map.of("months", months, "trendPercent", t, "avgExpense", avg));
    }

    @GetMapping("/month")
    public ResponseEntity<Map<String, Object>> month(@RequestParam int year,
                                                     @RequestParam int month,
                                                     Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        Map<String, Object> m = statisticService.getMonthlyStatistics(user, year, month);
        return ResponseEntity.ok(m);
    }
}
