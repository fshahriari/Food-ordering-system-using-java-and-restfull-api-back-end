package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.Rating;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
}