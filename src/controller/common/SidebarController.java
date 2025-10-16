package controller.common;

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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
        // Initial view should be set by the caller (e.g., LoginController)
    }

    public void loadScene(String UI) {
        try {
            root = FXMLLoader.load(getClass().getResource(UI + ".fxml"));
            SceneBorderPane.setCenter(root);
            // Hide sidebar for order view, show for others
            if (UI.equals("/view/staff/order")) {
                SceneBorderPane.setLeft(null); // Hide sidebar for order view
            } else {
                // Ensure sidebar is visible for manager views
                if (SceneBorderPane.getLeft() == null) {
                    SceneBorderPane.setLeft(SceneBorderPane.lookup(".left")); // Reattach sidebar if removed
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void HandleOverviewClicked(ActionEvent event) {
        loadScene("/view/common/dashboard");
    }

    @FXML
    private void HandleOrderClicked(ActionEvent event) {
        loadScene("/view/staff/order");
    }

    @FXML
    private void HandleProductClicked(ActionEvent event) {
        loadScene("/view/admin/product_management");
    }

    @FXML
    private void HandleTableClicked(ActionEvent event) {
        loadScene("/view/admin/table_management");
    }

    @FXML
    private void HandleInventoryClicked(ActionEvent event) {
        loadScene("/view/admin/inventory_management");
    }

    @FXML
    private void HandleCashbookClicked(ActionEvent event) {
        loadScene("/view/common/cashbook");
    }

    @FXML
    private void HandleEmployeeClicked(ActionEvent event) {
        loadScene("/view/admin/user_management");
    }

    @FXML
    private void HandleReportClicked(ActionEvent event) {
        loadScene("/view/admin/report");
    }

    @FXML
    private void HandlePasswordClicked(ActionEvent event) {
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