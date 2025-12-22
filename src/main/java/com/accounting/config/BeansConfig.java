package com.accounting.config;

import com.accounting.service.BudgetService;
import com.accounting.service.StatisticService;
import com.accounting.service.TransactionService;
import com.accounting.storage.StorageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {
    @Bean
    public StorageManager storageManager() {
        return new StorageManager();
    }

    @Bean
    public TransactionService transactionService(StorageManager storageManager) {
        return new TransactionService(storageManager);
    }

    @Bean
    public BudgetService budgetService(StorageManager storageManager, TransactionService transactionService) {
        return new BudgetService(storageManager, transactionService);
    }

    @Bean
    public StatisticService statisticService(TransactionService transactionService) {
        return new StatisticService(transactionService);
    }
}
