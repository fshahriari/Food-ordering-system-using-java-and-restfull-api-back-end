package com.snappfood.controller;

import com.snappfood.dao.OrderDAO;
import com.snappfood.dao.RatingDAO;
import com.snappfood.dao.RestaurantDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the business logic for customer-facing operations.
 */
public class CustomerController {

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final UserDAO userDAO = new UserDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    /**
     * Handles fetching the detailed view of a single restaurant, including its menus and food items.
     *
     * @param userId       The ID of the authenticated user.
     * @param restaurantId The ID of the restaurant to fetch.
     * @return A map containing the restaurant's details and its structured menu.
     * @throws Exception for authorization, validation, or database errors.
     */
    public Map<String, Object> handleGetVendorDetails(Integer userId, int restaurantId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view restaurant details.");
        }

        Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (restaurant == null || restaurantDAO.isRestaurantPending(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found or is not active.");
        }

        List<Menu> menus = restaurantDAO.getMenusForRestaurant(restaurantId);

        Map<String, Object> response = new HashMap<>();
        response.put("vendor", restaurant);

        List<String> menuTitles = new ArrayList<>();
        for (Menu menu : menus) {
            menuTitles.add(menu.getTitle());
            List<Food> foodItems = restaurantDAO.getFoodItemsForMenu(menu.getId());
            response.put(menu.getTitle(), foodItems);
        }
        response.put("menu_titles", menuTitles);

        response.put("status", 200);
        return response;
    }

    /**
     * Handles fetching the details of a single food item, ensuring it belongs to an approved restaurant.
     *
     * @param userId The ID of the authenticated user.
     * @param itemId The ID of the food item to fetch.
     * @return A map containing the food item's details.
     * @throws Exception for authorization, validation, or database errors.
     */
    public Map<String, Object> handleGetItemDetails(Integer userId, int itemId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view item details.");
        }

        Food foodItem = restaurantDAO.getFoodItemIfRestaurantIsApproved(itemId);

        if (foodItem == null) {
            throw new ResourceNotFoundException("Food item with ID " + itemId + " not found or is not available.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("food_item", foodItem);
        return response;
    }

    /**
     * Handles adding a restaurant to a customer's favorites list.
     *
     * @param userId       The ID of the authenticated customer.
     * @param restaurantId The ID of the restaurant to favorite.
     * @return A map with a success message.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleAddFavoriteRestaurant(Integer userId, int restaurantId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to add a favorite.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("Only customers can add restaurants to favorites.");
        }

        Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (restaurant == null || restaurantDAO.isRestaurantPending(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found or is not active.");
        }

        restaurantDAO.addFavoriteRestaurant(userId, restaurantId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Restaurant added to favorites successfully.");
        return response;
    }

    /**
     * Handles fetching the list of favorite restaurants for a customer.
     *
     * @param userId The ID of the authenticated customer.
     * @return A map containing the list of favorite restaurants.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleGetFavoriteRestaurants(Integer userId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view your favorites.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("Only customers can view favorites.");
        }

        List<Restaurant> favoriteRestaurants = restaurantDAO.getFavoriteRestaurantsByCustomerId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("restaurants", favoriteRestaurants);
        return response;
    }

    /**
     * Handles removing a restaurant from a customer's favorites list.
     *
     * @param userId       The ID of the authenticated customer.
     * @param restaurantId The ID of the restaurant to remove from favorites.
     * @return A map with a success message.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleRemoveFavoriteRestaurant(Integer userId, int restaurantId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to remove a favorite.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("Only customers can remove restaurants from favorites.");
        }

        // Check if the restaurant exists to provide a better error message, though not strictly required for DELETE.
        Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (restaurant == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }

        restaurantDAO.removeFavoriteRestaurant(userId, restaurantId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Removed from favorites");
        return response;
    }

    /**
     * Handles the submission of a new rating for an order.
     *
     * @param userId The ID of the authenticated customer.
     * @param rating The rating object parsed from the request body.
     * @return A map with a success message.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleSubmitRating(Integer userId, Rating rating) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to submit a rating.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("Only customers can submit ratings.");
        }

        if (rating.getOrderId() <= 0) {
            throw new InvalidInputException("A valid order_id is required.");
        }
        if (rating.getRating() < 1 || rating.getRating() > 5) {
            throw new InvalidInputException("Rating must be between 1 and 5.");
        }

        Order order = orderDAO.getOrderById(rating.getOrderId());
        if (order == null) {
            throw new ResourceNotFoundException("Order with ID " + rating.getOrderId() + " not found.");
        }

        if (order.getCustomerId() != userId) {
            throw new ForbiddenException("You can only rate your own orders.");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ConflictException("You can only rate completed orders.");
        }

        rating.setCustomerId(userId);
        rating.setRestaurantId(order.getRestaurantId());

        try {
            ratingDAO.addRating(rating);
        } catch (SQLException e) {
            if (e.getSQLState().equals("23000")) { // Integrity constraint violation
                throw new ConflictException("You have already rated this order.");
            }
            throw e; // Re-throw other SQL errors
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Rating submitted successfully.");
        return response;
    }

    /**
     * Handles listing vendors with optional filters and sorting.
     *
     * @param userId  The ID of the authenticated user.
     * @param filters A map containing the filter and sort criteria.
     * @return A map containing the list of matching restaurants.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleListVendors(Integer userId, Map<String, Object> filters) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view vendors.");
        }

        User user = userDAO.findUserById(userId);
        if (user == null || user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("Only customers can view vendors.");
        }

        if (filters.containsKey("categories")) {
            List<String> categories = (List<String>) filters.get("categories");
            for (String category : categories) {
                try {
                    FoodCategory.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new InvalidInputException("Invalid category provided: " + category);
                }
            }
        }

        List<Restaurant> restaurants = restaurantDAO.findActiveRestaurants(filters);
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("restaurants", restaurants);
        return response;
    }

    /**
     * Handles listing food items with optional filters and sorting.
     *
     * @param userId  The ID of the authenticated user.
     * @param filters A map containing the filter and sort criteria.
     * @return A map containing the list of matching food items.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleListItems(Integer userId, Map<String, Object> filters) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view items.");
        }

        if (filters.containsKey("categories")) {
            List<String> categories = (List<String>) filters.get("categories");
            for (String category : categories) {
                try {
                    FoodCategory.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new InvalidInputException("Invalid category provided: " + category);
                }
            }
        }

        List<Food> foodItems = restaurantDAO.findActiveFoodItems(filters);
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("items", foodItems);
        return response;
    }

    /**
     * Handles fetching ratings for a specific order.
     *
     * @param userId  The ID of the authenticated user.
     * @param orderId The ID of the order.
     * @return A map containing the list of ratings.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleGetRatingByOrderId(Integer userId, int orderId) throws Exception {
        if (userId == null) {
            throw new UnauthorizedException("You must be logged in to view ratings.");
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order with ID " + orderId + " not found.");
        }


        List<Rating> ratings = ratingDAO.getRatingsByOrderId(orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("ratings", ratings);
        return response;
    }
}
