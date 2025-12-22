package com.accounting.ui;

import com.accounting.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ApiClient {
    private final String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private String token;
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    public Map<String,Object> register(String username, String email, String password) {
        try {
            String body = mapper.writeValueAsString(Map.of("username", username, "email", email, "password", password));
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) throw new RuntimeException("register failed: " + resp.statusCode());
            return mapper.readValue(resp.body(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public String login(String username, String password) {
        try {
            String body = mapper.writeValueAsString(Map.of("username", username, "password", password));
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) throw new RuntimeException("login failed: " + resp.statusCode());
            Map<String,Object> map = mapper.readValue(resp.body(), Map.class);
            this.token = (String) map.get("token");
            return token;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<Transaction> listTransactions() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/sync/transactions"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) throw new RuntimeException("list failed: " + resp.statusCode());
            return mapper.readValue(resp.body(), mapper.getTypeFactory().constructCollectionType(List.class, Transaction.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<Transaction> uploadTransactions(List<Transaction> txs) {
        try {
            String body = mapper.writeValueAsString(txs);
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/sync/transactions/upload"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) throw new RuntimeException("upload failed: " + resp.statusCode());
            return mapper.readValue(resp.body(), mapper.getTypeFactory().constructCollectionType(List.class, Transaction.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }
}
