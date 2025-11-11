package com.example.spendwise.model;

public class SavingCircleInvitation {
    private String invitationId;
    private String circleId;
    private String circleName;
    private String challengeTitle;
    private String inviterEmail;
    private String inviteeEmail;
    private String creatorUid; // UID of the circle creator
    private String status; // "pending", "accepted", "declined"
    private long sentAt;
    private long respondedAt;
    private double goalAmount;
    private String frequency;

    // Default constructor for Firebase
    public SavingCircleInvitation() {
    }

    public SavingCircleInvitation(String circleId, String circleName, String challengeTitle,
                                  String inviterEmail, String inviteeEmail,
                                  double goalAmount, String frequency) {
        this.invitationId = java.util.UUID.randomUUID().toString();
        this.circleId = circleId;
        this.circleName = circleName;
        this.challengeTitle = challengeTitle;
        this.inviterEmail = inviterEmail;
        this.inviteeEmail = inviteeEmail;
        this.status = "pending";
        this.sentAt = System.currentTimeMillis();
        this.respondedAt = 0;
        this.goalAmount = goalAmount;
        this.frequency = frequency;
    }

    public SavingCircleInvitation(String circleId, String circleName, String challengeTitle,
                                  String inviterEmail, String inviteeEmail,
                                  double goalAmount, String frequency, String creatorUid) {
        this(circleId, circleName, challengeTitle, inviterEmail, inviteeEmail, goalAmount, frequency);
        this.creatorUid = creatorUid;
    }

    // Getters and Setters
    public String getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(String invitationId) {
        this.invitationId = invitationId;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setCircleId(String circleId) {
        this.circleId = circleId;
    }

    public String getCircleName() {
        return circleName;
    }

    public void setCircleName(String circleName) {
        this.circleName = circleName;
    }

    public String getChallengeTitle() {
        return challengeTitle;
    }

    public void setChallengeTitle(String challengeTitle) {
        this.challengeTitle = challengeTitle;
    }

    public String getInviterEmail() {
        return inviterEmail;
    }

    public void setInviterEmail(String inviterEmail) {
        this.inviterEmail = inviterEmail;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getSentAt() {
        return sentAt;
    }

    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }

    public long getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(long respondedAt) {
        this.respondedAt = respondedAt;
    }

    public double getGoalAmount() {
        return goalAmount;
    }

    public void setGoalAmount(double goalAmount) {
        this.goalAmount = goalAmount;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isAccepted() {
        return "accepted".equals(status);
    }

    public boolean isDeclined() {
        return "declined".equals(status);
    }
}