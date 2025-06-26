package com.snappfood.model;

public class User
{
    private String fullName;
    private String phone;
    private String email; //optional
    private String password;
    private String address; //optional
    private   int id;
    private String profileImageBase64;
    private String role;
    private BankInfo bankInfo;
    //BankInfo, for seller, buyer, and com.snappfood.model.courier

    public User(){}

    public User (String name, String phoneNumber, String email,
         String password, String role, String address ,String profilePic, BankInfo bankInfo)
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
    public String getRole(){
        return role;
    }
    public void setRole(String role){
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
    //methods to add: sign up, log in, managing profile(edit, view, delete)
}
