package com.accounting.model;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;

/**
 * 分类实体类
 * 用于对账目进行分类管理
 */
public class Category {
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("type")
    private Transaction.TransactionType type; // 支出分类或收入分类
    
    @SerializedName("icon")
    private String icon; // 图标名称或路径
    
    @SerializedName("color")
    private String color; // 颜色代码，如 #FF5733
    
    @SerializedName("isDefault")
    private boolean isDefault; // 是否为默认分类
    
    public Category() {
        this.id = UUID.randomUUID().toString();
    }
    
    public Category(String userId, String name, Transaction.TransactionType type) {
        this();
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.isDefault = false;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Transaction.TransactionType getType() {
        return type;
    }
    
    public void setType(Transaction.TransactionType type) {
        this.type = type;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}

