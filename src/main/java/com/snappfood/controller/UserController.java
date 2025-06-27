package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.model.User;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// 404, 401 and 429 are left.

public class UserController {

    private final UserDAO userDAO = new UserDAO();

    public Map<String, Object> handleSignup(User user) throws SQLException {
        Map<String, Object> response = new HashMap<>();

        if (user.getPhone() == null || user.getPassword() == null || user.getRole() == null) {
            response.put("status", 400);
            if (user.getPhone() == null) {
                response.put("error", "Invalid `phone`");
            } else if (user.getPassword() == null) {
                response.put("error", "Invalid `password`");
            } else {
                response.put("error", "Invalid `role`");
            }
            return response;
        }

        if (!user.getRole().equals("buyer") && !user.getRole().equals("seller")) {
            response.put("status", 403);
            response.put("error", "Forbidden");
            return response;
        }

        if (user.getProfilePic() != null && !user.getProfilePic().matches("^[A-Za-z0-9+/=]+$")) {
            response.put("status", 415);
            response.put("error", "Unsupported media type");
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
