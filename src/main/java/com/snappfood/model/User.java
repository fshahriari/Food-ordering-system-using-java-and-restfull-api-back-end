package com.snappfood.model;

public class User
{
    private String name;
    private String phoneNumber;
    private String email; //optional
    private String password;
    private String address; //optional
    public  int id;
    private String profilePic;
    private String role;
    //BankInfo, for seller, buyer, and com.snappfood.model.courier

    public User(){}

    public User (String name, String phoneNumber, String email,
         String password, String role, String address, String profilePic)
    {
        this.name = name;
		this.phoneNumber = phoneNumber;
		this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
        this.profilePic = profilePic;

    }
    public User(int id, String name, String phoneNumber, String email, String password, String role, String address, String profilePic) {
        this(name, phoneNumber, email, password, role, address, profilePic);
        this.id = id;
    }
    public int getId(){
        return id;
    }
    public void setId(int id){
        this.id = id;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {

    }
    //methods to add: sign up, log in, managing profile(edit, view, delete)
}
