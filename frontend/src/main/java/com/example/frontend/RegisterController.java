package com.example.frontend;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
                                      //قسمت سوکت و اتصال به بک اند مونده
                                      // این که چجوری از کاربر عکس پروفایل دریافت کنه رو هنوز انجام ندادم

public class RegisterController {

    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private ImageView backGroundRegister;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextArea addressArea;
    @FXML
    private Label registerMassageLabel;
    @FXML
    private TextField emailField;

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        roleComboBox.getItems().addAll("buyer", "seller", "courier");
        initializePic(location, resources);
    }

    @FXML
    private void signUpClicked(ActionEvent event) throws IOException {

        if (fullNameField.getText().isBlank() || phoneField.getText().isBlank() || passwordField.getText().isBlank() || addressArea.getText().isBlank()) {
            registerMassageLabel.setText("Please fill all the blanks");
        }
        else {
            if( !isValidEmail(emailField.getText()) ) {
                registerMassageLabel.setText("Please enter a valid email address");
            }
            else if( !isValidPassword(passwordField.getText()) ) {
                registerMassageLabel.setText("Please enter a valid password");
            }
            else if (!isValidName(fullNameField.getText())) {
                registerMassageLabel.setText("Please enter a valid name");
            }
            else if(!isValidPhoneNumber(phoneField.getText())) {
                registerMassageLabel.setText("Please enter a valid phone number");
            }
            else {
                Parent root = FXMLLoader.load(getClass().getResource("/com/example/frontend/dashboard.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));

            }

        }
    }
    @FXML
    private void cancelClicked(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/frontend/hello-view.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    private void initializePic(URL location, ResourceBundle resources) {
        Image backGround = new Image(getClass().getResource("/pictures/foodleft.png").toExternalForm());

        backGroundRegister.setImage(backGround);
    }
    private static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        return email != null && email.matches(emailRegex);
    }
    private static boolean isValidName(String name) {
        return name != null && name.matches("[a-zA-Z]+");
    }
    private static boolean isValidPassword(String password) {
        return password != null && password.length()>= 8 && password.matches(".*[a-zA-Z].*") && password.matches(".*[0-9].*");
    }
    private static boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("09\\d{9}");
    }


}
