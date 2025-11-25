package com.example.spendwise.model;

import java.util.ArrayList;
import java.util.List;

public class ChatSession {
    private String id;
    private String title;
    private String summary;
    private long timestamp;
    private List<ChatMessage> messages;

    public ChatSession() {
        this.messages = new ArrayList<>();
    }

    public ChatSession(String id, String title, String summary, long timestamp) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.timestamp = timestamp;
        this.messages = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

    public void addMessage(ChatMessage message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
}
