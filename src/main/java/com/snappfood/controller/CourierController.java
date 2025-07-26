package com.snappfood.controller;

import com.snappfood.dao.OrderDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.ForbiddenException;
import com.snappfood.exception.UnauthorizedException;
import com.snappfood.model.Order;
import com.snappfood.model.Role;
import com.snappfood.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the business logic for courier-related operations.
 */
public class CourierController {

    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();

    /**
     * Handles fetching available delivery jobs for a courier.
     * @param userId The ID of the authenticated courier.
     * @return A map containing a list of available orders.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleGetAvailableDeliveries(Integer userId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view available deliveries.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.COURIER) {
            throw new ForbiddenException("Only couriers can view available deliveries.");
        }

        if (userDAO.isUserPending(user.getPhone())) {
            throw new ForbiddenException("Your courier account is pending approval.");
        }

        List<Order> availableDeliveries = orderDAO.getAvailableDeliveries();

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("deliveries", availableDeliveries);
        return response;
    }
}