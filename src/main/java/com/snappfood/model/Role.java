package com.snappfood.model;

import com.google.gson.annotations.SerializedName;

public enum Role {
    @SerializedName("seller")
    SELLER("seller"),

    @SerializedName("customer")
    CUSTOMER("customer"),

    @SerializedName("admin")
    ADMIN("admin"),

    @SerializedName("courier")
    COURIER("courier");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String role) {
        for (Role r : Role.values()) {
            if (r.getValue().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }
}
