package com.snappfood.model;

import com.snappfood.controller.AdminController;

import java.util.HashMap;
import java.util.List;

public class Admin extends User {
    //static HashMap<String, User> users => DB
    //a static list of pending users to be confirmed - DB
    //a static list of pending orders - DB
    //a static report of sales statics - DB
    public Admin(String name, String phoneNumber, String email, String password, String address, byte[] profileImage) {
        super(name, phoneNumber, email, password, Role.ADMIN, address, profileImage, null);
    }

    // methods to add: confirming or rejecting restaurant requests, confirming orders
    // confirm or removing users, (static) view overall system statics
    // (static) checking order problems

    public static void confirm() { //still not completed
        AdminController adminController = new AdminController();
        List<User> pendingUsers = adminController.getPendingSellersAndCouriers();

        if (pendingUsers != null) {
            System.out.println("Pending Sellers and Couriers:");
            for (User user : pendingUsers) {
                System.out.println("ID: " + user.getId() + ", Name: " + user.getName() + ", Role: " + user.getRole().getValue());
            }
        }

    }
}

