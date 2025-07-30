package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.Rating;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for handling database operations related to ratings.
 */
public class RatingDAO {

    private static final String RATINGS_TABLE = "ratings";

    /**
     * Inserts a new rating into the database.
     * @param rating The Rating object to be inserted.
     * @return The generated ID of the new rating.
     * @throws SQLException if a database error occurs, especially a duplicate entry for the order_id.
     */
    public int addRating(Rating rating) throws SQLException {
        String sql = "INSERT INTO " + RATINGS_TABLE + " (order_id, customer_id, restaurant_id, rating, comment) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, rating.getOrderId());
            stmt.setInt(2, rating.getCustomerId());
            stmt.setInt(3, rating.getRestaurantId());
            stmt.setInt(4, rating.getRating());
            stmt.setString(5, rating.getComment());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating rating failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Retrieves all ratings for a given order ID.
     * @param orderId The ID of the order.
     * @return A list of Rating objects.
     * @throws SQLException if a database error occurs.
     */
    public List<Rating> getRatingsByOrderId(int orderId) throws SQLException {
        List<Rating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM " + RATINGS_TABLE + " WHERE order_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ratings.add(extractRatingFromResultSet(rs));
                }
            }
        }
        return ratings;
    }

    private Rating extractRatingFromResultSet(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setId(rs.getInt("id"));
        rating.setOrderId(rs.getInt("order_id"));
        rating.setCustomerId(rs.getInt("customer_id"));
        rating.setRestaurantId(rs.getInt("restaurant_id"));
        rating.setRating(rs.getInt("rating"));
        rating.setComment(rs.getString("comment"));
        rating.setCreatedAt(rs.getTimestamp("created_at"));
        return rating;
    }
}