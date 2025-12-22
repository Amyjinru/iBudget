package com.accounting.service;

import com.accounting.model.Transaction;
import com.accounting.storage.StorageManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SyncService {
    private static final String CACHE_FILE = "sync-cache.json";
    private final StorageManager storageManager = new StorageManager();
    private final Gson gson = new Gson();
    private List<Transaction> cache = new ArrayList<>();
    public SyncService() {
        load();
    }
    public List<Transaction> downloadAll() {
        return new ArrayList<>(cache);
    }
    public List<Transaction> uploadAndMerge(List<Transaction> incoming) {
        Map<String, Transaction> byId = cache.stream().collect(Collectors.toMap(Transaction::getId, x -> x));
        for (Transaction t : incoming) {
            Transaction existing = byId.get(t.getId());
            if (existing == null) {
                byId.put(t.getId(), t);
            } else {
                LocalDateTime eu = existing.getUpdatedAt();
                LocalDateTime iu = t.getUpdatedAt();
                if (eu == null || (iu != null && iu.isAfter(eu))) {
                    byId.put(t.getId(), t);
                }
            }
        }
        cache = new ArrayList<>(byId.values());
        save();
        return downloadAll();
    }
    private void load() {
        try {
            String json = storageManager.readFile(CACHE_FILE);
            if (json != null && !json.trim().isEmpty()) {
                List<Transaction> list = gson.fromJson(json, new TypeToken<List<Transaction>>(){}.getType());
                if (list != null) cache = list;
            }
        } catch (Exception ignored) {}
    }
    private void save() {
        try {
            storageManager.writeFile(CACHE_FILE, gson.toJson(cache));
        } catch (Exception ignored) {}
    }
}
