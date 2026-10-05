package com.aives.model;

import java.sql.Timestamp;

public class User {

    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private Role role;
    private String studentCode;
    private boolean active;
    private Timestamp createdAt;

    public String getInitials() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        return (parts.length == 1 ? first : first + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
