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

    public void confirm() {
        AdminController adminController = new AdminController();
        List<User> pendingUsers = adminController.getPendingSellersAndCouriers();

        if (pendingUsers == null || pendingUsers.isEmpty()) {
            System.out.println("No pending users to review.");
            return;
        }

        System.out.println("Pending Sellers and Couriers:");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-5s | %-20s | %-10s%n", "ID", "Name", "Role");
        System.out.println("--------------------------------------------------");

        for (User user : pendingUsers) {
            System.out.printf("%-5d | %-20s | %-10s%n", user.getId(), user.getName(), user.getRole().getValue());
        }
        System.out.println("--------------------------------------------------");


        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the ID of the user you want to confirm/reject (or '0' to exit): ");
        int userId = scanner.nextInt();

        if (userId == 0) {
            return;
        }

        System.out.print("Enter 'c' to confirm or 'r' to reject: ");
        String action = scanner.next();

        try {
            if (action.equalsIgnoreCase("c")) {
                adminController.confirmUser(userId);
                System.out.println("User confirmed successfully.");
            } else if (action.equalsIgnoreCase("r")) {
                adminController.rejectUser(userId);
                System.out.println("User rejected successfully.");
            } else {
                System.out.println("Invalid action. Please try again.");
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}