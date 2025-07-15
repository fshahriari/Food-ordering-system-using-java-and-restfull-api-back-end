package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.Role;
import com.snappfood.model.User;
import com.snappfood.server.SessionRegistry;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class UserController {

    private final UserDAO userDAO = new UserDAO();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_IN_MINUTES = 1;

    /**
     * Handles fetching the profile for an authenticated user.
     * @param userId The ID of the authenticated user.
     * @return A map containing the user's profile information.
     * @throws ResourceNotFoundException if no user is found for the given ID.
     * @throws SQLException if a database error occurs.
     */
    public Map<String, Object> handleGetProfile(int userId) throws ResourceNotFoundException, SQLException {
        User user = userDAO.findUserById(userId);
        if (user == null) {
            // This is a safeguard, as the token validation should prevent this.
            throw new ResourceNotFoundException("User profile not found.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("user", user);
        return response;
    }

    public Map<String, Object> handleLogin(String phone, String password) throws Exception {
        User user = userDAO.findUserByPhone(phone);

        if (user == null) {
            throw new ResourceNotFoundException("Not found");
        }

        if (user.getLockTime() != null && user.getLockTime().after(new Timestamp(System.currentTimeMillis()))) {
            throw new TooManyRequestsException("Too many requests - Account is locked");
        }

        if (userDAO.isUserPending(phone)) {
            throw new ForbiddenException("Forbidden - User is pending approval");
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            userDAO.incrementFailedLoginAttempts(phone);
            if (user.getFailedLoginAttempts() + 1 >= MAX_FAILED_ATTEMPTS) {
                userDAO.lockUserAccount(phone, LOCK_TIME_IN_MINUTES);
                throw new TooManyRequestsException("Too many requests - Account locked");
            }
            throw new ForbiddenException("Forbidden - Invalid credentials");
        }

        userDAO.resetFailedLoginAttempts(phone);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "User logged in successfully.");
        String token = SessionRegistry.createSession(user.getId());
        response.put("token", token);
        response.put("user", user);

        return response;
    }

    public Map<String, Object> handleSignup(User user) throws Exception {
        if (user.getPhone() == null || !user.getPhone().matches("^[0-9]{10,15}$")) {
            throw new InvalidInputException("Invalid phone number");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new InvalidInputException("Invalid password");
        }

        if (userDAO.findUserByPhone(user.getPhone()) != null) {
            throw new DuplicatePhoneNumberException("Phone number already exists.");
        }

        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        boolean success;
        if (user.getRole() == Role.SELLER || user.getRole() == Role.COURIER) {
            success = userDAO.insertPendingUser(user);
        } else {
            success = userDAO.insertUser(user);
        }

        if (success) {
            Map<String, Object> response = new HashMap<>();
            String message;
            if (user.getRole() == Role.SELLER || user.getRole() == Role.COURIER) {
                message = "Registration request sent. Waiting for admin approval.";
                response.put("status", 200);
                response.put("message", message);
            } else {
                User createdUser = userDAO.findUserByPhone(user.getPhone());
                String token = SessionRegistry.createSession(createdUser.getId());
                message = "User registered successfully.";
                response.put("status", 200);
                response.put("message", message);
                response.put("user_id", String.valueOf(createdUser.getId()));
                response.put("token", token);
            }
            return response;
        } else {
            throw new InternalServerErrorException("Failed to create user due to a database error.");
        }
    }
}
