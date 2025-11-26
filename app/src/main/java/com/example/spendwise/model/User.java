package com.example.spendwise.model;

public class User {
    // Attributes
    private String id;
    private String name;
    private String email;
    // Password should be stored securely in real apps!
    private String password;

    private String profileImageUrl;
    private java.util.List<String> friends;

    // Constructors
    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
        this.friends = new java.util.ArrayList<>();
    }

    public User(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.friends = new java.util.ArrayList<>();
    }

    // Getters and Setters
    // Get user ID
    public String getId() {
        return id;
    }

    // Set user ID
    public void setId(String id) {
        this.id = id;
    }

    // Get user name
    public String getName() {
        return name;
    }

    // Set user name
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public java.util.List<String> getFriends() {
        return friends;
    }

    public void setFriends(java.util.List<String> friends) {
        this.friends = friends;
    }

    public void addFriend(String friendId) {
        if (this.friends == null) {
            this.friends = new java.util.ArrayList<>();
        }
        if (!this.friends.contains(friendId)) {
            this.friends.add(friendId);
        }
    }

    public void removeFriend(String friendId) {
        if (this.friends != null) {
            this.friends.remove(friendId);
        }
    }

    // Print user information to console
    public void printUserInfo() {
        System.out.println("User: " + name + ", Email: " + email);
    }
}