package com.snappfood.model;

public class Buyer extends User
{
    //orders history (Database or ArrayList)
    public Buyer(String name, String phoneNumber, String email, String password, String address) {
        super(name, phoneNumber, email, password, address);
    }

    //methods to add: view order history, managing the shopping cart, place orders 
    
}
