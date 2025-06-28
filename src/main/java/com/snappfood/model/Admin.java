package com.snappfood.model;

import java.util.HashMap;

public class Admin extends User
{
    static HashMap<String, User> usres;
    //a static list of pending orders
    //a static report of sales statics
//    public Admin (String name, String phoneNumber, String email, String password, String address) {
//        super(name, phoneNumber, email, password, address);
//    }

    // methods to add: confirming or rejecting restaurant requests, confirming orders
    // confirm or removing users, (static) view overall system statics
    // (static) checking order problems

    public static boolean confirm(User user) {
        // Logic to confirm a user, e.g., setting a flag or updating a database
        if (usres.containsKey(user.getPhone())) {
            return true;
        }
        return false;
    }
}

