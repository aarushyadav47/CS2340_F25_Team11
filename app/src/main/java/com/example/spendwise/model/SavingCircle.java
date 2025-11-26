package com.example.spendwise.model;

import java.util.UUID;

public class SavingCircle {
    private String id;
    private String groupName;
    private String creatorEmail;
    private String creatorUid; // UID of the creator (for reading from correct path)
    private String challengeTitle;
    private double goalAmount;
    private String frequency; // "Weekly" or "Monthly"
    private String notes;
    private long createdAt; // timestamp for sorting

    // Default constructor required for Firebase
    public SavingCircle() {
    }

    // Constructor with all parameters including timestamp
    public SavingCircle(String groupName, String creatorEmail, String challengeTitle,
                        double goalAmount, String frequency, String notes, long createdAt) {
        this.id = UUID.randomUUID().toString();
        this.groupName = groupName;
        this.creatorEmail = creatorEmail;
        this.challengeTitle = challengeTitle;
        this.goalAmount = goalAmount;
        this.frequency = frequency;
        this.notes = notes;
        // Use the passed timestamp instead of System.currentTimeMillis()
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getCreatorEmail() {
        return creatorEmail;
    }

    public String getChallengeTitle() {
        return challengeTitle;
    }

    public double getGoalAmount() {
        return goalAmount;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getNotes() {
        return notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setCreatorEmail(String creatorEmail) {
        this.creatorEmail = creatorEmail;
    }

    public void setChallengeTitle(String challengeTitle) {
        this.challengeTitle = challengeTitle;
    }

    public void setGoalAmount(double goalAmount) {
        this.goalAmount = goalAmount;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // Getter and setter for creator UID
    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }
}