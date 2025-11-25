package com.example.spendwise.model;

public class ChatMessage {
    private String role;      // "user" or "AI"
    private String content;
    private long timestamp;

    public ChatMessage() {} // Needed for Firebase

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters & Setters for Firebase serialization/deserialization
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}