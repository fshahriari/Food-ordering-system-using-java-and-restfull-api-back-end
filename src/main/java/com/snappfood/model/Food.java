package com.snappfood.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents a food item available in a restaurant.
 * This class maps directly to the 'food_item' schema in the aut_food.yaml API specification.
 */
public class Food {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("imageBase64")
    private String imageBase64;

    @SerializedName("description")
    private String description;

    @SerializedName("vendor_id")
    private int restaurantId;

    @SerializedName("price")
    private int price;

    @SerializedName("supply")
    private int supply;

    @SerializedName("categories")
    private List<String> categories;

    /**
     * Default constructor.
     */
    public Food() {
    }

    /**
     * Constructs a new Food item with all its properties.
     *
     * @param id The unique identifier for the food item.
     * @param name The name of the food item.
     * @param imageBase64 The Base64 encoded image string.
     * @param description A brief description of the food item.
     * @param restaurantId The ID of the restaurant this food item belongs to.
     * @param price The price of the food item.
     * @param supply The available quantity or supply of the food item.
     * @param categories A list of categories or keywords associated with the food.
     */
    public Food(int id, String name, String imageBase64, String description, int restaurantId, int price, int supply, List<String> categories) {
        this.id = id;
        this.name = name;
        this.imageBase64 = imageBase64;
        this.description = description;
        this.restaurantId = restaurantId;
        this.price = price;
        this.supply = supply;
        this.categories = categories;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getSupply() {
        return supply;
    }

    public void setSupply(int supply) {
        this.supply = supply;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}