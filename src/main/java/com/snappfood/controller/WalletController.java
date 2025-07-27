package com.snappfood.controller;

import com.snappfood.dao.OrderDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.dao.WalletDAO;
import com.snappfood.exception.ConflictException;
import com.snappfood.exception.ForbiddenException;
import com.snappfood.exception.InvalidInputException;
import com.snappfood.exception.ResourceNotFoundException;
import com.snappfood.exception.UnauthorizedException;
import com.snappfood.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the business logic for wallet and payment operations.
 */
public class WalletController {

    private final WalletDAO walletDAO = new WalletDAO();
    private final UserDAO userDAO = new UserDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    /**
     * Handles a user's request to top up their wallet.
     * @param userId The ID of the authenticated user.
     * @param body The request body containing the top-up amount.
     * @return A map with a success message and the new balance.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleTopUp(Integer userId, Map<String, Double> body) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to top up your wallet.");
        }

        Double amountDouble = body.get("amount");
        if (amountDouble == null || amountDouble <= 0) {
            throw new InvalidInputException("Amount must be a positive number.");
        }
        int amount = amountDouble.intValue();

        walletDAO.topUpWallet(userId, amount);
        Wallet wallet = walletDAO.getWalletByUserId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Wallet topped up successfully.");
        response.put("new_balance", wallet.getBalance());
        return response;
    }

    /**
     * Handles the payment for an order.
     * @param userId The ID of the authenticated user.
     * @param body The request body containing the order_id and payment method.
     * @return A map with a success message.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handlePayment(Integer userId, Map<String, Object> body) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to make a payment.");
        }

        Double orderIdDouble = (Double) body.get("order_id");
        if (orderIdDouble == null) {
            throw new InvalidInputException("order_id is required.");
        }
        int orderId = orderIdDouble.intValue();

        String method = (String) body.get("method");
        if (method == null || (!method.equals("wallet") && !method.equals("online"))) {
            throw new InvalidInputException("Invalid payment method. Must be 'wallet' or 'online'.");
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order with ID " + orderId + " not found.");
        }

        if (order.getCustomerId() != userId) {
            throw new ForbiddenException("You can only pay for your own orders.");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("This order cannot be paid for in its current state.");
        }

        if (method.equals("wallet")) {
            Wallet wallet = walletDAO.getWalletByUserId(userId);
            if (wallet.getBalance() < order.getPayPrice()) {
                throw new ConflictException("Insufficient wallet balance.");
            }
            walletDAO.topUpWallet(userId, -order.getPayPrice()); // Deduct from balance
        }

        orderDAO.updateOrderStatus(orderId, OrderStatus.PENDING_ADMIN_APPROVAL, null);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Payment successful.");
        return response;
    }

    /**
     * Handles fetching the transaction history for the authenticated user.
     * @param userId The ID of the authenticated user.
     * @return A map containing the list of transactions.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleGetTransactionHistory(Integer userId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view your transaction history.");
        }

        List<Transaction> transactions = walletDAO.getTransactionsByUserId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("transactions", transactions);
        return response;
    }
}