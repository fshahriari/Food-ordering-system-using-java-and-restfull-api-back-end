package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.User;
import com.snappfood.model.Role;
import com.snappfood.model.BankInfo;
import com.snappfood.model.ConfirmStatus;

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
            stmt.setString(5, user.getRole().getValue());
            stmt.setString(6, user.getAddress());
            stmt.setBytes(7, user.getProfileImage());
            stmt.setString(8, user.getBankInfo().getBankName());
            stmt.setString(9, user.getBankInfo().getAccountNumber());
            stmt.executeUpdate();
            return true;
        }
    }

    public boolean insertPendingUser(User user) throws SQLException {
        String sql = "INSERT INTO pending_users (full_name, phone, email, password, role, address, profile_image, bank_name, account_number, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole().getValue());
            stmt.setString(6, user.getAddress());
            stmt.setBytes(7, user.getProfileImage());
            stmt.setString(8, user.getBankInfo().getBankName());
            stmt.setString(9, user.getBankInfo().getAccountNumber());
            stmt.setString(10, ConfirmStatus.PENDING.name());
            stmt.executeUpdate();
            return true;
        }
    }

    public User findUserByPhone(String phone) throws SQLException {
        User user = findInTable("users", phone);
        if (user == null) {
            user = findInTable("pending_users", phone);
        }
        return user;
    }

    private User findInTable(String tableName, String phone) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE phone = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<User> getPendingUsers() throws SQLException {
        List<User> pendingUsers = new ArrayList<>();
        String sql = "SELECT * FROM pending_users WHERE status = 'PENDING'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                pendingUsers.add(extractUserFromResultSet(rs));
            }
        }
        return pendingUsers;
    }

    public void confirmUser(int pendingUserId) throws SQLException {
        String selectSql = "SELECT * FROM pending_users WHERE id = ?";
        User userToConfirm = null;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setInt(1, pendingUserId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    userToConfirm = extractUserFromResultSet(rs);
                }
            }
        }

        if (userToConfirm != null) {
            insertUser(userToConfirm);
            deletePendingUser(pendingUserId);
        }
    }

    public void rejectUser(int pendingUserId) throws SQLException {
        String updateSql = "UPDATE pending_users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, ConfirmStatus.REJECTED.name());
            stmt.setInt(2, pendingUserId);
            stmt.executeUpdate();
        }
    }

    private void deletePendingUser(int pendingUserId) throws SQLException {
        String deleteSql = "DELETE FROM pending_users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, pendingUserId);
            stmt.executeUpdate();
        }
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        String fullName = rs.getString("full_name");
        String phone = rs.getString("phone");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String address = rs.getString("address");
        int id = rs.getInt("id");
        String roleStr = rs.getString("role");
        byte[] profileImage = rs.getBytes("profile_image");
        BankInfo bankInfo = new BankInfo(rs.getString("bank_name"), rs.getString("account_number"));

        Role role = null;
        for (Role r : Role.values()) {
            if (r.getValue().equalsIgnoreCase(roleStr)) {
                role = r;
                break;
            }
        }

        User user = new User(fullName, phone, email, password, role, address, profileImage, bankInfo);
        user.setId(id);
        return user;
    }
}