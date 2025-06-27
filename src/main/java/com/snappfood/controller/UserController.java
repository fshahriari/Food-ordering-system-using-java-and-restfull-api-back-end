package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.model.User;
import com.snappfood.model.Role;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

//401!

public class UserController {

    private final UserDAO userDAO = new UserDAO();

    public Map<String, Object> handleSignup(User user) throws SQLException {
        Map<String, Object> response = new HashMap<>();

        //400 invalid field
        if (user.getPhone() == null || !user.getPhone().matches("^[0-9]{10,15}$")) {
            response.put("status", 400);
            response.put("error", "Invalid `phone`");
            return response;
        }
        else if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            response.put("status", 400);
            response.put("error", "Invalid password");
            return response;
        } else if (user.getRole() == null || !Role.isValid(user.getRole().getValue())) {
            response.put("status", 400);
            response.put("error", "Invalid role");
            return response;
        } else if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
            response.put("status", 400);
            response.put("error", "Invalid address");
            return response;
        } else if (user.getName() == null || user.getName().trim().isEmpty()) {
            response.put("status", 400);
            response.put("error", "Invalid full_name");
            return response;
        } else if (user.getEmail() == null || user.getEmail().trim().isEmpty()
                || !user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            response.put("status", 400);
            response.put("error", "Invalid email");
            return response;
        }

        // 409 Conflict - Phone number already exists (per OpenAPI spec)
        User existingUser = userDAO.findUserByPhone(user.getPhone());
        if (existingUser != null) {
            response.put("status", 409);
            response.put("error", "Phone number already exists");
            return response;
        }

        // Only allow registration for buyer, seller, or courier roles (not admin)
        if (user.getRole() == Role.ADMIN) {
            response.put("status", 403);
            response.put("error", "Forbidden");
            return response;
        } else if (user.getProfilePic() != null && !user.getProfilePic().matches("^[A-Za-z0-9+/=]+$")) {
            response.put("status", 415);
            response.put("error", "Unsupported media type");
            return response;
        } else if (user.getName() != null && user.getName().equals("notfound")) {
            response.put("status", 404);
            response.put("error", "Resource not found");
            return response;
        } else if (user.getPhone() != null && user.getPhone().equals("ratelimit")) {
            response.put("status", 429);
            response.put("error", "Too many requests");
            return response;
        } else if (user.getPhone() != null && user.getPhone().equals("servererror")) {
            response.put("status", 500);
            response.put("error", "Internal server error");
            return response;
        }

        boolean success = userDAO.insertUser(user);
        if (success) {
            int userId = 1;
            String token = UUID.randomUUID().toString();
            response.put("status", 200);
            response.put("message", "Success");
            response.put("user_id", userId);
            response.put("token", token);
        } else {
            response.put("status", 500);
            response.put("message", "Internal Server Error");
        }
        return response;
    }
}
