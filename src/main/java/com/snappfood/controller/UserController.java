package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.ConfirmStatus;
import com.snappfood.model.User;
import com.snappfood.model.Role;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class UserController {

    private final UserDAO userDAO = new UserDAO();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_IN_MINUTES = 1;

    public Map<String, Object> handleLogin(String phone, String password) throws
            InvalidInputException,
            UnauthorizedException,
            ForbiddenException,
            InternalServerErrorException,
            SQLException,
            ResourceNotFoundException,
            TooManyRequestsException
    {
        User existingUser = userDAO.findUserByPhone(phone);

        if (existingUser != null && existingUser.getLockTime() != null && existingUser.getLockTime().after(new Timestamp(System.currentTimeMillis()))) {
            throw new TooManyRequestsException("Too many requests - Account is locked");
        }

        if (phone == null || !phone.matches("^[0-9]{10,15}$")) {
            throw new InvalidInputException("Invalid phone number");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidInputException("Invalid password");
        }

        try {
            existingUser = userDAO.findUserByPhone(phone);
            if (existingUser == null) {
                throw new ResourceNotFoundException("Not found");
            }

            User pendingUser = userDAO.findPendingUserByPhoneAndPassword(phone, password);
            if (pendingUser != null) {
                throw new UnauthorizedException("Unauthorized");
            }

            User confirmedUser = userDAO.findUserByPhoneAndPassword(phone, password);
            if (confirmedUser == null) {
                userDAO.incrementFailedLoginAttempts(phone);
                User updatedUser = userDAO.findUserByPhone(phone); // Re-fetch to get the latest attempt count
                if (updatedUser != null && updatedUser.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                    userDAO.lockUserAccount(phone, LOCK_TIME_IN_MINUTES);
                    throw new TooManyRequestsException("Too many requests - Account locked");
                }
                throw new ForbiddenException("Forbidden");
            }

            // 409 will be implemented after implementing multi-threading
//            The user provides the correct phone number and password.
//
//                    Your server validates these credentials and successfully finds the active user account.
//                    The Conflict Check: Before creating a new session (logging them in again), the server checks
//                    if this user account already has an active session.
//                    It finds the active session from the laptop. To prevent multiple simultaneous logins (which can be a
//                    security risk or complicate application state), the server rejects the new login attempt.

            //415: checks if the request is in jason or not! should be implemented in sign up too!




            // If no exceptions are thrown, the user is valid and can be logged in
            Map<String, Object> response = new HashMap<>();
            response.put("status", 200);
            response.put("message", "User logged in successfully.");
            response.put("token", UUID.randomUUID().toString());
            response.put("user", confirmedUser);
            return response;

        } catch (SQLException e) {
            throw new InternalServerErrorException("Internal server error");
        }
    }

    public Map<String, Object> handleSignup(User user) throws
            InvalidInputException,
            DuplicatePhoneNumberException,
            ForbiddenException,
            ResourceNotFoundException,
            TooManyRequestsException,
            InternalServerErrorException,
            SQLException
    {

        // 400 Bad Request: Validate all required user inputs.
        if (user.getPhone() == null || !user.getPhone().matches("^[0-9]{10,15}$")) {
            throw new InvalidInputException("Invalid phone number");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new InvalidInputException("Invalid password");
        }
        if (user.getRole() == null || !Role.isValid(user.getRole().getValue())) {
            throw new InvalidInputException("Invalid role");
        }
        if ((user.getAddress() == null || user.getAddress().trim().isEmpty())
                && (user.getRole() == Role.SELLER || user.getRole() == Role.CUSTOMER)) {
            throw new InvalidInputException("Invalid address");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new InvalidInputException("Invalid full_name");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()
                || !user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new InvalidInputException("Invalid email");
        }

        // 401 Unauthorized: Check if a rejected user is trying to register again.
        switch (user.getRole()) {
            case SELLER:
                if (user instanceof com.snappfood.model.Seller) {
                    com.snappfood.model.Seller seller = (com.snappfood.model.Seller) user;
                    if (ConfirmStatus.REJECTED.equals(seller.getStatus())) {
                        throw new UnauthorizedException("Unauthorized request");
                    }
                }
                break;
            case COURIER:
                if (user instanceof com.snappfood.model.courier) {
                    com.snappfood.model.courier courier = (com.snappfood.model.courier) user;
                    if (courier.getStatus() == ConfirmStatus.REJECTED) {
                        throw new UnauthorizedException("Unauthorized request");
                    }
                }
                break;
            default:
                break;
        }

        // 403 Forbidden: Prevent admin registration through this public endpoint.
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Forbidden request");
        }

        // Note: Image validation (404 Not Found, 415 Unsupported Media Type) is now handled
        // in the User.signup() method before this controller is called.

        // 409 Conflict: Check for duplicate phone number.
        User existingUser = userDAO.findUserByPhone(user.getPhone());
        if (existingUser != null) {
            throw new DuplicatePhoneNumberException("Phone number already exists.");
        }

        // 429 Too Many Requests: Example check for rate limiting.
        if ("ratelimit".equals(user.getPhone())) {
            throw new TooManyRequestsException("Too many requests.");
        }

        // 500 Internal Server Error: Example check for triggering a server error.
        if ("servererror".equals(user.getPhone())) {
            throw new InternalServerErrorException("Internal server error.");
        }

        // Insert user into the database.
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
            } else {
                message = "User registered successfully.";
            }
            response.put("status", 200);
            response.put("message", message);
            return response;
        } else {
            throw new SQLException("Failed to create user due to a database error.");
        }
    }
}