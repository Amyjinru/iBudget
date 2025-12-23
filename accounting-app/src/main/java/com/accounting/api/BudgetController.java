package com.accounting.api;

import com.accounting.model.Budget;
import com.accounting.service.BudgetService;
import com.accounting.service.TransactionService;
import com.accounting.storage.StorageManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ResponseEntity<List<Budget>> list(Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(budgetService.getBudgetsByUserId(user));
    }

    @PostMapping
    public ResponseEntity<Budget> createOrSet(@RequestBody Map<String, Object> body, Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        String categoryId = (String) body.getOrDefault("categoryId", null);
        // 将空字符串归一化为null，表示总预算
        if (categoryId != null && categoryId.isBlank()) {
            categoryId = null;
        }
        double amount = Double.parseDouble(String.valueOf(body.get("amount")));
        int year = Integer.parseInt(String.valueOf(body.get("year")));
        int month = Integer.parseInt(String.valueOf(body.get("month")));
        
        // 检查是否为周期型预算
        if (body.containsKey("startDate") && body.containsKey("periodUnit")) {
            // 周期型预算
            Budget b = new Budget();
            b.setUserId(user);
            b.setCategoryId(categoryId);
            b.setAmount(amount);
            b.setYear(year);
            b.setMonth(month);
            b.setStartDate(LocalDate.parse((String) body.get("startDate")));
            b.setPeriodUnit(Budget.PeriodUnit.valueOf((String) body.get("periodUnit")));
            b.setPeriodCount(Integer.parseInt(String.valueOf(body.getOrDefault("periodCount", 1))));
            return ResponseEntity.ok(budgetService.addBudget(b));
        } else {
            // 月度预算
            Budget b = budgetService.setMonthlyBudget(user, categoryId, amount, year, month);
            return ResponseEntity.ok(b);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> update(@PathVariable String id, @RequestBody Budget incoming, Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        // 更新预算前强制绑定到当前登录用户
        incoming.setUserId(user);
        Budget b = budgetService.updateBudget(id, incoming);
        return b != null ? ResponseEntity.ok(b) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean ok = budgetService.deleteBudget(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> usage(@RequestParam(required = false) String categoryId,
                                                     @RequestParam int year,
                                                     @RequestParam int month,
                                                     Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        boolean over = budgetService.isOverBudget(user, categoryId, year, month);
        double overAmount = budgetService.getOverBudgetAmount(user, categoryId, year, month);
        double rate = budgetService.getBudgetUsageRate(user, categoryId, year, month);
        // 返回预算使用情况：是否超额、超额金额和使用率
        return ResponseEntity.ok(Map.of("over", over, "overAmount", overAmount, "rate", rate));
    }
    
    @GetMapping("/stats/{id}")
    public ResponseEntity<BudgetService.BudgetStats> getStats(@PathVariable String id) {
        Budget budget = budgetService.getBudgetById(id);
        if (budget == null) {
            return ResponseEntity.notFound().build();
        }
        BudgetService.BudgetStats stats = budgetService.calculateStats(budget);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<Budget>> getActiveBudgets(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String atDate,
            Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        LocalDate date = atDate != null ? LocalDate.parse(atDate) : LocalDate.now();
        List<Budget> budgets = budgetService.findActiveBudgets(user, categoryId, date);
        return ResponseEntity.ok(budgets);
    }
}

