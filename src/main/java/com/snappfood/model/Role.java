package com.snappfood.model;

public enum Role {
    SELLER("seller"),
    BUYER("buyer"),
    ADMIN("admin"),
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
