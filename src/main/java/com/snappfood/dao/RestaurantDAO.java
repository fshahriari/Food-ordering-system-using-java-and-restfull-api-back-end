package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.Food;
import com.snappfood.model.FoodCategory;
import com.snappfood.model.Restaurant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for handling database operations related to Restaurants and Food items.
 * This version dynamically creates a separate menu table for each restaurant.
 */
public class RestaurantDAO {

    private static final String RESTAURANTS_TABLE = "restaurants";
    private static final String PENDING_RESTAURANTS_TABLE = "pending_restaurants";
    private static final String FOOD_ITEMS_TABLE = "food_items";
    private static final String RESTAURANT_SELLERS_TABLE = "restaurant_sellers";

    /**
     * Creates a dedicated menu table for a newly created restaurant.
     * WARNING: DYNAMIC TABLE CREATION IS AN ADVANCED OPERATION AND CAN BE RISKY.
     * @param restaurantId The ID of the restaurant for which to create a menu table.
     * @throws SQLException if a database access error occurs.
     */
    public void createMenuTableForRestaurant(int restaurantId) throws SQLException {
        String menuTableName = "menu_" + restaurantId;
        String sql = "CREATE TABLE IF NOT EXISTS " + menuTableName + " (" +
                "    food_item_id INT NOT NULL," +
                "    supply INT NOT NULL," +
                "    PRIMARY KEY (food_item_id)," +
                "    FOREIGN KEY (food_item_id) REFERENCES food_items(id) ON DELETE CASCADE" +
                ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public int createPendingRestaurant(Restaurant restaurant) throws SQLException {
        String sql = "INSERT INTO " + PENDING_RESTAURANTS_TABLE + " (name, logo_base64, address, phone_number, working_hours, category) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, restaurant.getName());
            stmt.setString(2, restaurant.getLogoBase64());
            stmt.setString(3, restaurant.getAddress());
            stmt.setString(4, restaurant.getPhoneNumber());
            stmt.setString(5, restaurant.getWorkingHours());
            stmt.setString(6, restaurant.getCategory());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating pending restaurant failed, no ID obtained.");
                }
            }
        }
    }

    public Restaurant getRestaurantById(int restaurantId) throws SQLException {
        String sql = "SELECT * FROM " + RESTAURANTS_TABLE + " WHERE id = ?";
        Restaurant restaurant = null;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    restaurant = extractRestaurantFromResultSet(rs);
                    restaurant.setSellerPhoneNumbers(getSellersForRestaurant(restaurantId));
                }
            }
        }
        return restaurant;
    }

    public List<Restaurant> getRestaurantsBySellerPhoneNumber(String sellerPhoneNumber) throws SQLException {
        List<Restaurant> restaurants = new ArrayList<>();
        String sql = "SELECT r.* FROM " + RESTAURANTS_TABLE + " r " +
                "JOIN " + RESTAURANT_SELLERS_TABLE + " rs ON r.id = rs.restaurant_id " +
                "WHERE rs.seller_phone_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sellerPhoneNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    restaurants.add(extractRestaurantFromResultSet(rs));
                }
            }
        }
        return restaurants;
    }

    public boolean updateRestaurant(Restaurant restaurant) throws SQLException {
        String sql = "UPDATE " + RESTAURANTS_TABLE + " SET name = ?, logo_base64 = ?, address = ?, phone_number = ?, working_hours = ?, category = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, restaurant.getName());
            stmt.setString(2, restaurant.getLogoBase64());
            stmt.setString(3, restaurant.getAddress());
            stmt.setString(4, restaurant.getPhoneNumber());
            stmt.setString(5, restaurant.getWorkingHours());
            stmt.setString(6, restaurant.getCategory());
            stmt.setInt(7, restaurant.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteRestaurant(int restaurantId) throws SQLException {
        String sql = "DELETE FROM " + RESTAURANTS_TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            String menuTableName = "menu_" + restaurantId;
            stmt.execute("DROP TABLE IF EXISTS " + menuTableName);

            PreparedStatement deleteStmt = conn.prepareStatement(sql);
            deleteStmt.setInt(1, restaurantId);
            return deleteStmt.executeUpdate() > 0;
        }
    }

    public int addFoodItem(Food food, int supply) throws SQLException {
        String sql = "INSERT INTO " + FOOD_ITEMS_TABLE + " (name, image_base64, description, price, category) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, food.getName());
            stmt.setString(2, food.getImageBase64());
            stmt.setString(3, food.getDescription());
            stmt.setInt(4, food.getPrice());
            stmt.setString(5, food.getCategory().getValue());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int foodId = generatedKeys.getInt(1);
                    addFoodItemToMenu(food.getRestaurantId(), foodId, supply);
                    return foodId;
                } else {
                    throw new SQLException("Creating food item failed, no ID obtained.");
                }
            }
        }
    }

    public void addFoodItemToMenu(int restaurantId, int foodId, int supply) throws SQLException {
        String menuTableName = "menu_" + restaurantId;
        String sql = "INSERT INTO " + menuTableName + " (food_item_id, supply) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, foodId);
            stmt.setInt(2, supply);
            stmt.executeUpdate();
        }
    }

    public List<Food> getMenuForRestaurant(int restaurantId) throws SQLException {
        List<Food> menu = new ArrayList<>();
        String menuTableName = "menu_" + restaurantId;
        String sql = "SELECT f.*, mi.supply FROM " + FOOD_ITEMS_TABLE + " f " +
                "JOIN " + menuTableName + " mi ON f.id = mi.food_item_id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Food food = extractFoodFromResultSet(rs);
                    food.setSupply(rs.getInt("supply"));
                    menu.add(food);
                }
            }
        }
        return menu;
    }

    public boolean updateFoodItem(Food food, int supply) throws SQLException {
        String sql = "UPDATE " + FOOD_ITEMS_TABLE + " SET name = ?, image_base64 = ?, description = ?, price = ?, category = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, food.getName());
            stmt.setString(2, food.getImageBase64());
            stmt.setString(3, food.getDescription());
            stmt.setInt(4, food.getPrice());
            stmt.setString(5, food.getCategory().getValue());
            stmt.setInt(6, food.getId());

            updateMenuItemSupply(food.getRestaurantId(), food.getId(), supply);

            return stmt.executeUpdate() > 0;
        }
    }

    public void updateMenuItemSupply(int restaurantId, int foodId, int supply) throws SQLException {
        String menuTableName = "menu_" + restaurantId;
        String sql = "UPDATE " + menuTableName + " SET supply = ? WHERE food_item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, supply);
            stmt.setInt(2, foodId);
            stmt.executeUpdate();
        }
    }

    public boolean deleteFoodItem(int foodId) throws SQLException {
        String sql = "DELETE FROM " + FOOD_ITEMS_TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, foodId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Restaurant extractRestaurantFromResultSet(ResultSet rs) throws SQLException {
        Restaurant restaurant = new Restaurant(rs.getInt("tax_fee"), rs.getInt("additional_fee"));
        restaurant.setId(rs.getInt("id"));
        restaurant.setName(rs.getString("name"));
        restaurant.setLogoBase64(rs.getString("logo_base64"));
        restaurant.setAddress(rs.getString("address"));
        restaurant.setPhoneNumber(rs.getString("phone_number"));
        restaurant.setWorkingHours(rs.getString("working_hours"));
        restaurant.setCategory(rs.getString("category"));
        return restaurant;
    }

    private Food extractFoodFromResultSet(ResultSet rs) throws SQLException {
        Food food = new Food();
        food.setId(rs.getInt("id"));
        food.setName(rs.getString("name"));
        food.setImageBase64(rs.getString("image_base64"));
        food.setDescription(rs.getString("description"));
        food.setPrice(rs.getInt("price"));
        food.setCategory(FoodCategory.fromString(rs.getString("category")));
        return food;
    }

    public List<String> getSellersForRestaurant(int restaurantId) throws SQLException {
        List<String> sellerPhoneNumbers = new ArrayList<>();
        String sql = "SELECT seller_phone_number FROM " + RESTAURANT_SELLERS_TABLE + " WHERE restaurant_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sellerPhoneNumbers.add(rs.getString("seller_phone_number"));
                }
            }
        }
        return sellerPhoneNumbers;
    }

    public Restaurant getRestaurantByPhoneNumber(String phoneNumber) throws SQLException {
        String sql = "SELECT * FROM restaurants WHERE phone_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phoneNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractRestaurantFromResultSet(rs);
                }
            }
        }
        return null;
    }
}
