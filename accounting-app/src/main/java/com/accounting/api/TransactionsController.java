package com.accounting.api;

import com.accounting.model.Transaction;
import com.accounting.service.TransactionService;
import com.accounting.storage.StorageManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionsController {
    private final TransactionService transactionService;

    public TransactionsController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> list(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max,
            @RequestParam(required = false) String q,
            Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        List<Transaction> base = transactionService.getTransactionsByUserId(user);
        com.accounting.filter.FilterRule rule = com.accounting.filter.FilterRule.byKeyword(q);
        if (categoryId != null && !categoryId.isBlank()) {
            rule = rule.and(com.accounting.filter.FilterRule.byCategory(categoryId));
        }
        if (type != null && !type.isBlank()) {
            try {
                var tt = Transaction.TransactionType.valueOf(type);
                rule = rule.and(com.accounting.filter.FilterRule.byType(tt));
            } catch (IllegalArgumentException ignored) {}
        }
        if (min != null || max != null) {
            double lo = min != null ? min : Double.NEGATIVE_INFINITY;
            double hi = max != null ? max : Double.POSITIVE_INFINITY;
            rule = rule.and(com.accounting.filter.FilterRule.amountRange(lo, hi));
        }
        if (start != null && end != null) {
            try {
                var s = java.time.LocalDateTime.parse(start);
                var e = java.time.LocalDateTime.parse(end);
                rule = rule.and(com.accounting.filter.FilterRule.dateRange(s, e));
            } catch (Exception ignored) {}
        }
        List<Transaction> filtered = base.stream().filter(rule::test).toList();
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> get(@PathVariable String id, Authentication auth) {
        Transaction t = transactionService.getTransactionById(id);
        if (t == null) return ResponseEntity.notFound().build();
        String user = auth != null ? auth.getName() : null;
        if (user != null && (t.getUserId() == null || user.equals(t.getUserId()))) {
            return ResponseEntity.ok(t);
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@RequestBody Transaction incoming, Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        incoming.setUserId(user);
        if (incoming.getDate() == null) incoming.setDate(LocalDateTime.now());
        Transaction saved = transactionService.addTransaction(incoming);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(@PathVariable String id, @RequestBody Transaction incoming, Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        Transaction existing = transactionService.getTransactionById(id);
        if (existing == null) return ResponseEntity.notFound().build();
        if (user != null && (existing.getUserId() == null || user.equals(existing.getUserId()))) {
            incoming.setUserId(user);
            Transaction updated = transactionService.updateTransaction(id, incoming);
            return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(403).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        Transaction existing = transactionService.getTransactionById(id);
        if (existing == null) return ResponseEntity.notFound().build();
        String user = auth != null ? auth.getName() : null;
        if (user != null && (existing.getUserId() == null || user.equals(existing.getUserId()))) {
            boolean ok = transactionService.deleteTransaction(id);
            return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(403).build();
    }
}
