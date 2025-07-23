package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.Order;
import com.snappfood.model.OrderStatus;

import java.sql.*;
import java.util.Map;

/**
 * Data Access Object for handling all database operations related to orders.
 */
public class OrderDAO {

    private static final String ORDERS_TABLE = "orders";
    private static final String ORDER_ITEMS_TABLE = "order_items";
    private static final String FOOD_ITEMS_TABLE = "food_items";

    /**
     * Creates a new order in the database within a single transaction.
     * This includes creating the order record, adding items, and decrementing stock.
     * @param order The Order object to be created.
     * @return The complete Order object with its new ID, or null if the transaction fails.
     */
    public Order createOrder(Order order) throws SQLException {
        Connection conn = null;
        String insertOrderSQL = "INSERT INTO " + ORDERS_TABLE +
                " (customer_id, restaurant_id, status, delivery_address, raw_price, tax_fee, additional_fee, courier_fee, pay_price, coupon_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertOrderItemSQL = "INSERT INTO " + ORDER_ITEMS_TABLE + " (order_id, food_item_id, quantity) VALUES (?, ?, ?)";
        String updateStockSQL = "UPDATE " + FOOD_ITEMS_TABLE + " SET supply = supply - ? WHERE id = ?";

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement orderStmt = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS);
            orderStmt.setInt(1, order.getCustomerId());
            orderStmt.setInt(2, order.getRestaurantId());
            orderStmt.setString(3, order.getStatus().name());
            orderStmt.setString(4, order.getDeliveryAddress());
            orderStmt.setInt(5, order.getRawPrice());
            orderStmt.setInt(6, order.getTaxFee());
            orderStmt.setInt(7, order.getAdditionalFee());
            orderStmt.setInt(8, order.getCourierFee());
            orderStmt.setInt(9, order.getPayPrice());
            if (order.getCouponId() != null) {
                orderStmt.setInt(10, order.getCouponId());
            } else {
                orderStmt.setNull(10, Types.INTEGER);
            }
            orderStmt.executeUpdate();

            ResultSet generatedKeys = orderStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                order.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Creating order failed, no ID obtained.");
            }

            PreparedStatement orderItemStmt = conn.prepareStatement(insertOrderItemSQL);
            PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSQL);

            for (Map.Entry<Integer, Integer> entry : order.getItems().entrySet()) {
                int foodId = entry.getKey();
                int quantity = entry.getValue();

                orderItemStmt.setInt(1, order.getId());
                orderItemStmt.setInt(2, foodId);
                orderItemStmt.setInt(3, quantity);
                orderItemStmt.addBatch();

                updateStockStmt.setInt(1, quantity);
                updateStockStmt.setInt(2, foodId);
                updateStockStmt.addBatch();
            }
            orderItemStmt.executeBatch();
            updateStockStmt.executeBatch();

            conn.commit();
            return order;

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
}