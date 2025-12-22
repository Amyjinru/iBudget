package com.accounting.web;

import com.accounting.model.Budget;
import com.accounting.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class BudgetsController {
    @Autowired
    private BudgetService budgetService;

    @GetMapping("/budgets")
    public String list(Model model) {
        model.addAttribute("budgets", budgetService.getBudgetsByUserId("demo"));
        return "budgets";
    }

    @PostMapping("/budgets")
    public String add(
            @RequestParam double amount,
            @RequestParam(required = false) String categoryId
    ) {
        LocalDate now = LocalDate.now();
        Budget b = new Budget("demo", categoryId, amount, now.getYear(), now.getMonthValue());
        budgetService.addBudget(b);
        return "redirect:/budgets";
    }
}
