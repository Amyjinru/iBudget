package com.accounting.service.local;

import com.accounting.filter.FilterRule;
import com.accounting.model.Transaction;
import com.accounting.storage.StorageManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 本地交易服务类
 * 提供账目的增删改查和高级过滤功能
 */
public class LocalTransactionService {
    private static final String TRANSACTIONS_FILE = "transactions.json";
    private StorageManager storageManager;
    private Gson gson;
    private List<Transaction> transactions;
    
    public LocalTransactionService(StorageManager storageManager) {
        this.storageManager = storageManager;
        JsonSerializer<LocalDateTime> lts = (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString());
        JsonDeserializer<LocalDateTime> ltd = (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString());
        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, lts).registerTypeAdapter(LocalDateTime.class, ltd).create();
        this.transactions = new ArrayList<>();
        loadTransactions();
    }
    
    /**
     * 添加交易
     */
    public Transaction addTransaction(Transaction transaction) {
        if (transaction.getId() == null || transaction.getId().isEmpty()) {
            transaction.setId(java.util.UUID.randomUUID().toString());
        }
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactions.add(transaction);
        saveTransactions();
        return transaction;
    }
    
    /**
     * 删除交易
     */
    public boolean deleteTransaction(String transactionId) {
        boolean removed = transactions.removeIf(t -> t.getId().equals(transactionId));
        if (removed) {
            saveTransactions();
        }
        return removed;
    }
    
    /**
     * 更新交易
     */
    public Transaction updateTransaction(String transactionId, Transaction updatedTransaction) {
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId().equals(transactionId)) {
                updatedTransaction.setId(transactionId);
                updatedTransaction.setCreatedAt(transactions.get(i).getCreatedAt());
                updatedTransaction.setUpdatedAt(LocalDateTime.now());
                transactions.set(i, updatedTransaction);
                saveTransactions();
                return updatedTransaction;
            }
        }
        return null;
    }
    
    /**
     * 根据ID查询交易
     */
    public Transaction getTransactionById(String transactionId) {
        return transactions.stream()
            .filter(t -> t.getId().equals(transactionId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取所有交易
     */
    public List<Transaction> getAllTransactions() {
        loadTransactions();
        return new ArrayList<>(transactions);
    }
    
    /**
     * 根据用户ID获取交易
     */
    public List<Transaction> getTransactionsByUserId(String userId) {
        loadTransactions();
        return transactions.stream()
            .filter(t -> userId == null || userId.isEmpty() || userId.equals(t.getUserId()))
            .collect(Collectors.toList());
    }
    
    /**
     * 使用过滤规则查询交易
     */
    public List<Transaction> filterTransactions(FilterRule rule) {
        if (rule == null) {
            return getAllTransactions();
        }
        loadTransactions();
        return transactions.stream()
            .filter(rule::test)
            .collect(Collectors.toList());
    }
    
    /**
     * 多条件过滤
     */
    public List<Transaction> filterTransactions(List<FilterRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return getAllTransactions();
        }
        
        FilterRule combinedRule = rules.get(0);
        for (int i = 1; i < rules.size(); i++) {
            combinedRule = combinedRule.and(rules.get(i));
        }
        
        return filterTransactions(combinedRule);
    }
    
    /**
     * 按日期范围查询
     */
    public List<Transaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return filterTransactions(FilterRule.dateRange(startDate, endDate));
    }
    
    /**
     * 按分类查询
     */
    public List<Transaction> getTransactionsByCategory(String categoryId) {
        return filterTransactions(FilterRule.byCategory(categoryId));
    }
    
    /**
     * 按类型查询
     */
    public List<Transaction> getTransactionsByType(Transaction.TransactionType type) {
        return filterTransactions(FilterRule.byType(type));
    }
    
    /**
     * 按关键字搜索
     */
    public List<Transaction> searchTransactions(String keyword) {
        return filterTransactions(FilterRule.byKeyword(keyword));
    }
    
    /**
     * 计算总金额
     */
    public double calculateTotalAmount(List<Transaction> transactions) {
        return transactions.stream()
            .mapToDouble(t -> t.getType() == Transaction.TransactionType.INCOME ? 
                t.getAmount() : -t.getAmount())
            .sum();
    }
    
    /**
     * 导出为CSV
     */
    public void exportToCSV(String filePath, List<Transaction> transactions) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // 写入表头
            writer.append("ID,用户ID,类型,金额,分类ID,描述,日期,标签\n");
            
            // 写入数据
            for (Transaction t : transactions) {
                writer.append(t.getId()).append(",")
                      .append(t.getUserId() != null ? t.getUserId() : "").append(",")
                      .append(t.getType() != null ? t.getType().name() : "").append(",")
                      .append(String.valueOf(t.getAmount())).append(",")
                      .append(t.getCategoryId() != null ? t.getCategoryId() : "").append(",")
                      .append(t.getDescription() != null ? t.getDescription().replace(",", "，") : "").append(",")
                      .append(t.getDate() != null ? t.getDate().toString() : "").append(",")
                      .append(t.getTags() != null ? t.getTags().replace(",", "，") : "")
                      .append("\n");
            }
        }
    }
    
    /**
     * 从CSV导入
     */
    public List<Transaction> importFromCSV(String filePath) throws IOException {
        List<Transaction> imported = new ArrayList<>();
        Path path = Paths.get(filePath);
        
        try (Reader reader = Files.newBufferedReader(path)) {
            List<String> lines = Files.readAllLines(path);
            
            // 跳过表头
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts.length >= 7) {
                    Transaction t = new Transaction();
                    t.setId(parts[0]);
                    t.setUserId(parts[1]);
                    t.setType(Transaction.TransactionType.valueOf(parts[2]));
                    t.setAmount(Double.parseDouble(parts[3]));
                    t.setCategoryId(parts[4]);
                    t.setDescription(parts[5]);
                    t.setDate(LocalDateTime.parse(parts[6]));
                    if (parts.length > 7) {
                        t.setTags(parts[7]);
                    }
                    imported.add(t);
                }
            }
        }
        
        // 添加到现有交易列表
        for (Transaction t : imported) {
            addTransaction(t);
        }
        
        return imported;
    }
    
    /**
     * 加载交易数据
     */
    private void loadTransactions() {
        try {
            String json = storageManager.readFile(TRANSACTIONS_FILE);
            if (json != null && !json.trim().isEmpty()) {
                transactions = gson.fromJson(json, new TypeToken<List<Transaction>>(){}.getType());
                if (transactions == null) {
                    transactions = new ArrayList<>();
                }
            }
        } catch (Exception e) {
            System.err.println("加载交易数据失败: " + e.getMessage());
            transactions = new ArrayList<>();
        }
    }
    
    /**
     * 保存交易数据
     */
    private void saveTransactions() {
        try {
            String json = gson.toJson(transactions);
            storageManager.writeFile(TRANSACTIONS_FILE, json);
        } catch (Exception e) {
            System.err.println("保存交易数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量添加交易
     */
    public void addTransactions(List<Transaction> transactions) {
        for (Transaction t : transactions) {
            addTransaction(t);
        }
    }
    
    /**
     * 清空所有交易
     */
    public void clearAllTransactions() {
        transactions.clear();
        saveTransactions();
    }
    
    /**
     * 获取交易数量
     */
    public int getTransactionCount() {
        return transactions.size();
    }
}
