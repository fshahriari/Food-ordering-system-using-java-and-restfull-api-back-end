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

    private static final String USERS_TABLE = "users";
    private static final String PENDING_USERS_TABLE = "pending_users";

    public void incrementFailedLoginAttempts(String phone) throws SQLException {
        boolean updated = updateFailedAttemptsInTable(USERS_TABLE, phone);
        if (!updated) {
            updateFailedAttemptsInTable(PENDING_USERS_TABLE, phone);
        }
    }

    private boolean updateFailedAttemptsInTable(String tableName, String phone) throws SQLException {
        String sql = "UPDATE " + tableName + " SET failed_login_attempts = failed_login_attempts + 1 WHERE phone = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    public void resetFailedLoginAttempts(String phone) throws SQLException {
        String sql = "UPDATE " + USERS_TABLE + " SET failed_login_attempts = 0, lock_time = NULL WHERE phone = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            stmt.executeUpdate();
        }
    }

    public void lockUserAccount(String phone, int lockDurationInMinutes) throws SQLException {
        Timestamp lockTime = new Timestamp(System.currentTimeMillis() + (long) lockDurationInMinutes * 60 * 1000);
        boolean updated = lockAccountInTable(USERS_TABLE, phone, lockTime);
        if (!updated) {
            lockAccountInTable(PENDING_USERS_TABLE, phone, lockTime);
        }
    }

    private boolean lockAccountInTable(String tableName, String phone, Timestamp lockTime) throws SQLException {
        String sql = "UPDATE " + tableName + " SET lock_time = ? WHERE phone = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, lockTime);
            stmt.setString(2, phone);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

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

            // ** FIX IS HERE: Add null check for optional BankInfo **
            BankInfo bankInfo = user.getBankInfo();
            if (bankInfo != null) {
                stmt.setString(8, bankInfo.getBankName());
                stmt.setString(9, bankInfo.getAccountNumber());
            } else {
                stmt.setNull(8, java.sql.Types.VARCHAR);
                stmt.setNull(9, java.sql.Types.VARCHAR);
            }

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

            BankInfo bankInfo = user.getBankInfo();
            if (bankInfo != null) {
                stmt.setString(8, bankInfo.getBankName());
                stmt.setString(9, bankInfo.getAccountNumber());
            } else {
                stmt.setNull(8, java.sql.Types.VARCHAR);
                stmt.setNull(9, java.sql.Types.VARCHAR);
            }

            stmt.setString(10, ConfirmStatus.PENDING.name());
            stmt.executeUpdate();
            return true;
        }
    }

    public User findUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public User findUserByPhone(String phone) throws SQLException {
        User user = findInTable("users", phone);
        if (user == null) {
            user = findInTable("pending_users", phone);
        }
        return user;
    }

    public User findUserByPhoneAndPassword(String phone, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE phone = ?";
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

    public User findPendingUserByPhoneAndPassword(String phone, String password) throws SQLException {
        String sql = "SELECT * FROM pending_users WHERE phone = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean isUserPending(String phone) throws SQLException {
        return findInTable("pending_users", phone) != null;
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
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setAddress(rs.getString("address"));
        user.setProfileImage(rs.getBytes("profile_image"));

        String bankName = rs.getString("bank_name");
        String accountNumber = rs.getString("account_number");
        if (bankName != null && accountNumber != null) {
            BankInfo bankInfo = new BankInfo(bankName, accountNumber);
            user.setBankInfo(bankInfo);
        }

        Role role = null;
        for (Role r : Role.values()) {
            if (r.getValue().equalsIgnoreCase(rs.getString("role"))) {
                role = r;
                break;
            }
        }
        user.setRole(role);

        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        user.setLockTime(rs.getTimestamp("lock_time"));

        return user;
    }
}
