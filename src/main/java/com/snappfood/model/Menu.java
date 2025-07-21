package com.snappfood.model;

import java.util.List;

/**
 * Represents a titled menu for a restaurant, containing a curated list of food items.
 */
public class Menu {

    private int id;
    private int restaurantId;
    private String title;
    private List<Food> items;

    public Menu() {
    }

    public Menu(int restaurantId, String title) {
        this.restaurantId = restaurantId;
        this.title = title;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Food> getItems() {
        return items;
    }

    public void setItems(List<Food> items) {
        this.items = items;
    }
}