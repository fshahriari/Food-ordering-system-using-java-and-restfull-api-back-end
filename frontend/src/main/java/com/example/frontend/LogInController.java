package com.example.frontend;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
                                     // کارای سوکت و اتصال مونده

public class LogInController {
    @FXML
    private TextField phoneTextField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label logInMassageLabel;
    @FXML
    private ImageView backGroundLogIn;

    public void initialize(URL location, ResourceBundle resources) {
        Image backGround = new Image(getClass().getResource("/pictures/loginback.png").toExternalForm());

        backGroundLogIn.setImage(backGround);
    }

    @FXML
    private void cancelClicked(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/frontend/hello-view.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }
    @FXML
    private void logInClicked(ActionEvent event) throws IOException {
        if(phoneTextField.getText().isBlank() || passwordField.getText().isBlank()) {
            logInMassageLabel.setText("Please fill all the blank");
        }
        else {
            if (!isValidPassword(passwordField.getText())) {
                logInMassageLabel.setText("Please enter a valid password");
            } else if (!isValidPhone(phoneTextField.getText())) {
                logInMassageLabel.setText("Please enter a valid phone number");
            } else {
                Parent root = FXMLLoader.load(getClass().getResource("/com/example/frontend/dashboard.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));

            }
        }
    }

    private static boolean isValidPassword(String password) {
        return password != null && password.length()>= 8 && password.matches(".*[a-zA-Z].*") && password.matches(".*[0-9].*");
    }
    private static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("09\\d{9}");
    }
}
