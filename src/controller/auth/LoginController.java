package controller.auth;

import java.io.IOException;

import controller.common.SidebarController;
import dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import model.User;
import utils.SessionManager;

public class LoginController {
    @FXML private TextField usernameTextField; // Updated label to reflect username
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label invalidLoginLabel;
    @FXML private ImageView backButton;

    Stage window;
    Scene scene;
    Parent root;
    boolean isFullscreen;

    UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLoginClicked() throws IOException {
        if (usernameTextField.getText().isBlank() || passwordField.getText().isBlank()) {
            invalidLoginLabel.setText("Please enter username/password.");
        } else {
            User user = validateLogin();
            if (user != null) {
                SessionManager.setCurrentUser(user);
                window = (Stage) loginButton.getScene().getWindow();
                isFullscreen = true;
                if (user.getRole().equals("manager")) {
                    loadSidebarWithView("/view/common/dashboard"); // Load sidebar with dashboard for manager
                } else {
                    loadInterface("/view/staff/order.fxml"); // Load order view directly for staff
                }
            }
        }
    }

    @FXML
    private void handleGoBackClicked() throws IOException {
        window = (Stage) backButton.getScene().getWindow();
        isFullscreen = false;
        loadInterface("/view/default.fxml"); // Assuming default.fxml exists if needed
    }

    private void loadInterface(String fxmlPath) {
        try {
            root = FXMLLoader.load(getClass().getResource(fxmlPath));
            scene = new Scene(root);
            window.setScene(scene);
            window.setResizable(false);
            window.centerOnScreen();
            window.show();
            if (isFullscreen) {
                window.setFullScreenExitHint("");
                window.setFullScreen(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSidebarWithView(String initialViewPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/common/sidebar.fxml"));
            root = loader.load();
            SidebarController controller = loader.getController();
            controller.loadScene(initialViewPath); // Set the initial view
            scene = new Scene(root);
            window.setScene(scene);
            window.setResizable(false);
            window.centerOnScreen();
            window.show();
            if (isFullscreen) {
                window.setFullScreenExitHint("");
                window.setFullScreen(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private User validateLogin() {
        User user = userDAO.login(usernameTextField.getText().trim(), passwordField.getText());
        if (user != null) {
            return user;
        } else {
            invalidLoginLabel.setText("Invalid Login. Please try again.");
            usernameTextField.setText("");
            passwordField.setText("");
            return null;
        }
    }
}