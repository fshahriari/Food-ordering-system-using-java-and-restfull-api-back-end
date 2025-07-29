package com.snappfood.model;

import com.snappfood.controller.GenerallController;

public class Customer extends User
{

    public Customer (String name, String phoneNumber, String email, String password, String address, byte[] profileImage, BankInfo bankInfo) {
        super(name, phoneNumber, email, password, Role.CUSTOMER, address, GenerallController.toBase64(profileImage), bankInfo);
    }


}