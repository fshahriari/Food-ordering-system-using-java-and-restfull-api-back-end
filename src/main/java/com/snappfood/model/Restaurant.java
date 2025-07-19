package com.snappfood.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Represents a Restaurant in the food ordering system.
 * This class includes details specified in the project requirements and the 'restaurant' schema
 * from the aut_food.yaml API specification.
 */
public class Restaurant {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("logoBase64")
    private String logoBase64;

    @SerializedName("address")
    private String address;

    @SerializedName("phone")
    private String phoneNumber;

    private String workingHours;

    private String category;

    //TODO This is for in-memory representation; how it's stored in the DB might differ.
    private Map<Food, Integer> menu;

    private List<String> sellerPhoneNumbers;

    /**
     * Default constructor.
     */
    public Restaurant() {
    }

    /**
     * Constructs a new Restaurant with all its properties.
     *
     * @param id The unique identifier for the restaurant.
     * @param name The name of the restaurant.
     * @param logoBase64 The Base64 encoded logo string.
     * @param address The physical address of the restaurant.
     * @param phoneNumber The contact phone number.
     * @param workingHours The operating hours of the restaurant.
     * @param category The category of food the restaurant serves.
     * @param menu A map representing the menu, with Food items and their counts.
     * @param sellerPhoneNumbers A list of phone numbers for the sellers who own the restaurant.
     */
    public Restaurant(int id, String name, String logoBase64, String address, String phoneNumber, String workingHours, String category, Map<Food, Integer> menu, List<String> sellerPhoneNumbers) {
        this.id = id;
        this.name = name;
        this.logoBase64 = logoBase64;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.workingHours = workingHours;
        this.category = category;
        this.menu = menu;
        this.sellerPhoneNumbers = sellerPhoneNumbers;
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

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<Food, Integer> getMenu() {
        return menu;
    }

    public void setMenu(Map<Food, Integer> menu) {
        this.menu = menu;
    }

    public List<String> getSellerPhoneNumbers() {
        return sellerPhoneNumbers;
    }

    public void setSellerPhoneNumbers(List<String> sellerPhoneNumbers) {
        this.sellerPhoneNumbers = sellerPhoneNumbers;
    }
}