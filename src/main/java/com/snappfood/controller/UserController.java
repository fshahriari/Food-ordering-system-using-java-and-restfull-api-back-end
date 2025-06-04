package com.snappfood.controller;

import com.snappfood.dao.UserDAO;
import com.snappfood.model.User;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;


public class UserController {

    private final UserDAO userDAO = new UserDAO();

    public int handleSignup(User user) {
        if (user.getPhoneNumber() == null || user.getPassword() == null || user.getRole() == null)
            return 400;

        try {
            boolean success = userDAO.insertUser(user);
            return success ? 201 : 500;

        } catch (SQLIntegrityConstraintViolationException e) {
            return 409;
        } catch (SQLException e) {
            return 500;
        } catch (Exception e) {
            return 500;
        }
    }
}
