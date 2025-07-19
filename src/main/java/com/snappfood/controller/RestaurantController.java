package com.snappfood.controller;

import com.snappfood.dao.RestaurantDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.Food;
import com.snappfood.model.Restaurant;
import com.snappfood.model.Role;
import com.snappfood.model.User;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the business logic for restaurant and food-related operations.
 */
public class RestaurantController {

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final UserDAO userDAO = new UserDAO();

    private static final int MAX_RESTAURANT_CREATION_REQUESTS = 3;
    private static final long CREATION_RATE_LIMIT_WINDOW_MS = 3600000;
    private final Map<Integer, RequestTracker> restaurantCreationTrackers = new ConcurrentHashMap<>();

    private static final int MAX_FETCH_RESTAURANTS_REQUESTS = 20;
    private static final long FETCH_RATE_LIMIT_WINDOW_MS = 60000;
    private final Map<Integer, RequestTracker> fetchRestaurantsTrackers = new ConcurrentHashMap<>();


    public Map<String, Object> handleCreateRestaurant(Restaurant restaurant, Integer sellerId) throws Exception {
        if (sellerId == null) {
            throw new UnauthorizedException("You must be logged in to create a restaurant.");
        }
        RequestTracker tracker = restaurantCreationTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_RESTAURANT_CREATION_REQUESTS, CREATION_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You have made too many restaurant creation requests. Please try again later.");
        }
        User seller = userDAO.findUserById(sellerId);
        if (seller == null || seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only users with the 'seller' role can create restaurants.");
        }
        if (userDAO.isUserPending(seller.getPhone())) {
            throw new ForbiddenException("Your seller account is pending approval. You cannot create a restaurant yet.");
        }
        if (restaurant.getName() == null || restaurant.getName().trim().isEmpty()) {
            throw new InvalidInputException("Restaurant name is required.");
        }
        if (restaurant.getAddress() == null || restaurant.getAddress().trim().isEmpty()) {
            throw new InvalidInputException("Restaurant address is required.");
        }
        if (restaurant.getPhoneNumber() == null || !restaurant.getPhoneNumber().matches("^[0-9]{10,15}$")) {
            throw new InvalidInputException("A valid phone number is required.");
        }
        if (restaurant.getTaxFee() < 0 || restaurant.getAdditionalFee() < 0) {
            throw new InvalidInputException("Fees cannot be negative.");
        }
        int pendingRestaurantId = restaurantDAO.createPendingRestaurant(restaurant);
        restaurant.setId(pendingRestaurantId);
        Map<String, Object> response = new HashMap<>();
        response.put("status", 201);
        response.put("message", "Restaurant registration request submitted successfully. Waiting for admin approval.");
        response.put("restaurant", restaurant);
        return response;
    }

    /**
     * Handles fetching the list of restaurants owned by a specific seller.
     * @param sellerId The ID of the authenticated seller.
     * @return A map containing a list of the seller's restaurants.
     * @throws Exception for authorization or database errors.
     */
    public Map<String, Object> handleGetMyRestaurants(Integer sellerId) throws Exception {
        //401
        if (sellerId == null) {
            throw new UnauthorizedException("You must be logged in to view your restaurants.");
        }

        //429
        RequestTracker tracker = fetchRestaurantsTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_FETCH_RESTAURANTS_REQUESTS, FETCH_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("Too many requests. Please try again later.");
        }

        // Fetch user for authorization
        User seller = userDAO.findUserById(sellerId);

        //404
        if (seller == null) {
            throw new ResourceNotFoundException("The specified seller account does not exist.");
        }

        //403
        if (seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only users with the 'seller' role can view their restaurants.");
        }
        if (userDAO.isUserPending(seller.getPhone())) {
            throw new ForbiddenException("Your seller account is pending approval.");
        }

        List<Restaurant> restaurants = restaurantDAO.getRestaurantsBySellerPhoneNumber(seller.getPhone());

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("restaurants", restaurants);
        return response;
    }

    public Map<String, Object> handleAddFoodItem(Integer restaurantId, Food food, int supply, Integer sellerId) throws Exception {
        // TODO: Implement logic
        return null; // Placeholder
    }

    public Map<String, Object> handleGetRestaurantMenu(Integer restaurantId) throws Exception {
        // TODO: Implement logic
        return null; // Placeholder
    }

    private static class RequestTracker {
        private int requestCount;
        private long windowStartTime;
        private final int maxRequests;
        private final long windowMs;

        public RequestTracker(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
            this.requestCount = 0;
            this.windowStartTime = System.currentTimeMillis();
        }

        public synchronized boolean allowRequest() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - windowStartTime > windowMs) {
                windowStartTime = currentTime;
                requestCount = 1;
                return true;
            }
            if (requestCount < maxRequests) {
                requestCount++;
                return true;
            }
            return false;
        }
    }
}