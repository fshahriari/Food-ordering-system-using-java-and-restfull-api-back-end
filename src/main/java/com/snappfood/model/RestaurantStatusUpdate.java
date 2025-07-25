package com.snappfood.model;

/**
 * A simple model class to represent the JSON object for updating a restaurant's status.
 * Corresponds to the 'restaurant_status_update' schema in the OpenAPI specification.
 */
public class RestaurantStatusUpdate {
    private int restaurant_id;
    private String status;

    public int getRestaurantId() {
        return restaurant_id;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurant_id = restaurantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}