package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.ConfirmStatus;
import com.snappfood.model.Customer;
import com.snappfood.model.User;
import com.snappfood.model.Role;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class UserController {

    private final UserDAO userDAO = new UserDAO();

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
        boolean success = userDAO.insertUser(user);
        if (success) {
            Map<String, Object> response = new HashMap<>();
            // In a real application, you would get the new user's ID from the database,
            // for example, by modifying insertUser to return the generated ID.
            int userId = userDAO.findUserByPhone(user.getPhone()).getId(); // Fetch the newly created user to get the ID
            String token = UUID.randomUUID().toString();

            response.put("status", 200);
            response.put("message", "Success");
            response.put("user_id", userId);
            response.put("token", token);
            return response;
        } else {
            throw new InternalServerErrorException("Failed to create user due to a database error.");
        }
    }
}