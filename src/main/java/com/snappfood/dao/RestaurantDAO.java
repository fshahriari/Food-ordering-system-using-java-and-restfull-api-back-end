package com.snappfood.dao;

import com.snappfood.database.DatabaseManager;
import com.snappfood.model.Food;
import com.snappfood.model.FoodCategory;
import com.snappfood.model.Menu;
import com.snappfood.model.Restaurant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDAO {

    private static final String RESTAURANTS_TABLE = "restaurants";
    private static final String PENDING_RESTAURANTS_TABLE = "pending_restaurants";
    private static final String FOOD_ITEMS_TABLE = "food_items";
    private static final String MENUS_TABLE = "menus";
    private static final String MENU_ITEMS_TABLE = "menu_items";
    private static final String RESTAURANT_SELLERS_TABLE = "restaurant_sellers";

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


    /**
     * Adds a food item to a restaurant's master food list.
     */
    public int addFoodItem(Food food) throws SQLException {
        String sql = "INSERT INTO " + FOOD_ITEMS_TABLE + " (name, image_base64, description, price, category, supply, restaurant_id, keywords) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, food.getName());
            stmt.setString(2, food.getImageBase64());
            stmt.setString(3, food.getDescription());
            stmt.setInt(4, food.getPrice());
            stmt.setString(5, food.getCategory().getValue());
            stmt.setInt(6, food.getSupply());
            stmt.setInt(7, food.getRestaurantId());

            if (food.getKeywords() != null && !food.getKeywords().isEmpty()) {
                stmt.setString(8, String.join(",", food.getKeywords()));
            } else {
                stmt.setNull(8, Types.VARCHAR);
            }


            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating food item failed, no ID obtained.");
                }
            }
        }
    }


    /**
     * Creates a new titled menu for a restaurant.
     */
    public int createMenu(Menu menu) throws SQLException {
        String sql = "INSERT INTO " + MENUS_TABLE + " (restaurant_id, title) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, menu.getRestaurantId());
            stmt.setString(2, menu.getTitle());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating menu failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Deletes a titled menu and all its item associations.
     */
    public boolean deleteMenuById(int menuId) throws SQLException {
        String sql = "DELETE FROM " + MENUS_TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, menuId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Adds an existing food item from the master list to a specific titled menu.
     */
    public void addFoodItemToMenu(int menuId, int foodItemId) throws SQLException {
        String sql = "INSERT INTO " + MENU_ITEMS_TABLE + " (menu_id, food_item_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, menuId);
            stmt.setInt(2, foodItemId);
            stmt.executeUpdate();
        }
    }

    /**
     * Updates an existing food item in the master list.
     */
    public boolean updateFoodItem(Food food) throws SQLException {
        String sql = "UPDATE " + FOOD_ITEMS_TABLE + " SET name = ?, description = ?, price = ?, supply = ?, category = ?, image_base64 = ?, keywords = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, food.getName());
            stmt.setString(2, food.getDescription());
            stmt.setInt(3, food.getPrice());
            stmt.setInt(4, food.getSupply());
            stmt.setString(5, food.getCategory().getValue());
            stmt.setString(6, food.getImageBase64());
            if (food.getKeywords() != null && !food.getKeywords().isEmpty()) {
                stmt.setString(7, String.join(",", food.getKeywords()));
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }
            stmt.setInt(8, food.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a food item from the master list.
     */
    public boolean deleteFoodItem(int foodId) throws SQLException {
        String sql = "DELETE FROM " + FOOD_ITEMS_TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, foodId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean menuTitleExists(int restaurantId, String title) throws SQLException {
        return getMenuByTitle(restaurantId, title) != null;
    }

    public boolean isItemInMenu(int menuId, int foodItemId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + MENU_ITEMS_TABLE + " WHERE menu_id = ? AND food_item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, menuId);
            stmt.setInt(2, foodItemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean isFoodItemInAnyMenu(int foodId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + MENU_ITEMS_TABLE + " WHERE food_item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, foodId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Removes a food item from a specific titled menu.
     */
    public boolean removeItemFromMenu(int menuId, int foodItemId) throws SQLException {
        String sql = "DELETE FROM " + MENU_ITEMS_TABLE + " WHERE menu_id = ? AND food_item_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, menuId);
            stmt.setInt(2, foodItemId);
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

    /**
     * Retrieves all restaurants from the pending_restaurants table,
     * sorted alphabetically by their name.
     * @return A sorted list of pending Restaurant objects.
     * @throws SQLException if a database access error occurs.
     */
    public List<Restaurant> getPendingRestaurantsSortedByName() throws SQLException {
        List<Restaurant> pendingRestaurants = new ArrayList<>();
        String sql = "SELECT * FROM " + PENDING_RESTAURANTS_TABLE + " ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                pendingRestaurants.add(extractRestaurantFromResultSet(rs));
            }
        }
        return pendingRestaurants;
    }

    private Food extractFoodFromResultSet(ResultSet rs) throws SQLException {
        Food food = new Food();
        food.setId(rs.getInt("id"));
        food.setName(rs.getString("name"));
        food.setImageBase64(rs.getString("image_base64"));
        food.setDescription(rs.getString("description"));
        food.setPrice(rs.getInt("price"));
        food.setCategory(FoodCategory.fromString(rs.getString("category")));
        food.setSupply(rs.getInt("supply"));
        food.setRestaurantId(rs.getInt("restaurant_id"));
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

    public Food getFoodItemById(int foodId) throws SQLException {
        String sql = "SELECT * FROM " + FOOD_ITEMS_TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, foodId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractFoodFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean isRestaurantPending(int restaurantId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + PENDING_RESTAURANTS_TABLE + " WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<Restaurant> getPendingRestaurantsBySellerPhoneNumber(String sellerPhoneNumber) throws SQLException {
        List<Restaurant> restaurants = new ArrayList<>();
        String sql = "SELECT r.* FROM " + PENDING_RESTAURANTS_TABLE + " r " +
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

    public Menu getMenuByTitle(int restaurantId, String title) throws SQLException {
        String sql = "SELECT * FROM " + MENUS_TABLE + " WHERE restaurant_id = ? AND title = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, restaurantId);
            stmt.setString(2, title);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Menu menu = new Menu();
                    menu.setId(rs.getInt("id"));
                    menu.setRestaurantId(rs.getInt("restaurant_id"));
                    menu.setTitle(rs.getString("title"));
                    return menu;
                }
            }
        }
        return null;
    }

    public boolean isFoodItemInActiveOrder(int foodId) throws SQLException {
        //TODO
        // This is a placeholder. You'll need to implement the actual logic
        // to check if the food item is in an order that is not yet completed.
        return false;
    }

    public boolean isMenuItemInActiveOrder(int restaurantId) throws SQLException {
        //TODO
        // This is a placeholder. You'll need to implement the actual logic to check
        // if any food item in the menu is part of an active order.
        return false;
    }
}
