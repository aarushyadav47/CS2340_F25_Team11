package com.example.spendwise.model;
public class User {
    // Attributes
    private String id;
    private String name;
    private String email;
    private String password; // store securely in real apps!

    // Constructor
    public User(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

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

    // Example method
    public void printUserInfo() {
        System.out.println("User: " + name + ", Email: " + email);
    }
}