package com.snappfood.model;

import com.google.gson.annotations.SerializedName;

/**
 * Defines the available categories for food items and restaurants.
 * Using an enum ensures type safety and consistency.
 */
public enum FoodCategory {

    @SerializedName("Fast Food")
    FAST_FOOD("Fast Food"),

    @SerializedName("Vegetarian Food")
    VEGETARIAN("Vegetarian Food"),

    @SerializedName("Iranian Food")
    IRANIAN("Iranian Food"),

    @SerializedName("Sea Food")
    SEA_FOOD("Sea Food"),

    @SerializedName("UnDefined")
    UNDEFINED("UnDefined");


    private final String value;

    FoodCategory(String value) {
        this.value = value;
    }

    /**
     * @return The string representation of the category (e.g., "Fast Food").
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a string to its corresponding FoodCategory enum constant.
     * This is useful when reading data from the database.
     * @param text The string to convert.
     * @return The matching FoodCategory, or null if no match is found.
     */
    public static FoodCategory fromString(String text) {
        if (text != null) {
            for (FoodCategory b : FoodCategory.values()) {
                if (text.equalsIgnoreCase(b.value)) {
                    return b;
                }
            }
        }
        return UNDEFINED;
    }
}
