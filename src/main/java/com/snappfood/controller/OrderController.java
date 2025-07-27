package com.snappfood.controller;

import com.snappfood.dao.OrderDAO;
import com.snappfood.dao.RestaurantDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.dao.WalletDAO;
import com.snappfood.exception.*;
import com.snappfood.model.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the business logic for creating and managing orders.
 */
public class OrderController {

    private final OrderDAO orderDAO = new OrderDAO();
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final UserDAO userDAO = new UserDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    // Using TT (Toman Thousands) as the unit, 5TT = 5000 Toman
    private static final int COURIER_FEE = 50000;

    /**
     * Handles the creation of a new order.
     * Corresponds to POST /orders
     */
    public Map<String, Object> handleCreateOrder(Order order, int customerId) throws Exception {
        User customer = userDAO.findUserById(customerId);
        if (customer == null) {
            throw new ForbiddenException("You must be logged in to create an order.");
        }
        order.setCustomerId(customerId);

        if (order.getRestaurantId() <= 0
                || order.getDeliveryAddress() == null
                || order.getDeliveryAddress().trim().isEmpty()
                || order.getItems() == null || order.getItems().isEmpty()) {
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
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);


        Order createdOrder = orderDAO.createOrder(order);
        if (createdOrder == null) {
            throw new InternalServerErrorException("Failed to create the order.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Order submitted successfully and is pending payment.");
        response.put("order", createdOrder);
        return response;
    }

    /**
     * Handles fetching the details of a single order, ensuring the user is authorized.
     * @param userId The ID of the authenticated user.
     * @param orderId The ID of the order to fetch.
     * @return A map containing the complete order details.
     * @throws Exception for authorization, validation, or database errors.
     */
    public Map<String, Object> handleGetOrderDetails(Integer userId, int orderId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view an order.");
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order with ID " + orderId + " not found.");
        }

        if (order.getCustomerId() != userId) {
            User user = userDAO.findUserById(userId);
            if (user == null || user.getRole() != Role.ADMIN) {
                throw new ForbiddenException("You do not have permission to view this order.");
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("order", order);
        return response;
    }

    /**
     * Handles the logic for updating an order's status, enforcing business rules for state transitions.
     * @param userId The ID of the user requesting the change.
     * @param orderId The ID of the order to update.
     * @param newStatus The desired new status for the order.
     * @return A map with a success message.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleUpdateOrderStatus(Integer userId, int orderId, OrderStatus newStatus) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to update an order.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found.");
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order with ID " + orderId + " not found.");
        }

        boolean isCustomer = order.getCustomerId() == userId;
        boolean isSeller = false;
        if (user.getRole() == Role.SELLER) {
            Restaurant restaurant = restaurantDAO.getRestaurantById(order.getRestaurantId());
            if (restaurant != null && restaurant.getSellerPhoneNumbers().contains(user.getPhone())) {
                isSeller = true;
            }
        }
        boolean isCourier = user.getRole() == Role.COURIER;
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isCustomer && !isSeller && !isCourier && !isAdmin) {
            throw new ForbiddenException("You do not have permission to update this order.");
        }

        OrderStatus currentStatus = order.getStatus();

        // Prevent changes to orders in terminal states or under admin review
        if (currentStatus == OrderStatus.REJECTED_BY_ADMIN ||
                currentStatus == OrderStatus.COMPLETED ||
                currentStatus == OrderStatus.UNPAID_AND_CANCELLED) {
            throw new ConflictException("Order status cannot be changed from its current state: " + currentStatus);
        }

        boolean isValidTransition = false;
        Integer courierIdToSet = null;
        switch (currentStatus) {
            case PENDING_ADMIN_APPROVAL:
                if (isAdmin && (newStatus == OrderStatus.PENDING_VENDOR_APPROVAL || newStatus == OrderStatus.REJECTED_BY_ADMIN)) {
                    isValidTransition = true;
                }
                break;
            case PENDING_VENDOR_APPROVAL:
                if (isSeller && (newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.REJECTED_BY_VENDOR)) {
                    isValidTransition = true;
                }
                break;
            case PREPARING:
                if (isSeller && newStatus == OrderStatus.READY_FOR_PICKUP) {
                    isValidTransition = true;
                }
                break;
            case READY_FOR_PICKUP:
                if (isCourier && newStatus == OrderStatus.ON_THE_WAY) {
                    if (order.getCourierId() != null) {
                        throw new ConflictException("Delivery already assigned.");
                    }
                    courierIdToSet = userId;
                    isValidTransition = true;
                }
                break;
            case ON_THE_WAY:
                if (isCourier && newStatus == OrderStatus.COMPLETED) {
                    if (order.getCourierId() != userId) {
                        throw new ForbiddenException("You are not the assigned courier for this order.");
                    }
                    isValidTransition = true;
                }
                break;
        }

        // Allow cancellation by customer or seller from any non-terminal state
        if (newStatus == OrderStatus.CANCELLED && (isCustomer || isSeller)) {
            isValidTransition = true;
        }

        if (!isValidTransition) {
            throw new ConflictException("Invalid status transition from " + currentStatus + " to " + newStatus + " for your role.");
        }

        orderDAO.updateOrderStatus(orderId, newStatus, courierIdToSet);

        if (newStatus == OrderStatus.COMPLETED) {
            Restaurant restaurant = restaurantDAO.getRestaurantById(order.getRestaurantId());
            User seller = userDAO.findUserByPhone(restaurant.getSellerPhoneNumbers().get(0)); // Assuming one seller
            walletDAO.processOrderPayment(order, seller.getId(), userId);
        }


        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Order status updated successfully to " + newStatus.name());
        return response;
    }

    /**
     * Handles fetching the order history for the authenticated customer.
     * @param userId The ID of the authenticated user.
     * @param filters A map of query parameters (date, status).
     * @return A map containing the list of orders.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleGetOrderHistory(Integer userId, Map<String, String> filters) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view your order history.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("Only customers can view their order history.");
        }

        if (filters.containsKey("date")) {
            String date = filters.get("date");
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new InvalidInputException("Invalid date format. Please use YYYY-MM-DD.");
            }
        }
        if (filters.containsKey("status")) {
            String status = filters.get("status");
            try {
                OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("Invalid status value: " + status);
            }
        }

        List<Order> orderHistory = orderDAO.getOrderHistoryForCustomer(userId, filters);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("orders", orderHistory);
        return response;
    }
}
