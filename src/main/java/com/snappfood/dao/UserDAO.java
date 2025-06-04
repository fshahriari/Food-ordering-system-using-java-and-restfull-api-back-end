package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.User;

import java.sql.*;

public class UserDAO {

    public boolean insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users (name, phone, email, password, role, address, profilepic, bankinfo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getPhoneNumber());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getAddress());
            stmt.setString(7, user.getProfilePic());

            stmt.executeUpdate();
            return true;

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new SQLIntegrityConstraintViolationException();
        }
    }
}
