package main;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.User;
import com.snappfood.controller.UserController;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("successful connection");
        } catch (Exception e) {
            System.out.println("failed connection");
            e.printStackTrace();
        }
        User newUser = new User(
                "Ali Rezaei",
                "09121234567",
                "ali@example.com",
                "123456",
                "buyer",
                "Tehran, Iran"
        );


        UserController controller = new UserController();
        int statusCode = controller.handleSignup(newUser);
        
        System.out.println("Response code: " + statusCode);
    }
}

