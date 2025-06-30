package com.snappfood.model;

public class Customer extends User
{
    //orders history (Database or ArrayList)

    public Customer (String name, String phoneNumber, String email, String password, String address, byte[] profileImage, BankInfo bankInfo) {
        super(name, phoneNumber, email, password, Role.CUSTOMER, address, profileImage, bankInfo);
    }

    //methods to add: view order history, managing the shopping cart, place orders

}