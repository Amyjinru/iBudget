package com.accounting.service.local;

import com.accounting.model.Budget;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Map;

public class LocalAIAnalysisService {
    private static final String API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private static final String API_KEY = "84d97518-0d06-4269-a067-c4f79a4e0b9a";
    private static final String MODEL = "doubao-seed-1-6-251015";
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    public LocalAIAnalysisService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();
        this.gson = new Gson();
    }
    
    public String analyzeSpending(Map<String, Object> budgetData) {
        try {
            String prompt = buildPrompt(budgetData);
            String response = callDoubaoAPI(prompt);
            return cleanResponse(response);
        } catch (Exception e) {
            return "AI分析服务暂时不可用: " + e.getMessage();
        }
    }
    
    /**
     * 清理AI响应中的元信息
     */
    private String cleanResponse(String response) {
        if (response == null) return response;
        
        // 移除英文括号内的元信息模式
        response = response.replaceAll("(?i)\\([^)]*?字数[^)]*?\\)", "");
        response = response.replaceAll("(?i)\\([^)]*?简洁[^)]*?\\)", "");
        response = response.replaceAll("(?i)\\([^)]*?友好[^)]*?\\)", "");
        response = response.replaceAll("(?i)\\([^)]*?覆盖[^)]*?\\)", "");
        response = response.replaceAll("(?i)\\([^)]*?要点[^)]*?\\)", "");
        response = response.replaceAll("(?i)\\([^)]*?符合[^)]*?\\)", "");
        response = response.replaceAll("(?i)\\([^)]*?要求[^)]*?\\)", "");
        
        // 移除中文括号内的元信息模式
        response = response.replaceAll("(?i)（[^）]*?字数[^）]*?）", "");
        response = response.replaceAll("(?i)（[^）]*?简洁[^）]*?）", "");
        response = response.replaceAll("(?i)（[^）]*?友好[^）]*?）", "");
        response = response.replaceAll("(?i)（[^）]*?覆盖[^）]*?）", "");
        response = response.replaceAll("(?i)（[^）]*?要点[^）]*?）", "");
        response = response.replaceAll("(?i)（[^）]*?符合[^）]*?）", "");
        
        // 移除末尾的说明性句子(不在括号内)
        response = response.replaceAll("[，,]\\s*简洁[^。！？\\n]*", "");
        response = response.replaceAll("[，,]\\s*覆盖[^。！？\\n]*", "");
        response = response.replaceAll("[，,]\\s*包含[^。！？\\n]*", "");
        
        // 移除多余的空行和空格
        response = response.replaceAll("\\n{3,}", "\\n\\n");
        response = response.replaceAll("  +", " ");
        
        return response.trim();
    }
    
    private String buildPrompt(Map<String, Object> budgetData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的个人财务顾问助手。请根据以下用户的财务数据进行分析:\n\n");
        
        if (budgetData.containsKey("monthlyBudget")) {
            prompt.append("📊 本月预算: ¥").append(budgetData.get("monthlyBudget")).append("\n");
        }
        if (budgetData.containsKey("usedAmount")) {
            prompt.append("💰 已使用: ¥").append(budgetData.get("usedAmount")).append("\n");
        }
        if (budgetData.containsKey("remainingBudget")) {
            prompt.append("💳 剩余预算: ¥").append(budgetData.get("remainingBudget")).append("\n");
        }
        if (budgetData.containsKey("totalIncome")) {
            prompt.append("📈 本月总收入: ¥").append(budgetData.get("totalIncome")).append("\n");
        }
        if (budgetData.containsKey("totalExpense")) {
            prompt.append("📉 本月总支出: ¥").append(budgetData.get("totalExpense")).append("\n");
        }
        if (budgetData.containsKey("netAmount")) {
            prompt.append("💵 本月净收入: ¥").append(budgetData.get("netAmount")).append("\n");
        }
        
        if (budgetData.containsKey("categoryExpenses")) {
            prompt.append("\n📂 分类支出明细:\n");
            @SuppressWarnings("unchecked")
            Map<String, Double> categories = (Map<String, Double>) budgetData.get("categoryExpenses");
            categories.forEach((category, amount) -> 
                prompt.append("  - ").append(category).append(": ¥").append(String.format("%.2f", amount)).append("\n")
            );
        }
        
        if (budgetData.containsKey("monthlyTrend")) {
            prompt.append("\n📊 最近6个月支出趋势: ").append(budgetData.get("monthlyTrend")).append("\n");
        }
        
        if (budgetData.containsKey("isOverBudget") && Boolean.TRUE.equals(budgetData.get("isOverBudget"))) {
            prompt.append("\n⚠️ 警告: 本月已超出预算!\n");
        }
        
        prompt.append("\n请提供以下分析:\n");
        prompt.append("1. 消费趋势分析 - 评估当前消费模式\n");
        prompt.append("2. 超支风险预警 - 如果有超支风险或已超支,请明确指出\n");
        prompt.append("3. 消费建议 - 提供3-5条具体可行的节省建议\n");
        prompt.append("4. 预算调整建议 - 根据实际情况建议如何调整预算\n");
        prompt.append("\n重要要求: 禁止在回复中包含任何元信息、说明性文字或格式提示，如'字数约150'、'简洁覆盖所有要点'、'总字数约180'、'简洁友好'、'覆盖所有要求'等。请直接输出纯净的分析内容，用简洁、友好的语气，总字数控制在300字以内。");
        
        return prompt.toString();
    }
    
    private String callDoubaoAPI(String prompt) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        requestBody.add("messages", messages);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .timeout(Duration.ofSeconds(60))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            if (responseJson.has("choices") && responseJson.get("choices").isJsonArray() && 
                responseJson.getAsJsonArray("choices").size() > 0) {
                return responseJson.getAsJsonArray("choices").get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
            }
        }
        
        throw new Exception("API调用失败: " + response.statusCode());
    }
}
