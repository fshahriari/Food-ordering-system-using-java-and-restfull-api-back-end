package com.snappfood.controller;

import com.snappfood.dao.OrderDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.ConflictException;
import com.snappfood.exception.ForbiddenException;
import com.snappfood.exception.InvalidInputException;
import com.snappfood.exception.UnauthorizedException;
import com.snappfood.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the business logic for courier-related operations.
 */
public class CourierController {

    private final OrderController orderController = new OrderController();
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

    /**
     * Handles a courier updating the status of a delivery.
     * @param userId The ID of the authenticated courier.
     * @param orderId The ID of the order to update.
     * @param body The request body containing the new status.
     * @return A map with a success message and the updated order.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleUpdateDeliveryStatus(Integer userId, int orderId, Map<String, String> body) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to update a delivery status.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.COURIER) {
            throw new ForbiddenException("Only couriers can update delivery statuses.");
        }

        courier courier = (courier) user;
        if (courier.getCourierStatus() == CourierStatus.DELIVERING) {
            throw new ConflictException("You are already delivering an order and cannot accept another one.");
        }


        String statusStr = body.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            throw new InvalidInputException("Status is required in the request body.");
        }

        OrderStatus newStatus;
        switch (statusStr.toLowerCase()) {
            case "accepted":
                newStatus = OrderStatus.ON_THE_WAY;
                break;
            case "delivered":
                newStatus = OrderStatus.COMPLETED;
                break;
            default:
                throw new InvalidInputException("Invalid status value. Must be 'accepted' or 'delivered'.");
        }

        return orderController.handleUpdateOrderStatus(userId, orderId, newStatus);
    }

    /**
     * Handles fetching the delivery history for the authenticated courier with optional filters.
     * @param userId The ID of the authenticated courier.
     * @param filters A map of query parameters (search, vendor, user).
     * @return A map containing the list of past and current deliveries.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleGetDeliveryHistory(Integer userId, Map<String, String> filters) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view your delivery history.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.COURIER) {
            throw new ForbiddenException("Only couriers can view their delivery history.");
        }

        if (userDAO.isUserPending(user.getPhone())) {
            throw new ForbiddenException("Your courier account is pending approval.");
        }

        List<Order> deliveryHistory = orderDAO.getDeliveryHistoryForCourier(userId, filters);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("deliveries", deliveryHistory);
        return response;
    }
}