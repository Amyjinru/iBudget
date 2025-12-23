package com.accounting.api;

import com.accounting.model.UserToken;
import com.accounting.repository.UserTokenRepository;
import com.accounting.service.UserService;
import com.accounting.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserTokenRepository userTokenRepository;
    
    private static final long ACCESS_TOKEN_TTL = 30 * 60 * 1000; // 30 mins
    private static final long REFRESH_TOKEN_TTL = 7L * 24 * 60 * 60 * 1000; // 7 days
    
    public AuthController(UserService userService, JwtUtil jwtUtil, UserTokenRepository userTokenRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userTokenRepository = userTokenRepository;
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.getOrDefault("email", "");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_input"));
        }
        try {
            var u = userService.register(username.trim(), email, password);
            return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", "username_exists"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "server_error"));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String deviceId = body.getOrDefault("deviceId", "unknown");
        
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_input"));
        }
        var opt = userService.login(username, password);
        if (opt.isEmpty()) return ResponseEntity.status(401).build();
        
        var user = opt.get();
        
        // 生成双Token：短效AccessToken与长效RefreshToken（主体为userId以确保接口按用户识别）
        String accessToken = jwtUtil.generate(user.getId(), ACCESS_TOKEN_TTL);
        String refreshToken = UUID.randomUUID().toString(); // Opaque refresh token
        
        // 保存刷新Token到数据库，绑定设备ID
        UserToken userToken = new UserToken(
            user.getId(),
            refreshToken,
            deviceId,
            LocalDateTime.now().plusDays(7)
        );
        userTokenRepository.save(userToken);
        // 多端管理（上限5台）：超过上限时，保留最近续期的5个，其余移除
        var tokens = userTokenRepository.findByUserId(user.getId());
        if (tokens.size() > 5) {
            tokens.stream()
                    .sorted((a, b) -> b.getExpiryDate().compareTo(a.getExpiryDate()))
                    .skip(5)
                    .forEach(userTokenRepository::delete);
        }
        
        return ResponseEntity.ok(Map.of(
            "accessToken", accessToken,
            "token", accessToken, // 为兼容前端，额外返回token字段（同accessToken）
            "refreshToken", refreshToken,
            "userId", user.getId(),
            "username", user.getUsername()
        ));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().build();
        }
        
        var tokenOpt = userTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }
        
        UserToken token = tokenOpt.get();
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            userTokenRepository.delete(token);
            return ResponseEntity.status(401).body(Map.of("error", "token_expired"));
        }
        
        // Generate new Access Token
        // Need username. Fetch user or store username in UserToken? 
        // UserToken has userId.
        // Assuming UserService can findById, or we add username to UserToken.
        // Let's fetch user by ID.
        var userOpt = userService.getUserById(token.getUserId());
        if (userOpt.isEmpty()) {
             return ResponseEntity.status(401).build();
        }
        
        String newAccessToken = jwtUtil.generate(userOpt.get().getId(), ACCESS_TOKEN_TTL);
        // Rotate refresh token? Or keep same? 
        // 最佳实践：刷新时旋转RefreshToken并续期
        String newRefreshToken = UUID.randomUUID().toString();
        token.setToken(newRefreshToken);
        token.setExpiryDate(LocalDateTime.now().plusDays(7));
        userTokenRepository.save(token);
        // 多端管理（上限5台）：刷新后也进行上限治理
        var all = userTokenRepository.findByUserId(token.getUserId());
        if (all.size() > 5) {
            all.stream()
               .sorted((a, b) -> b.getExpiryDate().compareTo(a.getExpiryDate()))
               .skip(5)
               .forEach(userTokenRepository::delete);
        }
        
        return ResponseEntity.ok(Map.of(
            "accessToken", newAccessToken,
            "token", newAccessToken, // 兼容前端：返回token字段
            "refreshToken", newRefreshToken
        ));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken != null) {
            // 登出时移除对应的刷新Token，实现设备级登出
            userTokenRepository.deleteByToken(refreshToken);
        }
        return ResponseEntity.ok().build();
    }
}
