package com.snappfood.controller;

import com.snappfood.dao.RestaurantDAO;
import com.snappfood.dao.UserDAO;
import com.snappfood.exception.ForbiddenException;
import com.snappfood.exception.ResourceNotFoundException;
import com.snappfood.exception.UnauthorizedException;
import com.snappfood.model.Food;
import com.snappfood.model.Menu;
import com.snappfood.model.Restaurant;
import com.snappfood.model.User;

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

    /**
     * Handles fetching the detailed view of a single restaurant, including its menus and food items.
     * @param userId The ID of the authenticated user.
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
}