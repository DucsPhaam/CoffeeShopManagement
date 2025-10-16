package controller.auth;

import dao.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class ChangePasswordController implements Initializable {

    @FXML private PasswordField currentPasswordField;
    @FXML private TextField currentPasswordTxtField;
    @FXML private ImageView showCurrentPassword;

    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordTxtField;
    @FXML private ImageView showNewPassword;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTxtField;
    @FXML private ImageView showConfirmPassword;

    @FXML private Button changePasswordBtn;
    @FXML private Button confirmPasswordBtn;
    @FXML private Label warningLabel;

    @FXML private HBox currentPasswordHBox;
    @FXML private HBox ChangeBtnHBox;
    @FXML private HBox newPasswordHBox;
    @FXML private HBox confirmHBox;
    @FXML private HBox ConfirmBtnHBox;

    private int currentUserId;
    private String currentUsername;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind TextField và PasswordField
        currentPasswordField.textProperty().bindBidirectional(currentPasswordTxtField.textProperty());
        newPasswordField.textProperty().bindBidirectional(newPasswordTxtField.textProperty());
        confirmPasswordField.textProperty().bindBidirectional(confirmPasswordTxtField.textProperty());

        // Ẩn các trường mật khẩu mới ban đầu
        newPasswordHBox.setVisible(false);
        newPasswordHBox.setManaged(false);
        confirmHBox.setVisible(false);
        confirmHBox.setManaged(false);
        ConfirmBtnHBox.setVisible(false);
        ConfirmBtnHBox.setManaged(false);

        // Lấy thông tin user hiện tại (giả sử đã login)
        // Bạn cần thay thế bằng cách lấy user từ session
        currentUserId = 1; // Replace with actual logged-in user ID
        currentUsername = "admin"; // Replace with actual logged-in username
    }

    // Current Password - Show/Hide
    @FXML
    private void currentPasswordSetOnMousePressed(MouseEvent event) {
        currentPasswordTxtField.setVisible(true);
        currentPasswordTxtField.setManaged(true);
        currentPasswordField.setVisible(false);
        currentPasswordField.setManaged(false);
    }

    @FXML
    private void currentPasswordSetOnMouseReleased(MouseEvent event) {
        currentPasswordField.setVisible(true);
        currentPasswordField.setManaged(true);
        currentPasswordTxtField.setVisible(false);
        currentPasswordTxtField.setManaged(false);
    }

    // New Password - Show/Hide
    @FXML
    private void newPasswordSetOnMousePressed(MouseEvent event) {
        newPasswordTxtField.setVisible(true);
        newPasswordTxtField.setManaged(true);
        newPasswordField.setVisible(false);
        newPasswordField.setManaged(false);
    }

    @FXML
    private void newPasswordSetOnMouseReleased(MouseEvent event) {
        newPasswordField.setVisible(true);
        newPasswordField.setManaged(true);
        newPasswordTxtField.setVisible(false);
        newPasswordTxtField.setManaged(false);
    }

    // Confirm Password - Show/Hide
    @FXML
    private void confirmPasswordSetOnMousePressed(MouseEvent event) {
        confirmPasswordTxtField.setVisible(true);
        confirmPasswordTxtField.setManaged(true);
        confirmPasswordField.setVisible(false);
        confirmPasswordField.setManaged(false);
    }

    @FXML
    private void confirmPasswordSetOnMouseReleased(MouseEvent event) {
        confirmPasswordField.setVisible(true);
        confirmPasswordField.setManaged(true);
        confirmPasswordTxtField.setVisible(false);
        confirmPasswordTxtField.setManaged(false);
    }

    @FXML
    private void handleChangePasswordClicked(ActionEvent event) {
        String currentPassword = currentPasswordField.getText().trim();

        if (currentPassword.isEmpty()) {
            showWarning("🚫 Please enter your current password!");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Kiểm tra mật khẩu hiện tại (sử dụng PreparedStatement để tránh SQL Injection)
            String checkQuery = "SELECT password FROM users WHERE id = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(checkQuery);
            stmt.setInt(1, currentUserId);
            stmt.setString(2, currentPassword);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Mật khẩu đúng - Hiện form nhập mật khẩu mới
                warningLabel.setText("");

                currentPasswordHBox.setVisible(false);
                currentPasswordHBox.setManaged(false);
                ChangeBtnHBox.setVisible(false);
                ChangeBtnHBox.setManaged(false);

                newPasswordHBox.setVisible(true);
                newPasswordHBox.setManaged(true);
                confirmHBox.setVisible(true);
                confirmHBox.setManaged(true);
                ConfirmBtnHBox.setVisible(true);
                ConfirmBtnHBox.setManaged(true);
            } else {
                // Mật khẩu sai
                showWarning("🚫 Your password is incorrect!");
                currentPasswordField.clear();
                currentPasswordTxtField.clear();
            }

        } catch (SQLException e) {
            showWarning("❌ Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleConfirmPasswordClicked(ActionEvent event) {
        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        // Validate input
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showWarning("🚫 Please fill in all fields!");
            return;
        }

        if (newPassword.length() < 6) {
            showWarning("🚫 Password must be at least 6 characters!");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showWarning("🚫 Passwords do not match!");
            newPasswordField.clear();
            confirmPasswordField.clear();
            return;
        }

        // Check if new password is same as current password
        if (newPassword.equals(currentPasswordField.getText())) {
            showWarning("🚫 New password must be different from current password!");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Update password using PreparedStatement
            String updateQuery = "UPDATE users SET password = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(updateQuery);
            stmt.setString(1, newPassword);
            stmt.setInt(2, currentUserId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                showSuccess("✔ Password changed successfully!");

                // Reset form sau 1.5 giây
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        javafx.application.Platform.runLater(this::resetForm);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showWarning("❌ Failed to update password!");
            }

        } catch (SQLException e) {
            showWarning("❌ Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetForm() {
        // Clear all fields
        currentPasswordField.clear();
        currentPasswordTxtField.clear();
        newPasswordField.clear();
        newPasswordTxtField.clear();
        confirmPasswordField.clear();
        confirmPasswordTxtField.clear();
        warningLabel.setText("");

        // Show current password form
        currentPasswordHBox.setVisible(true);
        currentPasswordHBox.setManaged(true);
        ChangeBtnHBox.setVisible(true);
        ChangeBtnHBox.setManaged(true);

        // Hide new password form
        newPasswordHBox.setVisible(false);
        newPasswordHBox.setManaged(false);
        confirmHBox.setVisible(false);
        confirmHBox.setManaged(false);
        ConfirmBtnHBox.setVisible(false);
        ConfirmBtnHBox.setManaged(false);
    }

    private void showWarning(String message) {
        warningLabel.setText(message);
        warningLabel.setStyle("-fx-text-fill: #ff6b6b;");
    }

    private void showSuccess(String message) {
        warningLabel.setText(message);
        warningLabel.setStyle("-fx-text-fill: #51cf66;");
    }

    // Method để set current user từ bên ngoài
    public void setCurrentUser(int userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;
    }
}