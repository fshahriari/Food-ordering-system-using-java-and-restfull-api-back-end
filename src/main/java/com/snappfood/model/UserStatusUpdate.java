package com.snappfood.model;

/**
 * A simple model class to represent the JSON object for updating a user's status.
 * Corresponds to the 'user_status_update' schema in the OpenAPI specification.
 */
public class UserStatusUpdate {
    private int user_id;
    private String status;

    public int getUserId() {
        return user_id;
    }

    public void setUserId(int userId) {
        this.user_id = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}