package com.accounting.api;

import com.accounting.model.Transaction;
import com.accounting.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private final SyncService syncService = new SyncService();
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> list() {
        return ResponseEntity.ok(syncService.downloadAll());
    }
    @PostMapping("/transactions/upload")
    public ResponseEntity<List<Transaction>> upload(@RequestBody List<Transaction> incoming) {
        return ResponseEntity.ok(syncService.uploadAndMerge(incoming));
    }
}
