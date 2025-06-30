package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.User;
import com.snappfood.model.Role;
import com.snappfood.model.BankInfo;
import com.snappfood.model.Seller;
import com.snappfood.model.courier;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public boolean insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, phone, email, password, role, address, profile_image, bank_name, account_number) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole() != null ? user.getRole().getValue() : null);
            stmt.setString(6, user.getAddress());

            // Handle the profile image as BLOB
            if (user.getProfileImage() != null) {
                stmt.setBytes(7, user.getProfileImage());
            } else {
                stmt.setNull(7, java.sql.Types.BLOB);
            }

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
                    String fullName = rs.getString("full_name");
                    String email = rs.getString("email");
                    String password = rs.getString("password");
                    String address = rs.getString("address");
                    int id = rs.getInt("id");
                    String roleStr = rs.getString("role");
                    byte[] profileImage = rs.getBytes("profile_image");

                    Role role = null;
                    for (Role r : Role.values()) {
                        if (r.getValue().equalsIgnoreCase(roleStr)) {
                            role = r;
                            break;
                        }
                    }

                    BankInfo bankInfo = new BankInfo(rs.getString("bank_name"), rs.getString("account_number"));
                    User user = new User(fullName, phone, email, password, role, address, profileImage, bankInfo);
                    user.setId(id);
                    return user;
                }
            }
        }
        return null;
    }

    public List<User> getPendingUsers() throws SQLException {
        List<User> pendingUsers = new ArrayList<>();
        String sql = "SELECT id, full_name, role FROM users WHERE status = 'pending' AND (role = 'seller' OR role = 'courier')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("full_name"));
                String roleStr = rs.getString("role");
                Role role = null;
                for (Role r : Role.values()) {
                    if (r.getValue().equalsIgnoreCase(roleStr)) {
                        role = r;
                        break;
                    }
                }
                user.setRole(role);
                pendingUsers.add(user);
            }
        }
        return pendingUsers;
    }
}