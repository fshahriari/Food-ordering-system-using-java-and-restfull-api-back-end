package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.User;

import java.sql.*;

public class UserDAO {

    public boolean insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, phone, email, password, role, address, profile_image_base64, bank_name, account_number) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getAddress());
            stmt.setString(7, user.getProfilePic());

            if (user.getBankInfo() != null) {
                stmt.setString(8, user.getBankInfo().getBankName());
                stmt.setString(9, user.getBankInfo().getAccountNumber());
            } else {
                stmt.setString(8, null);
                stmt.setString(9, null);
            }

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
