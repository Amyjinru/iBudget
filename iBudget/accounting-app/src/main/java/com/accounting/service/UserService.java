package com.accounting.service;

import com.accounting.model.User;
import com.accounting.storage.StorageManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {
    private static final String USERS_FILE = "users.json";
    private final StorageManager storageManager;
    private final Gson gson = new Gson();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private List<User> users = new ArrayList<>();
    public UserService(StorageManager storageManager) {
        this.storageManager = storageManager;
        load();
    }
    public User register(String username, String email, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于 6 位");
        }
        if (findByUsername(username).isPresent()) {
            throw new IllegalStateException("用户名已存在");
        }
        // 简单的邮箱格式检查，避免明显错误
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        User u = new User(username, email, encoder.encode(password));
        // 设置创建时间，便于后续多端同步与审计
        u.setCreatedAt(LocalDateTime.now().toString());
        users.add(u);
        save();
        return u;
    }
    public Optional<User> login(String username, String password) {
        Optional<User> u = findByUsername(username);
        if (u.isPresent() && encoder.matches(password, u.get().getPasswordHash())) {
            return u;
        }
        return Optional.empty();
    }
    public Optional<User> findByUsername(String username) {
        return users.stream().filter(x -> username.equals(x.getUsername())).findFirst();
    }
    private void load() {
        try {
            String json = storageManager.readFile(USERS_FILE);
            if (json != null && !json.trim().isEmpty()) {
                List<User> list = gson.fromJson(json, new TypeToken<List<User>>(){}.getType());
                if (list != null) users = list;
            }
        } catch (Exception ignored) {}
    }
    private void save() {
        try {
            storageManager.writeFile(USERS_FILE, gson.toJson(users));
        } catch (Exception ignored) {}
    }
}
