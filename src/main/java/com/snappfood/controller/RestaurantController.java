package com.snappfood.controller;

import com.snappfood.dao.RestaurantDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.ForbiddenException;
import com.snappfood.exception.InvalidInputException;
import com.snappfood.exception.TooManyRequestsException;
import com.snappfood.exception.UnauthorizedException;
import com.snappfood.model.Food;
import com.snappfood.model.Restaurant;
import com.snappfood.model.Role;
import com.snappfood.model.User;

import java.sql.SQLException;
import java.util.HashMap;
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

    /**
     * Handles the creation of a new restaurant by a seller.
     * @param restaurant The restaurant object from the request.
     * @param sellerId The ID of the authenticated seller.
     * @return A map containing the response data.
     * @throws Exception for validation, authorization, or database errors.
     */
    public Map<String, Object> handleCreateRestaurant(Restaurant restaurant, Integer sellerId) throws Exception {
        //401
        if (sellerId == null) {
            throw new UnauthorizedException("You must be logged in to create a restaurant.");
        }

        //429
        RequestTracker tracker = restaurantCreationTrackers.computeIfAbsent(sellerId, k -> new RequestTracker());
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You have made too many restaurant creation requests. Please try again later.");
        }

        User seller = userDAO.findUserById(sellerId);

        //403
        if (seller == null || seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only users with the 'seller' role can create restaurants.");
        }
        if (userDAO.isUserPending(seller.getPhone())) {
            throw new ForbiddenException("Your seller account is pending approval. You cannot create a restaurant yet.");
        }

        //400
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
        if (restaurant.getCategory() == null || restaurant.getCategory().trim().isEmpty()) {
            throw new InvalidInputException("Category is required.");
        }
        if (restaurant.getWorkingHours() == null || restaurant.getWorkingHours().trim().isEmpty()) {
            throw new InvalidInputException("WorkingHours is required.");
        }
        if (!GenerallController.isValidImage(restaurant.getLogoBase64())) {
            throw new InvalidInputException("Logo is not a valid image.");
        }

        //409 -> sql exception
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
        // TODO: 1. Verify the sellerId is valid.
        // TODO: 2. Fetch the user to get their phone number.
        // TODO: 3. Use restaurantDAO.getRestaurantsBySellerPhoneNumber() to get the list.
        // TODO: 4. Return the list of restaurants.
        return null; // Placeholder
    }

    /**
     * Handles adding a new food item to a restaurant's menu.
     * @param restaurantId The ID of the restaurant to add the food to.
     * @param food The food item from the request.
     * @param supply The supply count for this food item in this specific restaurant.
     * @param sellerId The ID of the authenticated seller.
     * @return A map containing the newly created food item.
     * @throws Exception for validation, authorization, or database errors.
     */
    public Map<String, Object> handleAddFoodItem(Integer restaurantId, Food food, int supply, Integer sellerId) throws Exception {
        // TODO: 1. Validate the incoming food data (name, price, etc.).
        // TODO: 2. Verify the seller owns the restaurant with restaurantId.
        // TODO: 3. Use restaurantDAO.addFoodItem() to create the food and add it to the menu.
        // TODO: 4. Return the created food item with its new ID.
        return null; // Placeholder
    }

    /**
     * Handles fetching the menu for a specific restaurant.
     * @param restaurantId The ID of the restaurant.
     * @return A map containing the list of food items on the menu.
     * @throws Exception if the restaurant is not found or a database error occurs.
     */
    public Map<String, Object> handleGetRestaurantMenu(Integer restaurantId) throws Exception {
        // TODO: 1. Validate restaurantId.
        // TODO: 2. Use restaurantDAO.getMenuForRestaurant() to fetch the menu.
        // TODO: 3. Return the menu list.
        return null; // Placeholder
    }
}
