// Thêm vào SidebarController.java

package controller.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import dao.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import utils.SessionManager;

public class SidebarController implements Initializable {
    Stage window;
    Scene scene;
    Parent root;
    TimerTask task;
    Timer timer;
    Connection connectDb = DatabaseConnection.getConnection();
    LocalDate currentDate;
    int currentData;

    @FXML private BorderPane SceneBorderPane;
    @FXML private Button overviewBtn;
    @FXML private Button orderBtn;
    @FXML private Button productBtn;
    @FXML private Button tableBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button cashbookBtn;
    @FXML private Button employeeBtn;
    @FXML private Button reportBtn;
    @FXML private Button passwordBtn;
    @FXML private Button logoutBtn;
    @FXML private Label currentAdminLabel;
    @FXML private ImageView orderAlert;
    @FXML private HBox overviewBox;
    @FXML private HBox orderBox;
    @FXML private HBox productBox;
    @FXML private HBox tableBox;
    @FXML private HBox inventoryBox;
    @FXML private HBox cashbookBox;
    @FXML private HBox employeeBox;
    @FXML private HBox reportBox;
    @FXML private HBox passwordBox;

    // Store current active menu
    private HBox currentActiveMenu = null;

    private void alertOrder() {
        task = new TimerTask() {
            @Override
            public void run() {
                if (getOrderAlert() != 0) {
                    orderAlert.setVisible(true);
                } else {
                    orderAlert.setVisible(false);
                }
            }
        };
        timer = new Timer();
        timer.schedule(task, 0L, 1000L);
    }

