package com.accounting.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/ui")
public class TokensController {
    @GetMapping(value = "/tokens", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> tokens() {
        try {
            var res = new ClassPathResource("design-tokens.json");
            String json = Files.readString(res.getFile().toPath());
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
