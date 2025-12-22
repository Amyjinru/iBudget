package com.accounting.api;

import com.accounting.service.UserService;
import com.accounting.storage.StorageManager;
import com.accounting.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.userService = new UserService(new StorageManager());
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        var u = userService.register(body.get("username"), body.get("email"), body.get("password"));
        return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername()));
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        var opt = userService.login(body.get("username"), body.get("password"));
        if (opt.isEmpty()) return ResponseEntity.status(401).build();
        String token = jwtUtil.generate(opt.get().getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
