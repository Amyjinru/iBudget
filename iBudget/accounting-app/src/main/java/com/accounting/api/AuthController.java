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
        try {
            var u = userService.register(body.get("username"), body.get("email"), body.get("password"));
            return ResponseEntity.ok(Map.of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "email", u.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            // 参数错误，例如为空或格式不合法
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            // 业务冲突，例如用户名已存在
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // 兜底异常，避免前端拿不到提示
            return ResponseEntity.internalServerError().body(Map.of("error", "服务器内部错误"));
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }
        var opt = userService.login(username, password);
        if (opt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }
        String token = jwtUtil.generate(opt.get().getUsername());
        return ResponseEntity.ok(Map.of("token", token, "username", opt.get().getUsername()));
    }
}
