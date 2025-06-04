package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.User;

import java.sql.*;

public class UserDAO {


    public boolean registerUser(User user) throws SQLException {
        if (user.getPhone() == null || user.getPassword() == null || user.getRole() == null)
            throw new IllegalArgumentException(); // ← 400 Bad Request

        String sql = "INSERT INTO users (name, phone, email, password, role, address) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getAddress());

            stmt.executeUpdate();
            return true; // ← 201 Created

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new SQLIntegrityConstraintViolationException(); // ← 409 Conflict
        }
    }

    // ورود
    public User login(String phone, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE phone = ? AND password = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phone);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            } else {
                return null; // ← 401 Unauthorized
            }
        }
    }

    // دریافت پروفایل
    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return mapResultSetToUser(rs); // ← 200 OK
            else return null; // ← 404 Not Found
        }
    }

    // ویرایش اطلاعات
    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ?, password = ?, address = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getAddress());
            stmt.setInt(5, user.getId());

            int affected = stmt.executeUpdate();
            return affected > 0; // ← 200 OK if true, else 400
        }
    }

    // حذف کاربر
    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int affected = stmt.executeUpdate();
            return affected > 0; // ← 200 OK if true, else 404 Not Found
        }
    }

    // تبدیل نتیجه به شیء com.snappfood.model.User
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setPhone(rs.getString("phone"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setAddress(rs.getString("address"));
        return user;
    }
}

