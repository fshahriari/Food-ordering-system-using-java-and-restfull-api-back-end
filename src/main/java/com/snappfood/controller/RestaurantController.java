package com.snappfood.controller;

import com.snappfood.dao.OrderDAO;
import com.snappfood.dao.RestaurantDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.*;
import com.snappfood.model.*;

import java.sql.SQLException;
import java.util.ArrayList;
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
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderController orderController = new OrderController();


    private static final int MAX_RESTAURANT_CREATION_REQUESTS = 3;
    private static final long CREATION_RATE_LIMIT_WINDOW_MS = 3600000; // 1 hour
    private final Map<Integer, RequestTracker> restaurantCreationTrackers = new ConcurrentHashMap<>();

    private static final int MAX_FETCH_RESTAURANTS_REQUESTS = 20;
    private static final long FETCH_RATE_LIMIT_WINDOW_MS = 60000; // 1 minute
    private final Map<Integer, RequestTracker> fetchRestaurantsTrackers = new ConcurrentHashMap<>();

    private static final int MAX_ADD_FOOD_REQUESTS = 30;
    private static final long ADD_FOOD_RATE_LIMIT_WINDOW_MS = 3600000; // 1 hour
    private final Map<Integer, RequestTracker> addFoodItemTrackers = new ConcurrentHashMap<>();

    private static final int MAX_UPDATE_RESTAURANT_REQUESTS = 10;
    private static final long UPDATE_RESTAURANT_RATE_LIMIT_WINDOW_MS = 60000; // 1 minute
    private final Map<Integer, RequestTracker> updateRestaurantTrackers = new ConcurrentHashMap<>();

    private static final int MAX_UPDATE_FOOD_REQUESTS = 20;
    private static final long UPDATE_FOOD_RATE_LIMIT_WINDOW_MS = 60000; // 1 minute
    private final Map<Integer, RequestTracker> updateFoodItemTrackers = new ConcurrentHashMap<>();

    private static final int MAX_DELETE_FOOD_REQUESTS = 15;
    private static final long DELETE_FOOD_RATE_LIMIT_WINDOW_MS = 3600000; // 1 hour
    private final Map<Integer, RequestTracker> deleteFoodItemTrackers = new ConcurrentHashMap<>();

    private static final int MAX_CREATE_MENU_REQUESTS = 10;
    private static final long CREATE_MENU_RATE_LIMIT_WINDOW_MS = 3600000; // 1 hour
    private final Map<Integer, RequestTracker> createMenuTrackers = new ConcurrentHashMap<>();

    private static final int MAX_DELETE_MENU_REQUESTS = 10;
    private static final long DELETE_MENU_RATE_LIMIT_WINDOW_MS = 3600000; // 1 hour
    private final Map<Integer, RequestTracker> deleteMenuTrackers = new ConcurrentHashMap<>();

    private static final int MAX_ADD_ITEM_TO_MENU_REQUESTS = 50;
    private static final long ADD_ITEM_TO_MENU_RATE_LIMIT_WINDOW_MS = 3600000; // 1 hour
    private final Map<Integer, RequestTracker> addItemToMenuTrackers = new ConcurrentHashMap<>();

    private static final int MAX_REMOVE_ITEM_FROM_MENU_REQUESTS = 50;
    private static final long REMOVE_ITEM_FROM_MENU_RATE_LIMIT_WINDOW_MS = 3600000; // 1 hour
    private final Map<Integer, RequestTracker> removeItemFromMenuTrackers = new ConcurrentHashMap<>();

    private static final int MAX_GET_MENU_REQUESTS = 30;
    private static final long GET_MENU_RATE_LIMIT_WINDOW_MS = 60000; // 1 minute
    private final Map<String, RequestTracker> getMenuTrackers = new ConcurrentHashMap<>();


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

        int pendingRestaurantId = restaurantDAO.createPendingRestaurant(restaurant, seller.getPhone());
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

        // Combine both lists into one
        List<Restaurant> allRestaurants = new ArrayList<>();
        allRestaurants.addAll(approvedRestaurants);
        allRestaurants.addAll(pendingRestaurants);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("restaurants", allRestaurants);
        return response;
    }

    /**
     * Handles adding an item to the restaurant's master food list.
     * Corresponds to POST /restaurants/{id}/item
     */
    public Map<String, Object> handleAddFoodItemToMasterList(int restaurantId, int sellerId, Food food) throws Exception {
        User seller = authorizeSellerAction(sellerId, restaurantId);

        if (food == null || food.getName() == null || food.getName().trim().isEmpty()) {
            throw new InvalidInputException("Food name is required.");
        }
        if (!GenerallController.isValidImage(food.getImageBase64())) {
            throw new InvalidInputException("Food image is required.");
        }
        if (food.getDescription() != null && food.getDescription().length() > 200) {
            throw new InvalidInputException("Description cannot exceed 200 characters.");
        }
        if (food.getKeywords() == null || food.getKeywords().isEmpty()) {
            throw new InvalidInputException("At least one keyword is required for the food item.");
        }

        if (food.getPrice() < 0 || food.getSupply() < 0) {
            throw new InvalidInputException("Price and supply cannot be negative.");
        }

        food.setRestaurantId(restaurantId);
        food.setCategory(FoodCategory.valueOf(restaurantDAO.getRestaurantById(restaurantId).getCategory()));
        int foodId = restaurantDAO.addFoodItem(food);
        food.setId(foodId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Food item added to the master list successfully.");
        response.put("food_item", food);
        return response;
    }

    /**
     * Handles adding an existing food item from the master list to a titled menu.
     * Corresponds to PUT /restaurants/{id}/menu/{title}
     */
    public Map<String, Object> handleAddItemToTitledMenu(int restaurantId, int sellerId, String menuTitle, Integer foodItemId) throws Exception {
        User seller = authorizeSellerAction(sellerId, restaurantId);

        if (foodItemId == null) {
            throw new InvalidInputException("item_id is required in the request body.");
        }

        Food food = restaurantDAO.getFoodItemById(foodItemId);
        if (food == null || food.getRestaurantId() != restaurantId) {
            throw new ResourceNotFoundException("Food item with ID " + foodItemId + " not found in this restaurant's master list.");
        }

        RequestTracker tracker = addItemToMenuTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_ADD_ITEM_TO_MENU_REQUESTS, ADD_ITEM_TO_MENU_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You are adding items to menus too frequently. Please try again later.");
        }

        Menu menu = restaurantDAO.getMenuByTitle(restaurantId, menuTitle);
        if (menu == null) {
            throw new ResourceNotFoundException("Menu with title '" + menuTitle + "' not found.");
        }
        restaurantDAO.addFoodItemToMenu((int)menu.getId(), (int)foodItemId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Food item added to menu '" + menuTitle + "' successfully.");
        return response;
    }

    /**
     * Handles updating a food item in the master list.
     * Corresponds to PUT /restaurants/{id}/item/{item_id}
     */
    public Map<String, Object> handleUpdateMasterFoodItem(int restaurantId, int itemId, int sellerId, Food updatedFood) throws Exception {
        User seller = authorizeSellerAction(sellerId, restaurantId);

        Food existingFood = restaurantDAO.getFoodItemById(itemId);
        if (existingFood == null || existingFood.getRestaurantId() != restaurantId) {
            throw new ResourceNotFoundException("Food item with ID " + itemId + " not found in this restaurant.");
        }

        if (restaurantDAO.isFoodItemInActiveOrder((int) updatedFood.getId())) {
            throw new ConflictException("Cannot update the food item because it's in an active order.");
        }

        boolean isUpdated = false;
        if (updatedFood.getName() != null && !updatedFood.getName().trim().isEmpty()) {
            if (updatedFood.getName().equals(existingFood.getName())) {
                throw new InvalidInputException("New name must be different from the current name.");
            }
            existingFood.setName(updatedFood.getName());
            isUpdated = true;
        }

        if (updatedFood.getImageBase64() != null && !updatedFood.getImageBase64().trim().isEmpty()) {
            if (updatedFood.getImageBase64().equals(updatedFood.getImageBase64())) {
                throw new InvalidInputException("New image must be different from the current image.");
            }
            if (!GenerallController.isValidImage(updatedFood.getImageBase64())) {
                throw new InvalidInputException("Invalid image format. Please provide a valid Base64 encoded image.");
            }
            existingFood.setImageBase64(updatedFood.getImageBase64());
            isUpdated = true;
        }

        if (updatedFood.getDescription() != null && !updatedFood.getDescription().trim().isEmpty()) {
            if (updatedFood.getDescription().equals(existingFood.getDescription())) {
                throw new InvalidInputException("New description must be different from the current description.");
            }
            if (updatedFood.getDescription().length() > 200) {
                throw new InvalidInputException("Description cannot exceed 200 characters.");
            }
            existingFood.setDescription(updatedFood.getDescription());
            isUpdated = true;
        }

        if (updatedFood.getPrice() != 0) {
            if (updatedFood.getPrice() < 0) {
                throw new InvalidInputException("Price cannot be negative.");
            }
            if (updatedFood.getPrice() == existingFood.getPrice()) {
                throw new InvalidInputException("New price must be different from the current price.");
            }
            existingFood.setPrice(updatedFood.getPrice());
            isUpdated = true;
        }

        if (updatedFood.getSupply() != 0) {
            if (updatedFood.getSupply() < 0) {
                throw new InvalidInputException("Supply cannot be negative.");
            }
            if (updatedFood.getSupply() == existingFood.getSupply()) {
                throw new InvalidInputException("New supply must be different from the current supply.");
            }
            existingFood.setSupply(updatedFood.getSupply());
            isUpdated = true;
        }

        if (updatedFood.getKeywords() != null && !updatedFood.getKeywords().isEmpty()) {
            if (updatedFood.getKeywords().equals(existingFood.getKeywords())) {
                throw new InvalidInputException("New keywords must be different from the current keywords.");
            }
            existingFood.setKeywords(updatedFood.getKeywords());
            isUpdated = true;
        }

        if (!isUpdated) {
            throw new InvalidInputException("No update data provided.");
        }

        restaurantDAO.updateFoodItem(existingFood);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Food item updated successfully.");
        response.put("food_item", existingFood);
        return response;
    }

    /**
     * Handles deleting a food item from the master list.
     * Corresponds to DELETE /restaurants/{id}/item/{item_id}
     */
    public Map<String, Object> handleDeleteMasterFoodItem(int restaurantId, int itemId, int sellerId) throws Exception {
        User seller = authorizeSellerAction(sellerId, restaurantId);

        Food food = restaurantDAO.getFoodItemById(itemId);
        if (food == null || food.getRestaurantId() != restaurantId) {
            throw new ResourceNotFoundException("Food item with ID " + itemId + " not found in this restaurant.");
        }

        if (restaurantDAO.isFoodItemInAnyMenu(itemId)) {
            throw new ConflictException("Cannot delete food item. It is currently part of one or more menus. Please remove it from all menus first.");
        }

        if (restaurantDAO.isAnyFoodItemInActiveOrder(restaurantId)) {
            throw new ConflictException("Cannot delete food item. It is currently part of an active order. Please complete or cancel the order first.");
        }

        RequestTracker tracker = deleteFoodItemTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_DELETE_FOOD_REQUESTS, DELETE_FOOD_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You are deleting food items too frequently. Please try again later.");
        }

        restaurantDAO.deleteFoodItem(itemId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Food item deleted from master list successfully.");
        return response;
    }


    /**
     * Handles the creation of a new titled menu for a restaurant.
     * Corresponds to POST /restaurants/{id}/menu
     */
    public Map<String, Object> handleCreateMenu(int restaurantId, int sellerId, String title) throws Exception {
        User seller = authorizeSellerAction(sellerId, restaurantId);

        if (title == null || title.trim().isEmpty()) {
            throw new InvalidInputException("Menu title is required.");
        }

        if (restaurantDAO.getRestaurantById(restaurantId) == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }

        RequestTracker tracker = createMenuTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_CREATE_MENU_REQUESTS, CREATE_MENU_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You are creating menus too frequently. Please try again later.");
        }

        if (restaurantDAO.menuTitleExists(restaurantId, title)) {
            throw new ConflictException("A menu with this title already exists for this restaurant.");
        }

        Menu newMenu = new Menu(restaurantId, title);
        int menuId = restaurantDAO.createMenu(newMenu);
        newMenu.setId(menuId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Menu created successfully.");
        response.put("menu", newMenu);
        return response;
    }

    /**
     * Handles deleting a titled menu.
     * Corresponds to DELETE /restaurants/{id}/menu/{title}
     */
    public Map<String, Object> handleDeleteTitledMenu(int restaurantId, int sellerId, String menuTitle) throws Exception {
        User seller = authorizeSellerAction(sellerId, restaurantId);

        if (menuTitle == null || menuTitle.trim().isEmpty()) {
            throw new InvalidInputException("Menu title is required.");
        }
        if (restaurantDAO.getRestaurantById(restaurantId) == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }

        RequestTracker tracker = deleteMenuTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_DELETE_MENU_REQUESTS, DELETE_MENU_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You are deleting menus too frequently. Please try again later.");
        }

        if (!restaurantDAO.menuTitleExists(restaurantId, menuTitle)) {
            throw new ResourceNotFoundException("Menu with title '" + menuTitle + "' does not exist for this restaurant.");
        }

        if (restaurantDAO.isMenuEmpty(restaurantId, menuTitle)) {
            throw new ConflictException("Cannot delete menu. It is currently empty. Please add items to it first.");
        }

        Menu menu = restaurantDAO.getMenuByTitle(restaurantId, menuTitle);
        if (menu == null) {
            throw new ResourceNotFoundException("Menu with title '" + menuTitle + "' not found.");
        }

        restaurantDAO.deleteMenuById(menu.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Menu '" + menuTitle + "' deleted successfully.");
        return response;
    }

    /**
     * Handles removing a food item from a specific titled menu.
     * Corresponds to DELETE /restaurants/{id}/menu/{title}/{item_id}
     */
    public Map<String, Object> handleRemoveItemFromTitledMenu(int restaurantId, int sellerId, String menuTitle, int itemId) throws Exception {
        User seller = authorizeSellerAction(sellerId, restaurantId);

        Menu menu = restaurantDAO.getMenuByTitle(restaurantId, menuTitle);
        if (menu == null) {
            throw new ResourceNotFoundException("Menu with title '" + menuTitle + "' not found.");
        }

        if (!restaurantDAO.isItemInMenu(menu.getId(), itemId)) {
            throw new ResourceNotFoundException("Food item with ID " + itemId + " not found in this menu.");
        }

        if (restaurantDAO.isMenuItemInActiveOrder(restaurantId)) {
            throw new ConflictException("Can not remove the food item 'cause it's in an active order.");
        }

        RequestTracker tracker = removeItemFromMenuTrackers.computeIfAbsent(sellerId, k -> new RequestTracker(MAX_REMOVE_ITEM_FROM_MENU_REQUESTS, REMOVE_ITEM_FROM_MENU_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("You are removing items from menus too frequently. Please try again later.");
        }

        restaurantDAO.removeItemFromMenu(menu.getId(), itemId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Food item removed from menu '" + menuTitle + "' successfully.");
        return response;
    }



    /**
     * A helper method to centralize seller authorization and ownership checks.
     * @return The authorized User object if successful.
     * @throws Exception if authorization fails.
     */
    private User authorizeSellerAction(int sellerId, int restaurantId) throws Exception {
        if (sellerId <= 0) {
            throw new UnauthorizedException("You must be logged in to perform this action.");
        }

        User seller = userDAO.findUserById(sellerId);
        if (seller == null || seller.getRole() != Role.SELLER) {
            throw new ForbiddenException("Only sellers can perform this action.");
        }

        if (restaurantDAO.isRestaurantPending(restaurantId)) {
            throw new ForbiddenException("Action cannot be performed on a restaurant that is pending approval.");
        }

        Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (restaurant == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }

        if (!restaurant.getSellerPhoneNumber().equals(seller.getPhone())) {
            throw new ForbiddenException("You do not have permission to modify this restaurant.");
        }

        return seller;
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
        authorizeSellerAction(sellerId, restaurantId);

        Restaurant existingRestaurant = restaurantDAO.getRestaurantById(restaurantId); // Already fetched in authorize

        boolean isUpdated = false;
        if (updateData.getName() != null && !updateData.getName().trim().isEmpty()) {
            existingRestaurant.setName(updateData.getName());
            isUpdated = true;
        }

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
        existingRestaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (existingRestaurant == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }

        //403
        if (!existingRestaurant.getSellerPhoneNumber().equals(seller.getPhone())) {
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

        if (!isUpdated) {
            throw new InvalidInputException("No update data provided.");
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

    public Map<String, Object> handleGetRestaurantMenu(Integer restaurantId, String clientIp) throws Exception {
        if (restaurantId == null || restaurantId <= 0) {
            throw new InvalidInputException("Invalid restaurant ID.");
        }

        if (!userDAO.isUserCustomer(clientIp)) {
            throw new ForbiddenException("Only customers can view restaurant menus.");
        }

        RequestTracker tracker = getMenuTrackers.computeIfAbsent(clientIp,
                k -> new RequestTracker(MAX_GET_MENU_REQUESTS, GET_MENU_RATE_LIMIT_WINDOW_MS));
        if (!tracker.allowRequest()) {
            throw new TooManyRequestsException("Too many menu requests. Please try again later.");
        }

        Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (restaurant == null) {
            throw new ResourceNotFoundException("Restaurant with ID " + restaurantId + " not found.");
        }

        if (restaurantDAO.isRestaurantPending(restaurantId)) {
            throw new ForbiddenException("Cannot view menus of a restaurant that is pending approval.");
        }

        List<Menu> menus = restaurantDAO.getMenusByRestaurantId(restaurantId);
        Map<String, List<Food>> menuItems = new HashMap<>();

        for (Menu menu : menus) {
            List<Food> foodItems = restaurantDAO.getFoodItemsByMenuId(menu.getId());
            menuItems.put(menu.getTitle(), foodItems);
        }

        List<Menu> menues = restaurantDAO.getMenusByRestaurantId(restaurantId);
        List<String> menuTitles = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("vendor", restaurant);

        for (Menu menu : menues) {
            menuTitles.add(menu.getTitle());
            List<Food> foodItems = restaurantDAO.getFoodItemsByMenuId(menu.getId());
            response.put(menu.getTitle(), foodItems);
        }
        response.put("menu_titles", menuTitles);


        return response;
    }

    public Map<String, Object> handleGetRestaurantOrders(Integer sellerId, int restaurantId, Map<String, String> filters) throws Exception {
        User seller = authorizeSellerAction(sellerId, restaurantId);

        List<Order> orders = orderDAO.getOrdersForRestaurant(restaurantId, filters);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("orders", orders);
        return response;
    }

    /**
     * Handles updating the status of an order by a seller.
     * @param sellerId The ID of the authenticated seller.
     * @param restaurantId The ID of the restaurant the order belongs to.
     * @param orderId The ID of the order to update.
     * @param body The request body containing the new status.
     * @return A map with a success message.
     * @throws Exception for any validation, authorization, or database errors.
     */
    public Map<String, Object> handleUpdateOrderStatus(Integer sellerId, int restaurantId, int orderId, Map<String, String> body) throws Exception {
        authorizeSellerAction(sellerId, restaurantId);

        String newStatusStr = body.get("status");
        if (newStatusStr == null || newStatusStr.trim().isEmpty()) {
            throw new InvalidInputException("Status is required.");
        }

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid status value: " + newStatusStr);
        }

        orderController.handleUpdateOrderStatus(sellerId, orderId, newStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "Order status updated successfully.");
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