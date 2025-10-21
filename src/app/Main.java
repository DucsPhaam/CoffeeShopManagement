package app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import dao.DatabaseConnection;
import utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class Main extends Application {
 // Thêm method này vào Main.java hoặc một static initializer block


// Gọi method này trong main() trước khi launch(args)
public static void main(String[] args) {
  // ← Thêm dòng này
    launch(args);
}

    /**
     * Check and reset pending orders for current user\
     * Uses new database schema with 'users' table
     */
    void checkAndResetPendingOrders() {
        String checkQuery = "SELECT COUNT(1) FROM orders WHERE status = 'unpaid' " +
                "AND staff_id = (SELECT id FROM users WHERE username = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
            checkStmt.setString(1, SessionManager.getCurrentUsername());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) != 0) {
                String updateQuery = "UPDATE orders SET status = 'unpaid' " +
                        "WHERE status = 'unpaid' " +
                        "AND staff_id = (SELECT id FROM users WHERE username = ?)";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setString(1, SessionManager.getCurrentUsername());
                    updateStmt.executeUpdate();
                    System.out.println("Pending orders reset for: " + SessionManager.getCurrentUsername());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking/resetting pending orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Coffee Shop System Management");
        // Load icon from file path directly
        try {
            Image icon = new Image("file:src/resources/Background/icon.png");
            primaryStage.getIcons().add(icon);
            System.out.println("Icon loaded successfully");
        } catch (Exception e) {
            System.err.println("Error loading icon: " + e.getMessage());
        }
        primaryStage.setResizable(false);
        // Load login FXML
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/auth/login.fxml"));
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
            // Handle window close event
            primaryStage.setOnCloseRequest(e -> {
                // Set user status to offline
                SessionManager.setOfflineStatus();
                // Reset any pending orders
                checkAndResetPendingOrders();
                primaryStage.close();
            });
        } catch (Exception e) {
            System.err.println("Error loading login interface: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
