package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.model.User;

import java.sql.SQLException;
import java.util.List;

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

    public void confirmUser(int userId) throws SQLException {
        userDAO.confirmUser(userId);
    }

    public void rejectUser(int userId) throws SQLException {
        userDAO.rejectUser(userId);
    }
}