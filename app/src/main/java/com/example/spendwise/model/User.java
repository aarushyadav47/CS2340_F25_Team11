package com.example.spendwise.model;

public class User {
    // Attributes
    private String id;
    private String name;
    private String email;
    // Password should be stored securely in real apps!
    private String password;

    // Constructors
    public User(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
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

    // Print user information to console
    public void printUserInfo() {
        System.out.println("User: " + name + ", Email: " + email);
    }
}