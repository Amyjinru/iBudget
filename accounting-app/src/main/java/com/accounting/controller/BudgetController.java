package com.accounting.controller;

import com.accounting.model.Budget;
import com.accounting.service.BudgetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("budgetControllerLegacy")
@RequestMapping("/api/budgets-legacy")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<Budget> addBudget(@RequestBody Budget budget) {
        return ResponseEntity.ok(budgetService.addBudget(budget));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable String id) {
        if (budgetService.deleteBudget(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(@PathVariable String id, @RequestBody Budget budget) {
        Budget updated = budgetService.updateBudget(id, budget);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudgetById(@PathVariable String id) {
        Budget budget = budgetService.getBudgetById(id);
        if (budget != null) {
            return ResponseEntity.ok(budget);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getBudgetsByUserId(@RequestParam String userId) {
        return ResponseEntity.ok(budgetService.getBudgetsByUserId(userId));
    }

    @GetMapping("/month")
    public ResponseEntity<List<Budget>> getBudgetsByMonth(@RequestParam String userId, @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(budgetService.getBudgetsByMonth(userId, year, month));
    }

    @GetMapping("/total")
    public ResponseEntity<Budget> getTotalBudget(@RequestParam String userId, @RequestParam int year, @RequestParam int month) {
        Budget budget = budgetService.getTotalBudget(userId, year, month);
        if (budget != null) {
            return ResponseEntity.ok(budget);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/category")
    public ResponseEntity<Budget> getCategoryBudget(@RequestParam String userId, @RequestParam String categoryId, @RequestParam int year, @RequestParam int month) {
        Budget budget = budgetService.getCategoryBudget(userId, categoryId, year, month);
        if (budget != null) {
            return ResponseEntity.ok(budget);
        }
        return ResponseEntity.notFound().build();
    }
}
