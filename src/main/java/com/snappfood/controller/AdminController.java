package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.exception.ForbiddenException;
import com.snappfood.exception.UnauthorizedException;
import com.snappfood.model.Role;
import com.snappfood.model.User;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminController {

    private final UserDAO userDAO = new UserDAO();

    public List<User> getPendingSellersAndCouriers() {
        try {
            return userDAO.getPendingUsers();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Handles the logic for fetching the list of pending users for an admin.
     * @param adminId The ID of the user making the request.
     * @return A map containing the list of pending users.
     * @throws Exception for authorization or database errors.
     */
    public Map<String, Object> handleGetPendingUsers(Integer adminId) throws Exception {
        if (adminId == null) {
            throw new UnauthorizedException("You must be logged in as an admin to view pending users.");
        }
        User admin = userDAO.findUserById(adminId);
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to access this resource.");
        }

        List<User> pendingUsers = userDAO.getPendingUsersSortedByName();

        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("pending_users", pendingUsers);
        return response;
    }


    public void confirmUser(int userId) throws SQLException {
        userDAO.confirmUser(userId);
    }

    public void rejectUser(int userId) throws SQLException {
        userDAO.rejectUser(userId);
    }
}