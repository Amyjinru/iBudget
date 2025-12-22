package com.accounting.service;

import com.accounting.model.User;
import com.accounting.storage.StorageManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
        if (findByUsername(username).isPresent()) {
            throw new IllegalStateException("username exists");
        }
        User u = new User(username, email, encoder.encode(password));
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
