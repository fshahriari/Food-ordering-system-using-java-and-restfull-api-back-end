package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.User;
import com.snappfood.model.Role;
import com.snappfood.model.BankInfo;

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
            stmt.setString(5, user.getRole() != null ? user.getRole().getValue() : null);
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

    public User findUserByPhone(String phone) throws SQLException {
        String sql = "SELECT * FROM users WHERE phone = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String fullName = rs.getString("full_name");
                    String email = rs.getString("email");
                    String password = rs.getString("password");
                    String roleStr = rs.getString("role");
                    String address = rs.getString("address");
                    String profileImageBase64 = rs.getString("profile_image_base64");
                    String bankName = rs.getString("bank_name");
                    String accountNumber = rs.getString("account_number");

                    Role role = null;
                    for (Role r : Role.values()) {
                        if (r.getValue().equalsIgnoreCase(roleStr)) {
                            role = r;
                            break;
                        }
                    }
                    BankInfo bankInfo = null;
                    if (bankName != null && accountNumber != null) {
                        bankInfo = new BankInfo(bankName, accountNumber);
                    }
                    User user = new User(fullName, phone, email, password, role, address, profileImageBase64, bankInfo);
                    user.setId(id);
                    return user;
                }
            }
        }
        return null;
    }
}
