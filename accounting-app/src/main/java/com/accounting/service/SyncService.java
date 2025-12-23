package com.accounting.service;

import com.accounting.model.SyncLog;
import com.accounting.model.Transaction;
import com.accounting.repository.SyncLogRepository;
import com.accounting.repository.TransactionRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SyncService {
    private final TransactionRepository transactionRepository;
    private final SyncLogRepository syncLogRepository;
    private final Gson gson;

    public SyncService(TransactionRepository transactionRepository, SyncLogRepository syncLogRepository) {
        this.transactionRepository = transactionRepository;
        this.syncLogRepository = syncLogRepository;

        JsonSerializer<LocalDateTime> lts = (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString());
        JsonDeserializer<LocalDateTime> ltd = (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString());
        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, lts).registerTypeAdapter(LocalDateTime.class, ltd).create();
    }

    /**
     * 拉取增量更新
     */
    public Map<String, Object> pull(String userId, Long lastVersion) {
        List<SyncLog> changes = syncLogRepository.findChanges(userId, lastVersion);
        Long maxVersion = syncLogRepository.getMaxVersion(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("changes", changes);
        result.put("current_version", maxVersion);
        return result;
    }

    /**
     * 推送并合并更改 (LWW策略)
     */
    public Map<String, Object> push(String userId, List<Transaction> incomingTransactions) {
        List<String> successIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        Map<String, String> idMapping = new HashMap<>();
        
        Long currentMaxVersion = syncLogRepository.getMaxVersion(userId);
        
        for (Transaction incoming : incomingTransactions) {
            try {
                String clientId = incoming.getId();
                if (clientId == null || clientId.isEmpty()) {
                    String newId = java.util.UUID.randomUUID().toString();
                    incoming.setId(newId);
                    // 无法映射空ID键，客户端应始终提供临时ID；此处仅记录生成的ID
                }
                processIncomingTransaction(userId, incoming, currentMaxVersion);
                successIds.add(incoming.getId());
                if (clientId != null && !clientId.isEmpty()) {
                    idMapping.put(clientId, incoming.getId());
                }
                currentMaxVersion++; // Increment for next item in batch
            } catch (Exception e) {
                failedIds.add(incoming.getId());
                e.printStackTrace();
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success_ids", successIds);
        result.put("failed_ids", failedIds);
        result.put("id_mapping", idMapping);
        result.put("new_version", currentMaxVersion);
        return result;
    }

    private void processIncomingTransaction(String userId, Transaction incoming, Long currentVersion) {
        incoming.setUserId(userId); // Ensure user ID consistency
        if (incoming.getUpdatedAt() == null) {
            incoming.setUpdatedAt(LocalDateTime.now());
        }
        
        Transaction existing = transactionRepository.findById(incoming.getId()).orElse(null);
        
        if (existing == null) {
            // New transaction
            saveAndLog(incoming, SyncLog.Action.ADD, currentVersion + 1);
        } else {
            // Conflict resolution: Last Write Wins based on updatedAt
            int affected = transactionRepository.updateIfNewer(
                    incoming.getId(),
                    incoming.getUserId(),
                    incoming.getType(),
                    incoming.getAmount(),
                    incoming.getCategoryId(),
                    incoming.getDescription(),
                    incoming.getDate(),
                    incoming.getUpdatedAt(),
                    incoming.getTags()
            );
            if (affected > 0) {
                // Log update only if DB update applied
                saveAndLog(incoming, SyncLog.Action.UPDATE, currentVersion + 1);
            }
            // If existing is newer, ignore incoming (or could log a conflict)
        }
    }

    private void saveAndLog(Transaction transaction, SyncLog.Action action, Long version) {
        Transaction saved = transactionRepository.save(transaction);
        
        SyncLog log = new SyncLog(
            saved.getId(),
            saved.getUserId(),
            action,
            "Transaction",
            gson.toJson(saved),
            version
        );
        syncLogRepository.save(log);
    }
}
