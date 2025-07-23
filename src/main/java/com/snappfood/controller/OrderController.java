package com.snappfood.controller;

import com.snappfood.dao.OrderDAO;
import com.snappfood.dao.RestaurantDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the business logic for creating and managing orders.
 */
public class OrderController {

    private final OrderDAO orderDAO = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final UserDAO userDAO = new UserDAO();

    // Using TT (Toman Thousands) as the unit, 5TT = 5000 Toman
    private static final int COURIER_FEE = 50000;

    /**
     * Handles the creation of a new order.
     * Corresponds to POST /orders
     */
    public Map<String, Object> handleCreateOrder(Order order, int customerId) throws Exception {
        User customer = userDAO.findUserById(customerId);
        if (customer == null || customer.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("Only customers can place orders.");
        }
        order.setCustomerId(customerId);

        if (order.getRestaurantId() <= 0 || order.getDeliveryAddress() == null || order.getDeliveryAddress().trim().isEmpty() || order.getItems() == null || order.getItems().isEmpty()) {
            throw new InvalidInputException("Invalid order data. Restaurant, address, and items are required.");
        }

        Restaurant restaurant = restaurantDAO.getRestaurantById(order.getRestaurantId());
        if (restaurant == null || restaurantDAO.isRestaurantPending(order.getRestaurantId())) {
            throw new ResourceNotFoundException("Restaurant not found or is not currently active.");
        }

        int rawPrice = 0;
        for (Map.Entry<Integer, Integer> entry : order.getItems().entrySet()) {
            int foodId = entry.getKey();
            int quantity = entry.getValue();

            if (quantity <= 0) {
                throw new InvalidInputException("Item quantity must be greater than zero.");
            }

            Food foodItem = restaurantDAO.getFoodItemById(foodId);
            if (foodItem == null || foodItem.getRestaurantId() != order.getRestaurantId()) {
                throw new ConflictException("Food item with ID " + foodId + " does not belong to the selected restaurant.");
            }
            if (foodItem.getSupply() < quantity) {
                throw new ConflictException("Not enough stock for '" + foodItem.getName() + "'. Only " + foodItem.getSupply() + " remaining.");
            }
            rawPrice += foodItem.getPrice() * quantity;
        }

        order.setRawPrice(rawPrice);
        order.setTaxFee(restaurant.getTaxFee());
        order.setAdditionalFee(restaurant.getAdditionalFee());
        order.setCourierFee(COURIER_FEE);
        order.setPayPrice(rawPrice + restaurant.getTaxFee() + restaurant.getAdditionalFee() + COURIER_FEE);
        order.setStatus(OrderStatus.PENDING_ADMIN_APPROVAL);

        Order createdOrder = orderDAO.createOrder(order);
        if (createdOrder == null) {
            throw new InternalServerErrorException("Failed to create the order.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Order submitted successfully and is pending admin approval.");
        response.put("order", createdOrder);
        return response;
    }
}
