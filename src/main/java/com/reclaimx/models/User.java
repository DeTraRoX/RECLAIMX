package com.reclaimx.models;
import java.time.LocalDateTime;
public class User {
    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private String studentId;
    private String department;
    private int year;
    private String phone;
    private String role; // manages role of students and for admin as it is super user role
    private String createdAt;
    private boolean isActive;
    public User() {}
    public User(int id, String name, String email, String passwordHash, String studentId, String department, int year, String phone, String role, String createdAt, boolean isActive) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.studentId = studentId;
        this.department = department;
        this.year = year;
        this.phone = phone;
        this.role = role;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public boolean isIsActive() { return isActive; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
