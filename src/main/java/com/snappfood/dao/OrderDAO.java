package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.exception.ConflictException;
import com.snappfood.exception.ResourceNotFoundException;
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
    private static final String USERS_TABLE = "users";

    /**
     * Creates a new order in the database within a single transaction.
     * This includes creating the order record, adding items, and decrementing stock.
     * @param order The Order object to be created.
     * @return The complete Order object with its new ID, or null if the transaction fails.
     */
    public Order createOrder(Order order) throws SQLException {
        Connection conn = null;
        String insertOrderSQL = "INSERT INTO " + ORDERS_TABLE +
                " (customer_id, restaurant_id, status, delivery_address, raw_price, tax_fee, additional_fee, courier_fee, pay_price, coupon_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            orderStmt.setTimestamp(11, order.getCreatedAt());
            orderStmt.setTimestamp(12, order.getUpdatedAt());
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

    /**
     * Retrieves a single, complete order by its ID, including all its items.
     * @param orderId The ID of the order to fetch.
     * @return The complete Order object, or null if not found.
     * @throws SQLException if a database error occurs.
     */
    public Order getOrderById(int orderId) throws SQLException {
        Order order = null;
        String sql = "SELECT * FROM " + ORDERS_TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    order = extractOrderFromResultSet(rs);
                    order.setItems(getOrderItems(orderId)); // Fetch and attach the items
                }
            }
        }
        return order;
    }

    private Map<Integer, Integer> getOrderItems(int orderId) throws SQLException {
        Map<Integer, Integer> items = new HashMap<>();
        String sql = "SELECT food_item_id, quantity FROM " + ORDER_ITEMS_TABLE + " WHERE order_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.put(rs.getInt("food_item_id"), rs.getInt("quantity"));
                }
            }
        }
        return items;
    }

    /**
     * Retrieves orders for a specific restaurant, with optional filters.
     * @param restaurantId The ID of the restaurant.
     * @param filters A map of query parameters (status, search, user, courier).
     * @return A list of matching Order objects.
     * @throws SQLException if a database error occurs.
     */
    public List<Order> getOrdersForRestaurant(int restaurantId, Map<String, String> filters) throws SQLException {
        List<Order> orders = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT o.* FROM " + ORDERS_TABLE + " o " +
                        "LEFT JOIN " + USERS_TABLE + " c ON o.customer_id = c.id " +
                        "LEFT JOIN " + USERS_TABLE + " cr ON o.courier_id = cr.id " +
                        "WHERE o.restaurant_id = ? " +
                        "AND o.status NOT IN ('PENDING_ADMIN_APPROVAL', 'REJECTED_BY_ADMIN')"
        );

        List<Object> params = new ArrayList<>();
        params.add(restaurantId);

        if (filters.containsKey("status")) {
            sql.append(" AND o.status = ?");
            params.add(filters.get("status").toUpperCase());
        }
        if (filters.containsKey("search")) {
            sql.append(" AND c.full_name LIKE ?");
            params.add("%" + filters.get("search") + "%");
        }
        if (filters.containsKey("user")) {
            sql.append(" AND c.full_name LIKE ?");
            params.add("%" + filters.get("user") + "%");
        }
        if (filters.containsKey("courier")) {
            sql.append(" AND cr.full_name LIKE ?");
            params.add("%" + filters.get("courier") + "%");
        }

        sql.append(" ORDER BY o.created_at DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = extractOrderFromResultSet(rs);
                    order.setItems(getOrderItems(order.getId()));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    /**
     * A general method to update the status of an order in the database.
     * It handles the database transaction and returns stock for cancelled/rejected orders.
     * The business logic for valid state transitions should be handled in the controller.
     * @param orderId The ID of the order to update.
     * @param newStatus The new status to set for the order.
     * @throws SQLException for database access errors.
     * @throws ResourceNotFoundException if the order with the given ID is not found.
     */
    public void updateOrderStatus(int orderId, OrderStatus newStatus, Integer courierId) throws SQLException, ResourceNotFoundException {
        Connection conn = null;
        String updateSql;
        if (courierId != null) {
            updateSql = "UPDATE " + ORDERS_TABLE + " SET status = ?, courier_id = ?, updated_at = ? WHERE id = ?";
        } else {
            updateSql = "UPDATE " + ORDERS_TABLE + " SET status = ?, updated_at = ? WHERE id = ?";
        }

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // If the new status is a cancellation or rejection, return stock
            if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REJECTED_BY_VENDOR) {
                returnStockForOrder(orderId, conn);
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                int paramIndex = 1;
                stmt.setString(paramIndex++, newStatus.name());
                if (courierId != null) {
                    stmt.setInt(paramIndex++, courierId);
                }
                stmt.setTimestamp(paramIndex++, new Timestamp(System.currentTimeMillis()));
                stmt.setInt(paramIndex++, orderId);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new ResourceNotFoundException("Order with ID " + orderId + " not found during update.");
                }
            }

            conn.commit();

        } catch (SQLException | ResourceNotFoundException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e; // Re-throw to be handled by the controller
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Retrieves the order history for a specific customer, with optional filters.
     * @param customerId The ID of the customer.
     * @param filters A map of query parameters (date, status).
     * @return A list of matching Order objects.
     * @throws SQLException if a database error occurs.
     */
    public List<Order> getOrderHistoryForCustomer(int customerId, Map<String, String> filters) throws SQLException {
        List<Order> orders = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + ORDERS_TABLE + " WHERE customer_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(customerId);

        if (filters.containsKey("date")) {
            sql.append(" AND DATE(created_at) = ?");
            params.add(filters.get("date"));
        }
        if (filters.containsKey("status")) {
            sql.append(" AND status = ?");
            params.add(filters.get("status").toUpperCase());
        }

        sql.append(" ORDER BY created_at DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(extractOrderFromResultSet(rs));
                }
            }
        }
        return orders;
    }

    /**
     * Retrieves all orders that are ready for pickup and have not been assigned a courier.
     * @return A list of available Order objects.
     * @throws SQLException if a database error occurs.
     */
    public List<Order> getAvailableDeliveries() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM " + ORDERS_TABLE + " WHERE status = 'READY_FOR_PICKUP' AND courier_id IS NULL ORDER BY created_at ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                orders.add(extractOrderFromResultSet(rs));
            }
        }
        return orders;
    }
}