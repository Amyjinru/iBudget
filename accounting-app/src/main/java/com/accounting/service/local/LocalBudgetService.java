package com.accounting.service.local;

import com.accounting.model.Budget;
import com.accounting.model.Transaction;
import com.accounting.storage.StorageManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 本地预算服务类
 */
public class LocalBudgetService {
    private static final String BUDGETS_FILE = "budgets.json";
    private final StorageManager storageManager;
    private final LocalTransactionService transactionService;
    private final Gson gson;
    private List<Budget> budgets;

    public LocalBudgetService(StorageManager storageManager, LocalTransactionService transactionService) {
        this.storageManager = storageManager;
        this.transactionService = transactionService;
        this.gson = new Gson();
        this.budgets = new ArrayList<>();
        loadBudgets();
    }

    private void loadBudgets() {
        try {
            String json = storageManager.readFile(BUDGETS_FILE);
            if (json != null && !json.trim().isEmpty()) {
                budgets = gson.fromJson(json, new TypeToken<List<Budget>>(){}.getType());
                if (budgets == null) budgets = new ArrayList<>();
            }
        } catch (Exception e) {
            budgets = new ArrayList<>();
        }
    }

    private void saveBudgets() {
        try {
            storageManager.writeFile(BUDGETS_FILE, gson.toJson(budgets));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Budget setMonthlyBudget(String userId, String categoryId, double amount, int year, int month) {
        Budget existing = budgets.stream()
                .filter(b -> (userId == null || userId.equals(b.getUserId())) &&
                        (categoryId == null ? b.getCategoryId() == null : categoryId.equals(b.getCategoryId())) &&
                        b.getYear() == year && b.getMonth() == month)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setAmount(amount);
            saveBudgets();
            return existing;
        } else {
            Budget b = new Budget(userId, categoryId, amount, year, month);
            if (b.getId() == null) b.setId(UUID.randomUUID().toString());
            budgets.add(b);
            saveBudgets();
            return b;
        }
    }

    public double calculateUsedAmount(String userId, String categoryId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Transaction> txs = transactionService.getTransactionsByDateRange(start.atStartOfDay(), end.atTime(23, 59, 59));
        
        return txs.stream()
                .filter(t -> (userId == null || userId.isEmpty() || userId.equals(t.getUserId())))
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> categoryId == null || categoryId.equals(t.getCategoryId()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public boolean isOverBudget(String userId, String categoryId, int year, int month) {
        Budget b = budgets.stream()
                .filter(bg -> (userId == null || userId.equals(bg.getUserId())) &&
                        (categoryId == null ? bg.getCategoryId() == null : categoryId.equals(bg.getCategoryId())) &&
                        bg.getYear() == year && bg.getMonth() == month)
                .findFirst()
                .orElse(null);
        
        if (b == null) return false;
        return calculateUsedAmount(userId, categoryId, year, month) > b.getAmount();
    }
}
