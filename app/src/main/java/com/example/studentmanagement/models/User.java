package com.example.studentmanagement.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User implements Serializable {
    private String id;
    private String fullName;
    private String email;
    private int age;
    private String phoneNumber;
    private String photoUrl;
    private boolean isLocked;
    private String role; // "admin", "teacher", etc.

    // Required empty constructor for Firebase
    public User() {
    }

    public User(String fullName, String email, int age, String phoneNumber, boolean isLocked, String role) {
        this.fullName = fullName;
        this.email = email;
        this.age = age;
        this.phoneNumber = phoneNumber;
        this.isLocked = isLocked;
        this.role = role;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("fullName", fullName);
        result.put("email", email);
        result.put("age", age);
        result.put("phoneNumber", phoneNumber);
        result.put("photoUrl", photoUrl);
        result.put("locked", isLocked);  // Đổi từ "isLocked" thành "locked"
        result.put("role", role);
        return result;
    }
}