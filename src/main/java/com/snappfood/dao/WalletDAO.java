package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for handling all database operations related to wallets and transactions.
 */
public class WalletDAO {

    private static final String WALLETS_TABLE = "wallets";
    private static final String TRANSACTIONS_TABLE = "transactions";

    /**
     * Creates a new wallet for a user, typically upon registration.
     * @param userId The ID of the user.
     * @throws SQLException if a database error occurs.
     */
    public void createWallet(int userId) throws SQLException {
        String sql = "INSERT INTO " + WALLETS_TABLE + " (user_id, balance) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, 0); // Initial balance is 0
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves the wallet for a specific user.
     * @param userId The ID of the user.
     * @return The Wallet object, or null if not found.
     * @throws SQLException if a database error occurs.
     */
    public Wallet getWalletByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM " + WALLETS_TABLE + " WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(rs.getInt("user_id"));
                    wallet.setBalance(rs.getInt("balance"));
                    wallet.setCreatedAt(rs.getTimestamp("created_at"));
                    wallet.setUpdatedAt(rs.getTimestamp("updated_at"));
                    return wallet;
                }
            }
        }
        return null;
    }

    /**
     * Adds funds to a user's wallet and logs the transaction.
     * This is done within a transaction to ensure atomicity.
     * @param userId The ID of the user.
     * @param amount The amount to add.
     * @throws SQLException if a database error occurs.
     */
    public void topUpWallet(int userId, int amount) throws SQLException {
        Connection conn = null;
        String updateWalletSql = "UPDATE " + WALLETS_TABLE + " SET balance = balance + ? WHERE user_id = ?";
        String logTransactionSql = "INSERT INTO " + TRANSACTIONS_TABLE + " (user_id, amount, type, status) VALUES (?, ?, ?, ?)";

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // Update wallet balance
            try (PreparedStatement stmt = conn.prepareStatement(updateWalletSql)) {
                stmt.setInt(1, amount);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }

            // Log the transaction
            try (PreparedStatement stmt = conn.prepareStatement(logTransactionSql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, amount);
                stmt.setString(3, "WALLET_TOP_UP");
                stmt.setString(4, "SUCCESS");
                stmt.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Processes the payment for a completed order by transferring funds
     * from the customer's wallet to the seller's and courier's wallets.
     * This entire operation is performed within a single database transaction.
     * @param order The completed order.
     * @param sellerId The ID of the seller.
     * @param courierId The ID of the courier.
     * @throws SQLException if the transaction fails.
     */
    public void processOrderPayment(Order order, int sellerId, int courierId) throws SQLException {
        Connection conn = null;
        String updateWalletSql = "UPDATE " + WALLETS_TABLE + " SET balance = balance + ? WHERE user_id = ?";
        String logTransactionSql = "INSERT INTO " + TRANSACTIONS_TABLE + " (user_id, order_id, amount, type, status) VALUES (?, ?, ?, ?, ?)";

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. Pay the seller (raw price + additional fees)
            int sellerAmount = order.getRawPrice() + order.getAdditionalFee();
            try (PreparedStatement stmt = conn.prepareStatement(updateWalletSql)) {
                stmt.setInt(1, sellerAmount);
                stmt.setInt(2, sellerId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(logTransactionSql)) {
                stmt.setInt(1, sellerId);
                stmt.setInt(2, order.getId());
                stmt.setInt(3, sellerAmount);
                stmt.setString(4, "ORDER_PAYMENT");
                stmt.setString(5, "SUCCESS");
                stmt.executeUpdate();
            }

            // 2. Pay the courier
            int courierAmount = order.getCourierFee();
            try (PreparedStatement stmt = conn.prepareStatement(updateWalletSql)) {
                stmt.setInt(1, courierAmount);
                stmt.setInt(2, courierId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(logTransactionSql)) {
                stmt.setInt(1, courierId);
                stmt.setInt(2, order.getId());
                stmt.setInt(3, courierAmount);
                stmt.setString(4, "ORDER_PAYMENT");
                stmt.setString(5, "SUCCESS");
                stmt.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Retrieves the transaction history for a specific user.
     * @param userId The ID of the user.
     * @return A list of Transaction objects, sorted by the most recent.
     * @throws SQLException if a database error occurs.
     */
    public List<Transaction> getTransactionsByUserId(int userId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM " + TRANSACTIONS_TABLE + " WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();
                    transaction.setId(rs.getInt("id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setOrderId((Integer) rs.getObject("order_id"));
                    transaction.setAmount(rs.getInt("amount"));
                    transaction.setType(TransactionType.valueOf(rs.getString("type")));
                    transaction.setStatus(TransactionStatus.valueOf(rs.getString("status")));
                    transaction.setCreatedAt(rs.getTimestamp("created_at"));
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }
}