    private int getOrderAlert() {
        String checkquery = "SELECT COUNT(1) as counter FROM orders WHERE status = 'unpaid' AND DATE(created_at) = '" + currentDate + "'";
        try {
            Statement statement = connectDb.createStatement();
            ResultSet queryResult = statement.executeQuery(checkquery);
            while (queryResult.next()) {
                currentData = queryResult.getInt("counter");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentData;
    }

    private void checkAdminRole() {
        if (!SessionManager.isManager()) {
            productBox.setVisible(false);
            productBox.managedProperty().bind(productBox.visibleProperty());
            inventoryBox.setVisible(false);
            inventoryBox.managedProperty().bind(inventoryBox.visibleProperty());
            employeeBox.setVisible(false);
            employeeBox.managedProperty().bind(employeeBox.visibleProperty());
            reportBox.setVisible(false);
            reportBox.managedProperty().bind(reportBox.visibleProperty());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        checkAdminRole();
        currentAdminLabel.setText(SessionManager.getCurrentUsername() != null ? SessionManager.getCurrentUsername() : "Guest");
        currentDate = LocalDate.now();
        SessionManager.setOnlineStatus();
        alertOrder();
        
        // Set Overview as default active menu
        setActiveMenu(overviewBox);
    }

    /**
     * Set active menu highlight
     */
    private void setActiveMenu(HBox menuBox) {
        // Remove active class from previous menu
        if (currentActiveMenu != null) {
            currentActiveMenu.getStyleClass().remove("active");
        }
        
        // Add active class to new menu
        if (menuBox != null) {
            menuBox.getStyleClass().add("active");
            currentActiveMenu = menuBox;
        }
    }

   public void loadScene(String UI) {
    try {
        String fxmlPath = UI + ".fxml";
        System.out.println("=== Loading Scene ===");
        System.out.println("Requested path: " + fxmlPath);
        
        // Try multiple methods to load the FXML
        URL fxmlUrl = null;
        
        // Method 1: Try from classpath
        fxmlUrl = getClass().getResource(fxmlPath);
        System.out.println("Method 1 (classpath): " + fxmlUrl);
        
        // Method 2: Try with leading slash
        if (fxmlUrl == null && !fxmlPath.startsWith("/")) {
            fxmlUrl = getClass().getResource("/" + fxmlPath);
            System.out.println("Method 2 (with /): " + fxmlUrl);
        }
        
        // Method 3: Try direct file path (for development in VS Code)
        if (fxmlUrl == null) {
            try {
                String projectRoot = System.getProperty("user.dir");
                File fxmlFile = new File(projectRoot, "src" + fxmlPath);
                System.out.println("Method 3 (direct file): " + fxmlFile.getAbsolutePath());
                
                if (fxmlFile.exists()) {
                    fxmlUrl = fxmlFile.toURI().toURL();
                    System.out.println("✓ Found file directly: " + fxmlUrl);
                }
            } catch (Exception e) {
                System.err.println("Method 3 failed: " + e.getMessage());
            }
        }
        
        if (fxmlUrl == null) {
            throw new IOException("Could not find FXML file: " + fxmlPath);
        }
        
        System.out.println("✓ Using URL: " + fxmlUrl);
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        root = loader.load();
        
        if (root == null) {
            throw new IOException("FXML content is null");
        }
        
        SceneBorderPane.setCenter(root);
        System.out.println("✓ Successfully loaded and displayed scene");
        
        // Hide sidebar for order view
        if (UI.equals("/view/staff/order")) {
            SceneBorderPane.setLeft(null);
        } else {
            if (SceneBorderPane.getLeft() == null) {
                SceneBorderPane.setLeft(SceneBorderPane.lookup(".left"));
            }
        }
        
    } catch (IOException e) {
        System.err.println("✗ Error loading scene: " + UI);
        e.printStackTrace();
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loading Error");
        alert.setHeaderText("Failed to load view");
        alert.setContentText("Could not load: " + UI + "\nError: " + e.getMessage());
        alert.showAndWait();
        
    } catch (Exception e) {
        System.err.println("✗ Unexpected error loading scene: " + UI);
        e.printStackTrace();
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An unexpected error occurred");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}

    @FXML
    private void HandleOverviewClicked(ActionEvent event) {
        setActiveMenu(overviewBox);
        loadScene("/view/common/dashboard");
    }

    @FXML
    private void HandleOrderClicked(ActionEvent event) {
        setActiveMenu(orderBox);
        loadScene("/view/staff/order");
    }

    @FXML
    private void HandleProductClicked(ActionEvent event) {
        setActiveMenu(productBox);
        loadScene("/view/admin/product_management");
    }

    @FXML
    private void HandleTableClicked(ActionEvent event) {
        setActiveMenu(tableBox);
        loadScene("/view/admin/table_management");
    }

    @FXML
    private void HandleInventoryClicked(ActionEvent event) {
        setActiveMenu(inventoryBox);
        loadScene("/view/admin/inventory_management");
    }

    @FXML
    private void HandleCashbookClicked(ActionEvent event) {
        setActiveMenu(cashbookBox);
        loadScene("/view/common/cashbook");
        
        // Ensure sidebar is visible
        if (SceneBorderPane.getLeft() == null) {
            // Re-load sidebar if needed
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/common/sidebar.fxml"));
                Parent sidebar = loader.load();
                SceneBorderPane.setLeft(sidebar);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void HandleEmployeeClicked(ActionEvent event) {
        setActiveMenu(employeeBox);
        loadScene("/view/admin/user_management");
    }

    @FXML
    private void HandleReportClicked(ActionEvent event) {
        setActiveMenu(reportBox);
        loadScene("/view/admin/report");
    }

    @FXML
    private void HandlePasswordClicked(ActionEvent event) {
        setActiveMenu(passwordBox);
        loadScene("/view/auth/change_password");
    }

    @FXML
    private void HandleLogOutClicked(ActionEvent event) throws IOException {
        SessionManager.logout();
        window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        root = FXMLLoader.load(getClass().getResource("/view/auth/login.fxml"));
        scene = new Scene(root);
        window.setScene(scene);
        window.centerOnScreen();
        window.show();
    }
}
