package com.snappfood.model;

import com.google.gson.annotations.SerializedName;

/**
 * Defines the available categories for food items and restaurants.
 * Using an enum ensures type safety and consistency.
 */
public enum FoodCategory {

    @SerializedName("Fast Food")
    FAST_FOOD("Fast Food"),
    @SerializedName("Sea Food")
    SEA_FOOD("Sea Food"),
    @SerializedName("Persian Food")
    PERSIAN_FOOD("Persian Food"),
    @SerializedName("Vegetarian Food")
    VEGETARIAN_FOOD("Vegetarian Food"),

    @SerializedName("UNDEFINED")
    UNDEFINED("Undefined");

    private final String displayName;

    /**
     * Constructor for the enum.
     * @param displayName The user-friendly name for the category.
     */
    FoodCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the user-friendly display name.
     * @return The display name string.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Finds a FoodCategory enum constant from a user-friendly string.
     * This method is case-insensitive and is the safe way to parse category strings
     * from client requests.
     * * @param text The string to parse, e.g., "Fast Food".
     * @return The matching FoodCategory constant.
     * @throws IllegalArgumentException if no matching category is found.
     */
    public static FoodCategory fromString(String text) {
        if (text != null) {
            for (FoodCategory category : FoodCategory.values()) {
                if (text.equalsIgnoreCase(category.displayName)) {
                    return category;
                }
            }
        }
        throw new IllegalArgumentException("Cannot find a category for string: \"" + text + "\"");
    }
}