package com.snappfood.model;

import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

public class User {
    @SerializedName("full_name")
    private String fullName;

    private String phone;
    private String email;
    private String password;
    private String address;
    private int id;

    @SerializedName("profileImageBase64")
    private String profileImageBase64;

    private String profileImagePath;

    private Role role;

    @SerializedName("bank_info")
    private BankInfo bankInfo;

    private int failedLoginAttempts;

    private Timestamp lockTime;

    public User() {
    }

    public User(String name, String phoneNumber, String email, String password, Role role, String address, String profileImageBase64, BankInfo bankInfo) {
        this.fullName = name;
        this.phone = phoneNumber;
        this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
        this.profileImageBase64 = profileImageBase64;
        this.bankInfo = bankInfo;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }

    public BankInfo getBankInfo() {
        return bankInfo;
    }

    public void setBankInfo(BankInfo bankInfo) {
        this.bankInfo = bankInfo;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public java.sql.Timestamp getLockTime() {
        return lockTime;
    }

    public void setLockTime(java.sql.Timestamp lockTime) {
        this.lockTime = lockTime;
    }
}