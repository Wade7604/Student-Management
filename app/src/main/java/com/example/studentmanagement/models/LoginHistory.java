package com.example.studentmanagement.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class LoginHistory implements Serializable {
    private String id;
    private String userId;
    private String loginDate;
    private String ipAddress;
    private String deviceInfo;
    private boolean successful;

    // Required empty constructor for Firebase
    public LoginHistory() {
    }

    public LoginHistory(String userId, String loginDate, String ipAddress, String deviceInfo, boolean successful) {
        this.userId = userId;
        this.loginDate = loginDate;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.successful = successful;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(String loginDate) {
        this.loginDate = loginDate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("loginDate", loginDate);
        result.put("ipAddress", ipAddress);
        result.put("deviceInfo", deviceInfo);
        result.put("successful", successful);  // Use the field name directly
        return result;
    }
}