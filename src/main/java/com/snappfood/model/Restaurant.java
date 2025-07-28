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

    @SerializedName("tax_fee")
    private int taxFee;

    @SerializedName("additional_fee")
    private int additionalFee;

    private List<Food> masterFoodList;

    private List<Menu> menus;

    private String sellerPhoneNumber;

    /**
     * Default constructor.
     */
    public Restaurant(int taxFee, int additionalFee) {
        this.taxFee = taxFee;
        this.additionalFee = additionalFee;
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
     * @param sellerPhoneNumber A phone number for the seller who own the restaurant.
     * @param additionalFee the additional fee for each order of the restaurant
     * @param taxFee the constant fee for each order of the restaurant
     */
    public Restaurant(int id, String name, String logoBase64, String address, String phoneNumber, String workingHours, String category, int taxFee, int additionalFee, String sellerPhoneNumber) {
        this.id = id;
        this.name = name;
        this.logoBase64 = logoBase64;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.workingHours = workingHours;
        this.category = category;
        this.taxFee = taxFee;
        this.additionalFee = additionalFee;
        this.sellerPhoneNumber = sellerPhoneNumber;
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

    public String getSellerPhoneNumber() {
        return sellerPhoneNumber;
    }

    public void setSellerPhoneNumber(String sellerPhoneNumber) {
        this.sellerPhoneNumber = sellerPhoneNumber;
    }

    public int getTaxFee() {
        return taxFee;
    }
    public void setTaxFee(int taxFee) {
        this.taxFee = taxFee;
    }

    public int getAdditionalFee() {
        return additionalFee;
    }

    public void setAdditionalFee(int additionalFee) {
        this.additionalFee = additionalFee;
    }

    public List<Food> getMasterFoodList() {
        return masterFoodList;
    }

    public void setMasterFoodList(List<Food> masterFoodList) {
        this.masterFoodList = masterFoodList;
    }

    public List<Menu> getMenus() {
        return menus;
    }

    public void setMenus(List<Menu> menus) {
        this.menus = menus;
    }
}