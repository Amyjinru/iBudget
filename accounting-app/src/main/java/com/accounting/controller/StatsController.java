package com.accounting.controller;

import com.accounting.service.StatisticService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Map;

@RestController("statsControllerLegacy")
@RequestMapping("/api/stats-legacy")
public class StatsController {

    private final StatisticService statisticService;

    public StatsController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    @GetMapping("/expenses/monthly")
    public ResponseEntity<Map<YearMonth, Double>> getMonthlyExpenses(@RequestParam String userId, @RequestParam int months) {
        return ResponseEntity.ok(statisticService.getMonthlyExpenses(userId, months));
    }

    @GetMapping("/income/monthly")
    public ResponseEntity<Map<YearMonth, Double>> getMonthlyIncome(@RequestParam String userId, @RequestParam int months) {
        return ResponseEntity.ok(statisticService.getMonthlyIncome(userId, months));
    }

    @GetMapping("/expenses/category")
    public ResponseEntity<Map<String, Double>> getExpensesByCategory(@RequestParam String userId, @RequestParam String yearMonth) {
        try {
            YearMonth ym = YearMonth.parse(yearMonth);
            return ResponseEntity.ok(statisticService.getExpensesByCategory(userId, ym));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
