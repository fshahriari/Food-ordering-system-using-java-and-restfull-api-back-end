package com.snappfood.controller;

import com.snappfood.dao.OrderDAO;
import com.snappfood.dao.RestaurantDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.ForbiddenException;
import com.snappfood.exception.InvalidInputException;
import com.snappfood.exception.UnauthorizedException;
import com.snappfood.model.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminController {

    private final UserDAO userDAO = new UserDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    public List<User> getPendingSellersAndCouriers() {
        try {
            return userDAO.getPendingUsers();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Handles the logic for fetching the list of pending users for an admin.
     * @param adminId The ID of the user making the request.
     * @return A map containing the list of pending users.
     * @throws Exception for authorization or database errors.
     */
    public Map<String, Object> handleGetPendingUsers(Integer adminId) throws Exception {
        if (adminId == null) {
            throw new UnauthorizedException("You must be logged in as an admin to view pending users.");
        }
        User admin = userDAO.findUserById(adminId);
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to access this resource.");
        }

        List<User> pendingUsers = userDAO.getPendingUsersSortedByName();

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("pending_users", pendingUsers);
        return response;
    }

    /**
     * Handles the batch update of pending user statuses.
     * @param adminId The ID of the admin making the request.
     * @param userUpdates The list of updates to perform.
     * @return A map with a success message.
     * @throws Exception for authorization, validation, or database errors.
     */
    public Map<String, Object> handleUpdatePendingUsers(Integer adminId, List<UserStatusUpdate> userUpdates) throws Exception {
        if (adminId == null) {
            throw new UnauthorizedException("You must be logged in as an admin.");
        }
        User admin = userDAO.findUserById(adminId);
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to perform this action.");
        }

        if (userUpdates == null || userUpdates.isEmpty()) {
            throw new InvalidInputException("Request body cannot be empty.");
        }
        for (UserStatusUpdate update : userUpdates) {
            if (update.getUserId() <= 0 || update.getStatus() == null ||
                    (!update.getStatus().equalsIgnoreCase("approved") && !update.getStatus().equalsIgnoreCase("rejected"))) {
                throw new InvalidInputException("Invalid data format for user update. Each entry must have a valid 'user_id' and 'status' ('approved' or 'rejected').");
            }
        }

        try {
            userDAO.updatePendingUsersBatch(userUpdates);
        } catch (SQLException e) {
            // The DAO will rollback,just need to re-throw a more specific error
            throw new InvalidInputException(e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Pending users updated successfully.");
        return response;
    }

    public void confirmUser(int userId) throws SQLException {
        userDAO.confirmUser(userId);
    }

    public void rejectUser(int userId) throws SQLException {
        userDAO.rejectUser(userId);
    }

    public Map<String, Object> handleGetPendingOrders(Integer adminId) throws Exception {
        authorizeAdmin(adminId);
        List<Order> pendingOrders = orderDAO.getPendingAdminOrders();
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("pending_orders", pendingOrders);
        return response;
    }

    /**
     * Handles the batch update of pending order statuses using a list of Order objects.
     * @param adminId The ID of the admin making the request.
     * @param orderUpdates The list of Order objects with updated statuses.
     * @return A map with a success message.
     * @throws Exception for authorization, validation, or database errors.
     */
    public Map<String, Object> handleUpdatePendingOrders(Integer adminId, List<Order> orderUpdates) throws Exception {
        authorizeAdmin(adminId);

        if (orderUpdates == null || orderUpdates.isEmpty()) {
            throw new InvalidInputException("Request body cannot be empty.");
        }
        for (Order update : orderUpdates) {
            if (update.getId() <= 0 || update.getStatus() == null ||
                    (update.getStatus() != com.snappfood.model.OrderStatus.PENDING_VENDOR_APPROVAL && update.getStatus() != com.snappfood.model.OrderStatus.REJECTED_BY_ADMIN)) {
                throw new InvalidInputException("Invalid data format for order update. Each entry must have a valid 'id' and a 'status' of 'PENDING_VENDOR_APPROVAL' or 'REJECTED_BY_ADMIN'.");
            }
        }

        try {
            orderDAO.updatePendingOrdersBatch(orderUpdates);
        } catch (SQLException e) {
            throw new InvalidInputException(e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Pending orders updated successfully.");
        return response;
    }

    private void authorizeAdmin(Integer adminId) throws Exception {
        if (adminId == null) {
            throw new UnauthorizedException("You must be logged in as an admin.");
        }
        User admin = userDAO.findUserById(adminId);
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to perform this action.");
        }
    }

    /**
     * Handles the logic for fetching the list of pending restaurants for an admin.
     * @param adminId The ID of the user making the request.
     * @return A map containing the list of pending restaurants.
     * @throws Exception for authorization or database errors.
     */
    public Map<String, Object> handleGetPendingRestaurants(Integer adminId) throws Exception {
        authorizeAdmin(adminId);
        List<Restaurant> pendingRestaurants = restaurantDAO.getPendingRestaurantsSortedByName();
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("pending_restaurants", pendingRestaurants);
        return response;
    }

    public Map<String, Object> handleUpdatePendingRestaurants(Integer adminId, List<RestaurantStatusUpdate> restaurantUpdates) throws Exception {
        authorizeAdmin(adminId);

        if (restaurantUpdates == null || restaurantUpdates.isEmpty()) {
            throw new InvalidInputException("Request body cannot be empty.");
        }
        for (RestaurantStatusUpdate update : restaurantUpdates) {
            if (update.getRestaurantId() <= 0 || update.getStatus() == null ||
                    (!update.getStatus().equalsIgnoreCase("approved") && !update.getStatus().equalsIgnoreCase("rejected"))) {
                throw new InvalidInputException("Invalid data format. Each entry must have a valid 'restaurant_id' and 'status' ('approved' or 'rejected').");
            }
        }

        try {
            restaurantDAO.updatePendingRestaurantsBatch(restaurantUpdates);
        } catch (SQLException e) {
            throw new InvalidInputException(e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Pending restaurants updated successfully.");
        return response;
    }

    /**
     * Handles fetching a list of all active users.
     * @param userId The ID of the authenticated admin.
     * @return A map containing the list of all users.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleListAllUsers(Integer userId) throws Exception {
        authorizeAdmin(userId);
        List<User> users = userDAO.getAllActiveUsers();
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("users", users);
        return response;
    }

    /**
     * Handles fetching a list of all orders with optional filters.
     * @param userId The ID of the authenticated admin.
     * @param filters A map of query parameters.
     * @return A map containing the list of all matching orders.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleGetAllOrders(Integer userId, Map<String, String> filters) throws Exception {
        authorizeAdmin(userId);
        List<Order> orders = orderDAO.getAllOrders(filters);
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("orders", orders);
        return response;
    }
}