package com.snappfood.model;

import com.google.gson.annotations.SerializedName;
import java.util.List; // import this

/**
 * Represents a food item available in a restaurant.
 * A food item has one category, and its supply is managed by the restaurant's menu.
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

    @SerializedName("category")
    private FoodCategory category;

    @SerializedName("supply")
    private int supply;

    @SerializedName("keywords")
    private List<String> keywords;

    /**
     * Default constructor.
     */
    public Food() {
    }

    /**
     * Constructs a new Food item with its core properties.
     *
     * @param id The unique identifier for the food item.
     * @param name The name of the food item.
     * @param imageBase64 The Base64 encoded image string.
     * @param description A brief description of the food item.
     * @param restaurantId The ID of the restaurant this food item belongs to (for context).
     * @param price The price of the food item.
     * @param category The category of the food item (e.g., "Fast Food").
     * @param supply The available stock of this food item.
     * @param keywords The keywords for the food item.
     */
    public Food(int id, String name, String imageBase64, String description, int restaurantId, int price, FoodCategory category, int supply, List<String> keywords) { // Add keywords here
        this.id = id;
        this.name = name;
        this.imageBase64 = imageBase64;
        this.description = description;
        this.restaurantId = restaurantId;
        this.price = price;
        this.category = category;
        this.supply = supply;
        this.keywords = keywords;
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getRestaurantId() { return restaurantId; }
    public void setRestaurantId(int restaurantId) { this.restaurantId = restaurantId; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public FoodCategory getCategory() { return category; }
    public void setCategory(FoodCategory category) { this.category = category; }
    public int getSupply() { return supply; }
    public void setSupply(int supply) { this.supply = supply; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
}