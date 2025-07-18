package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.BankInfo;
import com.snappfood.model.Role;
import com.snappfood.model.User;
import com.snappfood.server.SessionRegistry;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.regex.Pattern;

public class UserController {

    private final UserDAO userDAO = new UserDAO();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_IN_MINUTES = 1;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private boolean isValidImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return false;
        }
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handles the logic for updating a user's profile.
     * @param userId The ID of the authenticated user.
     * @param updatedData A User object containing the new data from the request.
     * @return A map with a success message and the updated user object.
     * @throws Exception for various error conditions.
     */
    public Map<String, Object> handleUpdateProfile(Integer userId, User updatedData) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("Invalid token");
        }

        User existingUser = userDAO.findUserById(userId);
        if (existingUser == null) {
            throw new ResourceNotFoundException("User profile not found.");
        }

        if (updatedData.getName() != null && !updatedData.getName().trim().isEmpty()) {
            existingUser.setName(updatedData.getName());
        }
        if (updatedData.getEmail() != null) {
            if (!EMAIL_PATTERN.matcher(updatedData.getEmail()).matches()) {
                throw new InvalidInputException("Invalid email");
            }
            existingUser.setEmail(updatedData.getEmail());
        }
        if (updatedData.getAddress() != null && !updatedData.getAddress().trim().isEmpty()) {
            if (!updatedData.getAddress().matches("^[\\p{L}\\p{N}\\s,.-]{0,200}$") || updatedData.getAddress().trim().length() < 5) {
                throw new InvalidInputException("Invalid address");
            }
            existingUser.setAddress(updatedData.getAddress());
        }
        if (updatedData.getProfileImage() != null) {
            if (updatedData.getProfileImage() != null) {
                if (!isValidImage(updatedData.getProfileImage())) {
                    throw new InvalidInputException("Invalid or corrupted image file");
                }
                existingUser.setProfileImage(updatedData.getProfileImage());
            }
            existingUser.setProfileImage(updatedData.getProfileImage());
        }
        if (updatedData.getBankInfo() != null) {
            if (existingUser.getRole() != Role.SELLER && existingUser.getRole() != Role.COURIER) {
                throw new ForbiddenException("Bank info can only be set for sellers and couriers.");
            }
            BankInfo bankInfo = updatedData.getBankInfo();
            if (bankInfo.getBankName() == null || bankInfo.getBankName().matches("^[0-9]+$")) {
                throw new InvalidInputException("Invalid bank_name");
            }
            if (bankInfo.getAccountNumber() == null || !bankInfo.getAccountNumber().matches("^[0-9]+$")) {
                throw new InvalidInputException("Invalid account_number");
            }
            existingUser.setBankInfo(bankInfo);
        }
        if (updatedData.getPhone() != null && !updatedData.getPhone().trim().isEmpty()) {
            if (userDAO.findUserByPhone(updatedData.getPhone()) != null) {
                throw new ConflictException("Phone number already in use.");
            }
            existingUser.setPhone(updatedData.getPhone());
        }

        if (userDAO.isUserPending(updatedData.getPhone())) {
            throw new ForbiddenException("User account is pending approval and cannot be updated.");
        }


        boolean success = userDAO.updateUser(existingUser);
        if (!success) {
            throw new InternalServerErrorException("Failed to update user profile.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Profile updated successfully.");
        response.put("user", existingUser); // Return the updated user object
        return response;
    }

    /**
     * Handles all logic for fetching a user profile, including authentication and error checking.
     * @param userId The ID of the authenticated user, which can be null if authentication failed.
     * @return A map containing the user's profile information.
     * @throws SQLException if a database error occurs.
     * @throws UnauthorizedException if the userId is null (token was invalid or missing).
     * @throws InvalidInputException if the userId is not a positive integer.
     * @throws ResourceNotFoundException if no user is found for the given ID.
     * @throws TooManyRequestsException if the user's account is locked.
     * @throws ForbiddenException if the user's account is pending approval.
     * @throws ConflictException if there is a data conflict (e.g., optimistic locking failure).
     */
    public Map<String, Object> handleGetProfile(Integer userId) throws SQLException, ResourceNotFoundException, TooManyRequestsException, InvalidInputException, ForbiddenException, UnauthorizedException, ConflictException {
        //401
        if (userId == null) {
            throw new UnauthorizedException("Invalid token or user not authenticated.");
        }

        //400
        if (userId <= 0) {
            throw new InvalidInputException("Invalid token"); //actually userId was invalid
        }

        //409 Conflict: Placeholder for future optimistic locking logic.
        // if (isConflict()) {
        //     throw new ConflictException("Resource state has changed. Please refresh.");
        // }

        User user = userDAO.findUserById(userId);

        //404
        if (user == null) {
            throw new ResourceNotFoundException("User profile not found.");
        }

        //429
        if (user.getLockTime() != null && user.getLockTime().after(new Timestamp(System.currentTimeMillis()))) {
            throw new TooManyRequestsException("Account is temporarily locked due to too many failed login attempts.");
        }

        //403
        if (userDAO.isUserPending(user.getPhone())) {
            throw new ForbiddenException("User account is pending approval and cannot be accessed.");
        }

        //415 n 500 are handled in request handler

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("user", user);
        return response;
    }

    public Map<String, Object> handleLogin(String phone, String password) throws Exception {
        //400
        if (phone == null || !phone.matches("^[0-9]{10,15}$")) {
            throw new InvalidInputException("Invalid phone number");
        }
        if (password == null) {
            throw new InvalidInputException("Invalid password");
        }
        if (password.trim().isEmpty()) {
            throw new InvalidInputException("Invalid password");
        }

        User user = userDAO.findUserByPhone(phone);

        //404
        if (user == null) {
            throw new ResourceNotFoundException("Not found");
        }

        //409
        if (SessionRegistry.isUserActive(user.getId())) {
            throw new ConflictException("User is already logged in on another device. Continuing will log out the other session.");
        }

        //415 in request handler

        //429
        if (user.getLockTime() != null && user.getLockTime().after(new Timestamp(System.currentTimeMillis()))) {
            throw new TooManyRequestsException("Too many requests - Account is locked");
        }

        //401 n 403
        if (!BCrypt.checkpw(password, user.getPassword())) {
            userDAO.incrementFailedLoginAttempts(phone);
            if (user.getFailedLoginAttempts() + 1 >= MAX_FAILED_ATTEMPTS) {
                userDAO.lockUserAccount(phone, LOCK_TIME_IN_MINUTES);
                throw new UnauthorizedException("wrong password!");
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

        //400
        if (user == null) {
            throw new InvalidInputException("User data cannot be null.");
        }

        if (user.getPhone() == null || !user.getPhone().matches("^[0-9]{10,15}$")) {
            throw new InvalidInputException("Invalid phone_number");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new InvalidInputException("Invalid password");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new InvalidInputException("Invalid full_name");
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty() && !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            throw new InvalidInputException("Invalid email");
        }
        if (user.getAddress() != null
                || !user.getAddress().matches("^[\\p{L}\\p{N}\\s,.-]{0,200}$")
                || user.getAddress().trim().length() < 5) {
            throw new InvalidInputException("Invalid address");
        }
        if (user.getRole() == null || user.getRole().equals("undefined")
                || !Role.isValid(user.getRole().getValue())) {
            throw new InvalidInputException("Invalid role");
        }
        if (user.getRole() == Role.COURIER || user.getRole() == Role.COURIER) {
            if (user.getBankInfo() == null) {
                throw new InvalidInputException("Invalid bank_info");
            }
            if (user.getBankInfo().getBankName() == null
                    || !user.getBankInfo().getBankName().matches("^[\\p{L}\\s]{1,50}$")
                    || user.getBankInfo().getBankName().trim().length() < 3) {
                throw new InvalidInputException("Invalid bank_name");
            }
            if (user.getBankInfo().getAccountNumber() == null
                    || !user.getBankInfo().getAccountNumber().matches("^[0-9]{10,20}$")
                    || user.getBankInfo().getAccountNumber().trim().length() < 10) {
                throw new InvalidInputException("Invalid account_number");
            }
        }

        //409
        if (userDAO.findUserByPhone(user.getPhone()) != null) {
            throw new DuplicatePhoneNumberException("Phone number already exists.");
        }

        //415 in request handler

        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        boolean success;
        if (user.getRole() == Role.SELLER || user.getRole() == Role.COURIER) {
            success = userDAO.insertPendingUser(user);
        } else {
            success = userDAO.insertUser(user);
        }

        //500
        if (!success) {
            throw new InternalServerErrorException("Failed to create user due to a database error.");
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