package com.snappfood.model;

import com.snappfood.controller.AdminController;
import com.snappfood.controller.GenerallController;
import com.snappfood.dao.UserDAO;
import com.snappfood.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Admin extends User {

    public Admin(String name, String phoneNumber, String email, String password, String address, byte[] profileImage) {
        super(name, phoneNumber, email, password, Role.ADMIN, address, GenerallController.toBase64(profileImage), null);
    }

}