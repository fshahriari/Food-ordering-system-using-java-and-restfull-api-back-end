package main;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.BankInfo;
import com.snappfood.model.*;
import com.snappfood.controller.UserController;

import java.sql.SQLException;
import java.util.Map;


public class Main {
    public static void main(String[] args) throws SQLException {
        //try (Connection conn = DatabaseManager.getConnection()) {
       //     System.out.println("successful connection");
       // } catch (Exception e) {
      //      System.out.println("failed connection");
     //       e.printStackTrace();
     //   }
        BankInfo bankInfo = new BankInfo("Mallat", "1234567890123456");
        User newUser = new User(
                "Ali Rezaei",
                "09121234567",
                "ali@example.com",
                "123456",
                "buyer",
                "Tehran, Iran",
                "BASE64IMAGESTRING",
                bankInfo
        );
        UserController controller = new UserController();
        Map<String, Object> result = controller.handleSignup(newUser);

        System.out.println("Status Code: " + result.get("status"));
        if (result.containsKey("error")) {
            System.out.println("Error: " + result.get("error"));
        } else {
            System.out.println("Message: " + result.get("message"));
            System.out.println("User ID: " + result.get("user_id"));
            System.out.println("Token: " + result.get("token"));
        }
    }
}

