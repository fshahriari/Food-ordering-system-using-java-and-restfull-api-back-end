package com.snappfood.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.cj.protocol.x.SyncFlushDeflaterOutputStream;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.BankInfo;
import com.snappfood.model.Role;
import com.snappfood.model.Seller;
import com.snappfood.model.User;
import com.snappfood.server.SessionRegistry;
import org.mindrot.jbcrypt.BCrypt;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
public class UserController {

    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_IN_MINUTES = 1;

    private static final int MAX_LOGOUT_REQUESTS = 5;
    private static final long LOGOUT_RATE_LIMIT_WINDOW_MS = 60000; // 1 minute
    private final Map<Integer, LogoutRequestTracker> logoutRequestTrackers = new ConcurrentHashMap<>();


    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");


    /**
     * Handles the logic for updating a user's profile.
     * @param userId The ID of the authenticated user.
     * @param body The raw JSON string from the request body.
     * @return A map with a success message and the updated user object.
     * @throws Exception for various error conditions.
     */
    public Map<String, Object> handleUpdateProfile(Integer userId, String body) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("Invalid token");
        }

        User existingUser = userDAO.findUserById(userId);
        if (existingUser == null) {
            throw new ResourceNotFoundException("User profile not found.");
        }

        if (userDAO.isUserPending(String.valueOf(userId))) {
            throw new ForbiddenException("Pending users can't edit their profile.");
        }

        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> updatedData = gson.fromJson(body, type);

        if (updatedData.containsKey("full_name")) {
            existingUser.setName((String) updatedData.get("full_name"));
            if (existingUser.getName() == null || existingUser.getName().isEmpty()) {
                throw new InvalidInputException("full name is required and cannot be empty.");
            }
        }
        if (updatedData.containsKey("email")) {
            String email = (String) updatedData.get("email");
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new InvalidInputException("Invalid email format");
            }
            existingUser.setEmail(email);
        }
        if (updatedData.containsKey("address")) {
            if (userDAO.findUserById(userId).getRole() != Role.CUSTOMER) {
                throw new InvalidInputException("User is not a customer and can't have an address");
            }
            existingUser.setAddress((String) updatedData.get("address"));
        }
        if (updatedData.containsKey("phone")) {
            String newPhone = (String) updatedData.get("phone");
            if ((!newPhone.equals(existingUser.getPhone()) && userDAO.findUserByPhone(newPhone) != null)
                || userDAO.findUserByPhone(newPhone) != null) {
                throw new ConflictException("Phone number already in use.");
            }
            existingUser.setPhone(newPhone);
            if (existingUser.getPhone() == null || existingUser.getPhone().isEmpty()) {
                throw new InvalidInputException("Phone number is required and cannot be empty.");
            }
        }
        if (updatedData.containsKey("profileImageBase64")) {
            String imageBase64 = (String) updatedData.get("profileImageBase64");
            existingUser.setProfileImageBase64(imageBase64);
            if (!GenerallController.isValidImage(imageBase64)) {
                throw new InvalidInputException("Invalid image");
            }
        }
        if (updatedData.containsKey("bank_info")) {
            if (existingUser.getRole() == Role.ADMIN || existingUser.getRole() == Role.UNDEFIENED) {
                throw new ForbiddenException("Bank info can only be set for sellers and couriers and customers");
            }
            Map<String, String> bankInfoMap = (Map<String, String>) updatedData.get("bank_info");
            BankInfo bankInfo = new BankInfo(bankInfoMap.get("bank_name"), bankInfoMap.get("account_number"));
            if (bankInfo.getBankName() == null || bankInfo.getBankName().isEmpty()
                || bankInfo.getAccountNumber() == null || bankInfo.getAccountNumber().isEmpty()) {
                throw new InvalidInputException("Bank info required!");
            }
            existingUser.setBankInfo(bankInfo);
        }

        if (existingUser.getRole() == Role.SELLER) {
            Seller existingSeller = (Seller) existingUser;
            if (updatedData.containsKey("brand_name")) {
                existingSeller.setBrandName((String) updatedData.get("brand_name"));
            }
            if (updatedData.containsKey("brand_description")) {
                existingSeller.setBrandDescription((String) updatedData.get("brand_description"));
            }
        }

        boolean success = userDAO.updateUser(existingUser);
        if (!success) {
            throw new InternalServerErrorException("Failed to update user profile.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Profile updated successfully.");
        response.put("user", existingUser);
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
    public Map<String, Object> handleGetProfile(Integer userId) throws Exception {
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
            if (SessionRegistry.isUserActive(user.getId())) {
                String existingToken = SessionRegistry.getTokenByUserId(user.getId());
                Map<String, Object> response = new HashMap<>();
                response.put("status", 409);
                response.put("message", "You are already logged in.");
                response.put("token", existingToken);
                response.put("user", user);
                return response;
            }
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

        String token = SessionRegistry.createSession(user.getId());
        Map<String, Object> userResponseMap = buildUserResponseMap(user);
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Login successful.");
        response.put("token", token);
        response.put("user", userResponseMap);
        return response;
    }

    public Map<String, Object> handleSignup(User user) throws Exception {

        //400
        if (user == null) {
            throw new InvalidInputException("User data cannot be null.");
        }
        //409
        if (userDAO.findUserByPhone(user.getPhone()) != null
            || userDAO.isUserPending(user.getPhone())) {
            throw new DuplicatePhoneNumberException("Phone number already exists.");
        }
        //400
        if (user.getPhone() == null || !user.getPhone().matches("^[0-9]{8,15}$")) {
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
        if (user.getRole().equals(Role.CUSTOMER) &&
               ((user.getAddress() == null)
                || !user.getAddress().matches("^[\\p{L}\\p{N}\\s,.-]{0,200}$")
                || user.getAddress().trim().length() < 3)) {
            throw new InvalidInputException("Invalid address");
        }
        if (user.getRole() == null || user.getRole().equals("undefined")
                || !Role.isValid(user.getRole().getValue())) {
            throw new InvalidInputException("Invalid role");
        }
        if (user.getProfileImageBase64() != null && !GenerallController.isValidImage(user.getProfileImageBase64())) {
            throw new InvalidInputException("Invalid profile image");
        }
        if(user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("admin role is not allowed for signup");
        }


        if (user.getRole() == Role.CUSTOMER || user.getRole() == Role.COURIER
            || user.getRole() == Role.SELLER) {
            if (user.getBankInfo() == null) {
                throw new InvalidInputException("Invalid bank info");
            }
            if (user.getBankInfo().getBankName() == null || user.getBankInfo().getBankName().isEmpty()) {
                throw new InvalidInputException("Invalid bank name");
            }
            if (user.getBankInfo().getAccountNumber() == null) {
                throw new InvalidInputException("Invalid account number");
            }
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
            if (user.getRole() == Role.CUSTOMER) {
                User createdUser = userDAO.findUserByPhone(user.getPhone());
                String token = SessionRegistry.createSession(createdUser.getId());
                message = "User registered successfully and is now logged in.";
                response.put("status", 200);
                response.put("message", message);
                response.put("user_id", String.valueOf(createdUser.getId()));
                response.put("token", token);
            } else {
                message = "Registration request sent. Waiting for admin approval.";
                response.put("status", 201);
                response.put("message", message);
            }
            return response;
        } else {
            throw new InternalServerErrorException("Failed to create user due to a database error.");
        }
    }

    /**
     * Handles the logic for logging out a user.
     * @param token The session token of the user.
     * @return A map with a success message.
     * @throws UnauthorizedException if the token is missing or invalid.
     * @throws TooManyRequestsException if the user exceeds the logout rate limit.
     * @throws ForbiddenException if the user is not in a state that allows logging out.
     */
    public Map<String, Object> handleLogout(String token) throws Exception {
        //404
        if (token == null || token.isEmpty()) {
            throw new ResourceNotFoundException("No token provided.");
        }

        //401
        Integer userId = SessionRegistry.getUserIdFromToken(token);
        if (userId == null) {
            throw new UnauthorizedException("not authenticated");
        }

        //400
        if (userId <= 0) {
            throw new InvalidInputException("Invalid user ID from token");
        }

        //429
        logoutRequestTrackers.computeIfAbsent(userId, k -> new LogoutRequestTracker());
        LogoutRequestTracker tracker = logoutRequestTrackers.get(userId);
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("Too many logout requests. Please try again later.");
        }


        //403
        User user = userDAO.findUserById(userId);
        if (user != null && userDAO.isUserPending(user.getPhone())) {
            throw new ForbiddenException("User account is pending and cannot log out.");
        }

        //409
        if (!SessionRegistry.isUserActive(userId)) {
            throw new ConflictException("User is not currently logged in.");
        }

        SessionRegistry.invalidateSession(token);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "User logged out successfully.");
        return response;
    }

    private static class LogoutRequestTracker {
        private int requestCount;
        private long windowStartTime;

        public LogoutRequestTracker() {
            this.requestCount = 0;
            this.windowStartTime = System.currentTimeMillis();
        }

        public synchronized boolean allowRequest() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - windowStartTime > LOGOUT_RATE_LIMIT_WINDOW_MS) {
                // Reset window
                windowStartTime = currentTime;
                requestCount = 1;
                return true;
            }

            if (requestCount < MAX_LOGOUT_REQUESTS) {
                requestCount++;
                return true;
            }

            return false;
        }
    }

    /**
     * Creates a map representation of a User object suitable for API responses,
     * excluding sensitive information like the password.
     * @param user The User object to convert.
     * @return A map containing safe-to-expose user data.
     */
    private Map<String, Object> buildUserResponseMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("full_name", user.getName());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().getValue());
        userMap.put("address", user.getAddress());
        return userMap;
    }
}
