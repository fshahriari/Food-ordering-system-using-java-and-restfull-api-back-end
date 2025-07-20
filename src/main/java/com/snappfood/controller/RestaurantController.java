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

    private static final int MAX_ADD_FOOD_REQUESTS = 30;
    private static final long ADD_FOOD_RATE_LIMIT_WINDOW_MS = 3600000;
    private final Map<Integer, RequestTracker> addFoodItemTrackers = new ConcurrentHashMap<>();

    private static final int MAX_UPDATE_RESTAURANT_REQUESTS = 10;
    private static final long UPDATE_RESTAURANT_RATE_LIMIT_WINDOW_MS = 60000;
    private final Map<Integer, RequestTracker> updateRestaurantTrackers = new ConcurrentHashMap<>();

    private static final int MAX_UPDATE_FOOD_REQUESTS = 20;
    private static final long UPDATE_FOOD_RATE_LIMIT_WINDOW_MS = 60000; // 1 minute
    private final Map<Integer, RequestTracker> updateFoodItemTrackers = new ConcurrentHashMap<>();


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

    public Map<String, Object> handleGetMyRestaurants(Integer sellerId) throws Exception {
        if (sellerId == null) {
            throw new UnauthorizedException("You must be logged in to view your restaurants.");
        }
        RequestTracker tracker = fetchRestaurantsTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_FETCH_RESTAURANTS_REQUESTS, FETCH_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("Too many requests. Please try again later.");
        }
        User seller = userDAO.findUserById(sellerId);
        if (seller == null) {
            throw new ResourceNotFoundException("The specified seller account does not exist.");
        }
        if (seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only users with the 'seller' role can view their restaurants.");
        }
        if (userDAO.isUserPending(seller.getPhone())) {
            throw new ForbiddenException("Your seller account is pending approval.");
        }

        List<Restaurant> approvedRestaurants = restaurantDAO.getRestaurantsBySellerPhoneNumber(seller.getPhone());
        List<Restaurant> pendingRestaurants = restaurantDAO.getPendingRestaurantsBySellerPhoneNumber(seller.getPhone());


        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("approved_restaurants", approvedRestaurants);
        response.put("pending_restaurants", pendingRestaurants);

        return response;
    }

    /**
     * Handles adding a new food item to a restaurant's menu.
     * @param restaurantId The ID of the restaurant to add the food to.
     * @param food The food item from the request.
     * @param sellerId The ID of the authenticated seller.
     * @return A map containing the newly created food item.
     * @throws Exception for validation, authorization, or database errors.
     */
    public Map<String, Object> handleAddFoodItem(Integer restaurantId, Food food, Integer sellerId) throws Exception {
        //401
        if (sellerId == null) {
            throw new UnauthorizedException("You must be logged in to add a food item.");
        }

        //429
        RequestTracker tracker = addFoodItemTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_ADD_FOOD_REQUESTS, ADD_FOOD_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You are adding food items too quickly. Please try again later.");
        }

        //403
        User seller = userDAO.findUserById(sellerId);
        if (seller == null || seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only sellers can add food items.");
        }

        if (restaurantDAO.isRestaurantPending(restaurantId)) {
            throw new ForbiddenException("Cannot add food items to a restaurant that is pending approval.");
        }

        // Ownership Check
        List<String> sellerPhoneNumbers = restaurantDAO.getSellersForRestaurant(restaurantId);
        if (!sellerPhoneNumbers.contains(seller.getPhone())) {
            throw new ForbiddenException("You do not have permission to modify this restaurant's menu.");
        }

        //400 n 404
        if (restaurantDAO.getRestaurantById(restaurantId) == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }
        if (food.getName() == null || food.getName().trim().isEmpty()) {
            throw new InvalidInputException("Food name is required.");
        }
        if (food.getPrice() < 0) {
            throw new InvalidInputException("Price cannot be negative.");
        }
        if (food.getSupply() < 0) {
            throw new InvalidInputException("Supply cannot be negative.");
        }
        if (food.getCategory() == null) {
            throw new InvalidInputException("A valid food category is required.");
        }

        //409
        List<Food> currentMenu = restaurantDAO.getMenuForRestaurant(restaurantId);
        for (Food menuItem : currentMenu) {
            if (menuItem.getName().equalsIgnoreCase(food.getName().trim())) {
                throw new ConflictException("A food item with this name already exists in this restaurant's menu.");
            }
        }

        food.setRestaurantId(restaurantId);
        int newFoodId = restaurantDAO.addFoodItem(food, food.getSupply());
        food.setId(newFoodId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Food item added successfully.");
        response.put("food_item", food);
        return response;
    }

    /**
     * Handles updating an existing restaurant's details.
     * @param restaurantId The ID of the restaurant to update.
     * @param updateData A Restaurant object containing the fields to update.
     * @param sellerId The ID of the authenticated seller.
     * @return A map containing the fully updated restaurant object.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleUpdateRestaurant(Integer restaurantId, Restaurant updateData, Integer sellerId) throws Exception {
        //401
        if (sellerId == null) {
            throw new UnauthorizedException("You must be logged in to update a restaurant.");
        }

        //429
        RequestTracker tracker = updateRestaurantTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_UPDATE_RESTAURANT_REQUESTS, UPDATE_RESTAURANT_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You are updating this restaurant too frequently. Please try again later.");
        }

        //403
        User seller = userDAO.findUserById(sellerId);
        if (seller == null || seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only sellers can update restaurants.");
        }

        if (restaurantDAO.isRestaurantPending(restaurantId)) {
            throw new ForbiddenException("Cannot update a restaurant that is pending approval.");
        }

        //404
        Restaurant existingRestaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (existingRestaurant == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }

        //403
        if (!existingRestaurant.getSellerPhoneNumbers().contains(seller.getPhone())) {
            throw new ForbiddenException("You do not have permission to update this restaurant.");
        }

        //400
        if (updateData.getName() != null) {
            if (updateData.getName().trim().isEmpty())
                throw new InvalidInputException("Restaurant name cannot be empty.");
            existingRestaurant.setName(updateData.getName());
        }
        if (updateData.getAddress() != null) {
            if (updateData.getAddress().trim().isEmpty())
                throw new InvalidInputException("Restaurant address cannot be empty.");
            existingRestaurant.setAddress(updateData.getAddress());
        }
        if (updateData.getPhoneNumber() != null) {
            if (!updateData.getPhoneNumber().matches("^[0-9]{10,15}$"))
                throw new InvalidInputException("Invalid phone number format.");
            Restaurant conflictingRestaurant = restaurantDAO.getRestaurantByPhoneNumber(updateData.getPhoneNumber());
            if (conflictingRestaurant != null && conflictingRestaurant.getId() != existingRestaurant.getId()) {
                throw new ConflictException("This phone number is already in use by another restaurant.");
            }
            existingRestaurant.setPhoneNumber(updateData.getPhoneNumber());
        }
        if (updateData.getTaxFee() > 0) {
            if (updateData.getTaxFee() < 0)
                throw new InvalidInputException("Tax fee cannot be negative.");
            existingRestaurant.setTaxFee(updateData.getTaxFee());
        }


        boolean success = restaurantDAO.updateRestaurant(existingRestaurant);
        if (!success) {
            throw new InternalServerErrorException("Failed to update restaurant details.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Restaurant updated successfully.");
        response.put("restaurant", existingRestaurant);
        return response;
    }

    public Map<String, Object> handleGetRestaurantMenu(Integer restaurantId) throws Exception {
        // TODO: Implement logic
        return null; // Placeholder
    }

    /**
     * Handles the logic for updating a food item.
     * @param restaurantId The ID of the restaurant.
     * @param itemId The ID of the food item to update.
     * @param updatedFood A Food object with the new data.
     * @param sellerId The ID of the authenticated seller.
     * @return A map with a success message and the updated food item.
     * @throws Exception for various error conditions.
     */
    public Map<String, Object> handleUpdateFoodItem(Integer restaurantId, Integer itemId, Food updatedFood, Integer sellerId) throws Exception {
        if (sellerId == null) {
            throw new UnauthorizedException("You must be logged in to update a food item.");
        }

        RequestTracker tracker = updateFoodItemTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_UPDATE_FOOD_REQUESTS, UPDATE_FOOD_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You are updating food items too frequently. Please try again later.");
        }

        User seller = userDAO.findUserById(sellerId);
        if (seller == null || seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only sellers can update food items.");
        }

        if (restaurantId <= 0 || itemId <= 0) {
            throw new InvalidInputException("Invalid restaurant or item ID.");
        }

        if (restaurantDAO.isRestaurantPending(restaurantId)) {
            throw new ForbiddenException("Cannot update food items for a restaurant that is pending approval.");
        }

        Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (restaurant == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }
        if (!restaurant.getSellerPhoneNumbers().contains(seller.getPhone())) {
            throw new ForbiddenException("You do not have permission to modify this restaurant's menu.");
        }

        Food existingFood = restaurantDAO.getFoodItemById(itemId);
        if (existingFood == null || existingFood.getRestaurantId() != restaurantId) {
            throw new ResourceNotFoundException("Food item with ID " + itemId + " not found in this restaurant.");
        }

        boolean isUpdated = false;
        if (updatedFood.getName() != null && !updatedFood.getName().trim().isEmpty() && !updatedFood.getName().equals(existingFood.getName())) {
            // Check for name conflict before updating
            if (restaurantDAO.foodItemExistsByName(restaurantId, updatedFood.getName())) {
                throw new ConflictException("A food item with this name already exists in this restaurant's menu.");
            }
            existingFood.setName(updatedFood.getName());
            isUpdated = true;
        }
        if (updatedFood.getDescription() != null && !updatedFood.getDescription().equals(existingFood.getDescription())) {
            existingFood.setDescription(updatedFood.getDescription());
            isUpdated = true;
        }
        if (updatedFood.getPrice() >= 0 && updatedFood.getPrice() != existingFood.getPrice()) {
            existingFood.setPrice(updatedFood.getPrice());
            isUpdated = true;
        }
        if (updatedFood.getSupply() >= 0 && updatedFood.getSupply() != existingFood.getSupply()) {
            existingFood.setSupply(updatedFood.getSupply());
            isUpdated = true;
        }
        if (updatedFood.getImageBase64() != null && !updatedFood.getImageBase64().equals(existingFood.getImageBase64())) {
            existingFood.setImageBase64(updatedFood.getImageBase64());
            isUpdated = true;
        }
        if (updatedFood.getCategory() != null && !updatedFood.getCategory().equals(existingFood.getCategory())) {
            existingFood.setCategory(updatedFood.getCategory());
            isUpdated = true;
        }

        if (!isUpdated) {
            throw new InvalidInputException("No changes detected. Please provide new data to update.");
        }


        boolean success = restaurantDAO.updateFoodItem(existingFood, existingFood.getSupply());
        if (!success) {
            throw new InternalServerErrorException("Failed to update food item.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Food item updated successfully.");
        response.put("food_item", existingFood);
        return response;
    }

    public Map<String, Object> handleDeleteFoodItem(Integer restaurantId, Integer itemId, Integer sellerId) throws Exception {
        //401
        if (sellerId == null) {
            throw new UnauthorizedException("You must be logged in to delete a food item.");
        }

        User seller = userDAO.findUserById(sellerId);
        if (seller == null || seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only sellers can delete food items.");
        }

        //400
        if (restaurantId <= 0 || itemId <= 0) {
            throw new InvalidInputException("Invalid restaurant or item ID.");
        }

        //403
        Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (restaurant == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }
        if (!restaurant.getSellerPhoneNumbers().contains(seller.getPhone())) {
            throw new ForbiddenException("You do not have permission to modify this restaurant's menu.");
        }

        //404
        Food existingFood = restaurantDAO.getFoodItemById(itemId);
        if (existingFood == null || existingFood.getRestaurantId() != restaurantId) {
            throw new ResourceNotFoundException("Food item with ID " + itemId + " not found in this restaurant.");
        }

        //409
        if (restaurantDAO.isFoodItemInActiveOrder(itemId)) {
            throw new ConflictException("This food item cannot be deleted as it is part of an active order.");
        }

        boolean success = restaurantDAO.deleteFoodItem(itemId);
        if (!success) {
            throw new InternalServerErrorException("Failed to delete food item.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Food item removed successfully.");
        return response;
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