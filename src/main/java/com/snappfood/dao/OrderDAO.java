package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.Order;
import com.snappfood.model.OrderStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Retrieves all orders with PENDING_ADMIN_APPROVAL status, sorted by creation date.
     */
    public List<Order> getPendingAdminOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM " + ORDERS_TABLE + " WHERE status = ? ORDER BY created_at ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, OrderStatus.PENDING_ADMIN_APPROVAL.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(extractOrderFromResultSet(rs));
                }
            }
        }
        return orders;
    }

    /**
     * Updates a batch of pending orders in a single transaction.
     */
    public void updatePendingOrdersBatch(List<Order> orderUpdates) throws SQLException {
        Connection conn = null;
        String checkOrderSql = "SELECT status FROM " + ORDERS_TABLE + " WHERE id = ?";
        String updateStatusSql = "UPDATE " + ORDERS_TABLE + " SET status = ? WHERE id = ?";

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction

            PreparedStatement checkStmt = conn.prepareStatement(checkOrderSql);
            PreparedStatement updateStmt = conn.prepareStatement(updateStatusSql);

            for (Order update : orderUpdates) {
                checkStmt.setInt(1, update.getId());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next() || !OrderStatus.PENDING_ADMIN_APPROVAL.name().equals(rs.getString("status"))) {
                        throw new SQLException("Order with ID " + update.getId() + " is not a valid pending order.");
                    }
                }

                if (update.getStatus() == OrderStatus.PENDING_VENDOR_APPROVAL) {
                    updateStmt.setString(1, OrderStatus.PENDING_VENDOR_APPROVAL.name());
                    updateStmt.setInt(2, update.getId());
                    updateStmt.addBatch();
                } else if (update.getStatus() == OrderStatus.REJECTED_BY_ADMIN) {
                    updateStmt.setString(1, OrderStatus.REJECTED_BY_ADMIN.name());
                    updateStmt.setInt(2, update.getId());
                    updateStmt.addBatch();
                    returnStockForOrder(update.getId(), conn);
                }
            }

            updateStmt.executeBatch();
            conn.commit(); // Commit transaction

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
     * Helper method to return stock for a cancelled or rejected order.
     */
    private void returnStockForOrder(int orderId, Connection conn) throws SQLException {
        String getItemsSql = "SELECT food_item_id, quantity FROM " + ORDER_ITEMS_TABLE + " WHERE order_id = ?";
        String updateStockSql = "UPDATE " + FOOD_ITEMS_TABLE + " SET supply = supply + ? WHERE id = ?";

        Map<Integer, Integer> itemsToReturn = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(getItemsSql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itemsToReturn.put(rs.getInt("food_item_id"), rs.getInt("quantity"));
                }
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement(updateStockSql)) {
            for (Map.Entry<Integer, Integer> item : itemsToReturn.entrySet()) {
                stmt.setInt(1, item.getValue()); // quantity
                stmt.setInt(2, item.getKey());   // food_item_id
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private Order extractOrderFromResultSet(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setCustomerId(rs.getInt("customer_id"));
        order.setRestaurantId(rs.getInt("restaurant_id"));
        order.setCourierId((Integer) rs.getObject("courier_id"));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setDeliveryAddress(rs.getString("delivery_address"));
        order.setRawPrice(rs.getInt("raw_price"));
        order.setTaxFee(rs.getInt("tax_fee"));
        order.setAdditionalFee(rs.getInt("additional_fee"));
        order.setCourierFee(rs.getInt("courier_fee"));
        order.setPayPrice(rs.getInt("pay_price"));
        order.setCouponId((Integer) rs.getObject("coupon_id"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        order.setUpdatedAt(rs.getTimestamp("updated_at"));
        return order;
    }
}