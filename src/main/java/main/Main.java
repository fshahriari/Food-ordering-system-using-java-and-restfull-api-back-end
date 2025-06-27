package main;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.BankInfo;
import com.snappfood.model.*;
import com.snappfood.controller.UserController;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class Main {


    public static void main(String[] args) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("successful connection");
        } catch (Exception e) {
            System.out.println("failed connection");
            e.printStackTrace();
        }
        User.signup("Ali Rezaei", "09123456789", "ali@example.com", "123456",
                "buyer", "Tehran, Iran", "BASE64IMAGESTRING", "Mallat", "1234567890123456");
    }
}

