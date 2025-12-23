package com.accounting.api;

import com.accounting.model.Transaction;
import com.accounting.service.SyncService;
import com.accounting.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final TransactionService transactionService;

    @Autowired
    public SyncController(SyncService syncService, TransactionService transactionService) {
        this.syncService = syncService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> pull(
            @RequestParam(name = "last_version", defaultValue = "0") Long lastVersion,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(syncService.pull(userId, lastVersion));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> push(
            @RequestBody List<Transaction> incoming,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(syncService.push(userId, incoming));
    }

    // 兼容桌面客户端：获取当前用户的账单列表
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> listTransactions(Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        List<Transaction> list = transactionService.getTransactionsByUserId(userId);
        return ResponseEntity.ok(list);
    }

    // 兼容桌面客户端：上传账单列表并返回当前用户账单
    @PostMapping("/transactions/upload")
    public ResponseEntity<List<Transaction>> uploadTransactions(@RequestBody List<Transaction> incoming,
                                                                Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        syncService.push(userId, incoming);
        List<Transaction> list = transactionService.getTransactionsByUserId(userId);
        return ResponseEntity.ok(list);
    }
}
