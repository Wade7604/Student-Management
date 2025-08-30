package com.example.studentmanagement.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Student implements Serializable {
    private String id;
    private String studentId;
    private String name;
    private int age;
    private String phoneNumber;
    private String classNumber;
    private String email;
    private String className;
    private String address;
    private String dateOfBirth;

    private Map<String, Certificate> certificates;

    // Required empty constructor for Firebase
    public Student() {
    }

    public Student(String studentId, String name, int age, String phoneNumber) {
        this.studentId = studentId;
        this.name = name;
        this.age = age;
        this.phoneNumber = phoneNumber;
        this.certificates = new HashMap<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getClassNumber() {
        return classNumber;
    }

    public void setClassNumber(String classNumber) {
        this.classNumber = classNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Map<String, Certificate> getCertificates() {
        if (certificates == null) {
            certificates = new HashMap<>();
        }
        return certificates;
    }

    public void setCertificates(Map<String, Certificate> certificates) {
        this.certificates = certificates;
    }

    public void addCertificate(Certificate certificate) {
        if (certificates == null) {
            certificates = new HashMap<>();
        }
        certificates.put(certificate.getId(), certificate);
    }

    public void removeCertificate(String certificateId) {
        if (certificates != null) {
            certificates.remove(certificateId);
        }
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("name", name);
        result.put("age", age);
        result.put("phoneNumber", phoneNumber);
        result.put("classNumber", classNumber);
        result.put("email", email);
        result.put("className", className);
        result.put("address", address);
        result.put("dateOfBirth", dateOfBirth);
        result.put("certificates", certificates);
        return result;
    }
}