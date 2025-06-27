package com.snappfood.model;

import com.snappfood.controller.UserController;
import com.snappfood.model.Role;

import java.sql.SQLException;
import java.util.Map;

public class User
{
    private String fullName;
    private String phone;
    private String email; //optional
    private String password;
    private String address; //optional
    private int id;
    private String profileImageBase64;
    private Role role;
    private BankInfo bankInfo;
    //BankInfo, for seller, buyer, and com.snappfood.model.courier

    public User(){}

    public User (String name, String phoneNumber, String email,
         String password, Role role, String address ,String profilePic, BankInfo bankInfo)
    {
        this.fullName = name;
		this.phone = phoneNumber;
		this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
        this.profileImageBase64 = profilePic;
        this.bankInfo = bankInfo;

    }
    public int getId(){
        return id;
    }
    public void setId(int id){
        this.id = id;
    }
    public String getName(){
        return fullName;
    }
    public void setName(String name){
        this.fullName = name;
    }
    public String getPhone(){
        return phone;
    }
    public void setPhone(String phoneNumber){
        this.phone = phoneNumber;
    }
    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public String getPassword(){
        return password;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public Role getRole(){
        return role;
    }
    public void setRole(Role role){
        this.role = role;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getProfilePic() {
        return profileImageBase64;
    }
    public void setProfilePic(String profilePic) {
        this.profileImageBase64 = profilePic;
    }
    public BankInfo getBankInfo() {
        return bankInfo;
    }
    public void setBankInfo(BankInfo bankInfo) {
        this.bankInfo = bankInfo;
    }

    static public void signup(String name, String phone, String email, String password, String role, String address,
                          String profilePicAddress, String bankName, String accountNumber) throws SQLException {
        BankInfo bankInfo = new BankInfo(bankName, accountNumber);
        Role enumRole = null;
        for (Role r : Role.values()) {
            if (r.getValue().equalsIgnoreCase(role)) {
                enumRole = r;
                break;
            }
        }
        if (enumRole == null) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        User newUser = new User(
                name,
                phone,
                email,
                password,
                enumRole,
                address,
                profilePicAddress,
                bankInfo
        );
        UserController controller = new UserController();
        Map<String, Object> result = null;
        try {
            result = controller.handleSignup(newUser);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Status Code: " + result.get("status"));
        if (result.containsKey("error")) {
            System.out.println("Error: " + result.get("error"));
        } else {
            System.out.println("Message: " + result.get("message"));
            System.out.println("User ID: " + result.get("user_id"));
            System.out.println("Token: " + result.get("token"));
        }
    }

    static public void logIn()
    {

    }
    //methods to add: log in, managing profile(edit, view, delete)
}