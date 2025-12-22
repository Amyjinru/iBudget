package com.accounting.web;

import com.accounting.model.Transaction;
import com.accounting.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class TransactionsController {
    @Autowired
    private TransactionService transactionService;

    @GetMapping("/transactions")
    public String list(Model model) {
        model.addAttribute("transactions", transactionService.getAllTransactions());
        return "transactions";
    }

    @PostMapping("/transactions")
    public String add(
            @RequestParam(required = false) String userId,
            @RequestParam String type,
            @RequestParam double amount,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String tags
    ) {
        Transaction t = new Transaction();
        t.setUserId(userId != null ? userId : "demo");
        t.setType("INCOME".equalsIgnoreCase(type) ? Transaction.TransactionType.INCOME : Transaction.TransactionType.EXPENSE);
        t.setAmount(amount);
        t.setCategoryId(categoryId);
        t.setDescription(description);
        t.setTags(tags);
        t.setDate(LocalDateTime.now());
        transactionService.addTransaction(t);
        return "redirect:/transactions";
    }
}
