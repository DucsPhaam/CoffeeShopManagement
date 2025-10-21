package controller.manager;

import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import model.User;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class EmployeeController {

    @FXML private TableView<User> tableEmployees;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colShiftStatus;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterRole;
    @FXML private Label lblTotalEmployees;

    @FXML private VBox dialogPane;
    @FXML private Pane overlay;
    @FXML private Label dialogTitle;
    @FXML private TextField txtUsername;
    @FXML private ComboBox<String> cmbRole;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnSave;
    @FXML private Button btnStartShift;
    @FXML private Button btnEndShift;
    @FXML private ComboBox<String> cmbShiftStatus;
    @FXML private Button btnUpdateShift;

    private UserDAO userDAO;
    private ObservableList<User> userList;
    private ObservableList<User> filteredList;

    private User currentUser;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        userDAO = new UserDAO();

        userList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        setupTableColumns();
        loadUserData();
        setupSearchAndFilter();
        setupShiftControls();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colShiftStatus.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(userDAO.getCurrentShiftStatus(user.getId()));
        });

        colActions.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final Button btnManageShift = new Button("Shift");
            private final HBox actionBox = new HBox(8);

            {
                // EDIT BUTTON - Blue with clear text
                btnEdit.getStyleClass().add("action-button-edit");
                btnEdit.setFont(javafx.scene.text.Font.font("Segoe UI Semibold", 12));
                btnEdit.setStyle(
                    "-fx-background-color: #3b82f6; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-weight: 600; " +
                    "-fx-padding: 6 14; " +
                    "-fx-background-radius: 5; " +
                    "-fx-border-color: transparent; " +
                    "-fx-cursor: hand;"
                );
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(
                    btnEdit.getStyle() + "-fx-background-color: #2563eb;"
                ));
                btnEdit.setOnMouseExited(e -> btnEdit.setStyle(
                    btnEdit.getStyle() + "-fx-background-color: #3b82f6;"
                ));

                // MANAGE SHIFT BUTTON - Green with clear text
                btnManageShift.getStyleClass().add("action-button-view");
                btnManageShift.setFont(javafx.scene.text.Font.font("Segoe UI Semibold", 12));
                btnManageShift.setStyle(
                    "-fx-background-color: #10b981; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-weight: 600; " +
                    "-fx-padding: 6 14; " +
                    "-fx-background-radius: 5; " +
                    "-fx-border-color: transparent; " +
                    "-fx-cursor: hand;"
                );
                btnManageShift.setOnMouseEntered(e -> btnManageShift.setStyle(
                    btnManageShift.getStyle() + "-fx-background-color: #059669;"
                ));
                btnManageShift.setOnMouseExited(e -> btnManageShift.setStyle(
                    btnManageShift.getStyle() + "-fx-background-color: #10b981;"
                ));

                // DELETE BUTTON - Red with clear text
                btnDelete.getStyleClass().add("action-button-delete");
                btnDelete.setFont(javafx.scene.text.Font.font("Segoe UI Semibold", 12));
                btnDelete.setStyle(
                    "-fx-background-color: #ef4444; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 12px; " +
                    "-fx-font-weight: 600; " +
                    "-fx-padding: 6 14; " +
                    "-fx-background-radius: 5; " +
                    "-fx-border-color: transparent; " +
                    "-fx-cursor: hand;"
                );
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(
                    btnDelete.getStyle() + "-fx-background-color: #dc2626;"
                ));
                btnDelete.setOnMouseExited(e -> btnDelete.setStyle(
                    btnDelete.getStyle() + "-fx-background-color: #ef4444;"
                ));

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(btnEdit, btnManageShift, btnDelete);

                btnEdit.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUser(user);
                });

                btnManageShift.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleManageShift(user);
                });

                btnDelete.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBox);
                }
            }
        });
    }

    private void loadUserData() {
        userList.clear();
        userList.addAll(userDAO.getAllUsers());
        filteredList.setAll(userList);
        tableEmployees.setItems(filteredList);
        updateStatistics();
    }

    private void updateStatistics() {
        lblTotalEmployees.setText("Total: " + filteredList.size() + " employees");
    }

    private void setupSearchAndFilter() {
        cmbFilterRole.setValue("All");
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleFilter() {
        applyFilters();
    }

    private void applyFilters() {
        String searchText = txtSearch.getText().toLowerCase().trim();
        String roleFilter = cmbFilterRole.getValue();

        filteredList.clear();

        for (User user : userList) {
            boolean matchSearch = searchText.isEmpty() ||
                    user.getUsername().toLowerCase().contains(searchText);

            boolean matchRole = roleFilter.equals("All") ||
                    user.getRole().equalsIgnoreCase(roleFilter);

            if (matchSearch && matchRole) {
                filteredList.add(user);
            }
        }

        tableEmployees.setItems(filteredList);
        updateStatistics();
    }

    @FXML
    private void handleRefresh() {
        loadUserData();
        txtSearch.clear();
        cmbFilterRole.setValue("All");
    }

    @FXML
    private void handleAddEmployee() {
        isEditMode = false;
        currentUser = null;
        dialogTitle.setText("Add New Employee");

        txtUsername.clear();
        cmbRole.setValue(null);
        txtPassword.clear();

        showDialog();
    }

    private void handleEditUser(User user) {
        isEditMode = true;
        currentUser = user;
        dialogTitle.setText("Edit Employee");

        txtUsername.setText(user.getUsername());
        cmbRole.setValue(user.getRole());
        txtPassword.clear(); // Password is not editable, only set during creation

        showDialog();
    }

    private void handleDeleteUser(User user) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Are you sure you want to delete this employee?");
        confirmAlert.setContentText("Employee: " + user.getUsername());

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            if (userDAO.deleteUser(user.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Employee deleted successfully.");
                loadUserData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete employee. It may be in use.");
            }
        }
    }

    @FXML
    private void handleSaveEmployee() {
        if (!validateInput()) {
            return;
        }

        String username = txtUsername.getText().trim();
        String role = cmbRole.getValue();
        String password = txtPassword.getText().trim();

        if (isEditMode && currentUser != null) {
            currentUser.setUsername(username);
            currentUser.setRole(role);
            // Password is not updated during edit for security; only username and role are editable

            if (userDAO.updateUser(currentUser)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Employee updated successfully.");
                loadUserData();
                handleCloseDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update employee.");
            }
        } else {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setRole(role);
            newUser.setPassword(password); // Set password only during creation

            if (userDAO.addUser(newUser)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Employee added successfully.");
                loadUserData();
                handleCloseDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add employee.");
            }
        }
    }

    @FXML
    private void handleCloseDialog() {
        hideDialog();
    }

    private void showDialog() {
        overlay.setVisible(true);
        overlay.setManaged(true);
        dialogPane.setVisible(true);
        dialogPane.setManaged(true);
    }

    private void hideDialog() {
        overlay.setVisible(false);
        overlay.setManaged(false);
        dialogPane.setVisible(false);
        dialogPane.setManaged(false);
    }

    private boolean validateInput() {
        if (txtUsername.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please enter username.");
            txtUsername.requestFocus();
            return false;
        }

        if (cmbRole.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a role.");
            cmbRole.requestFocus();
            return false;
        }

        if (!isEditMode && txtPassword.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please enter a password.");
            txtPassword.requestFocus();
            return false;
        }

        if (!isEditMode && txtPassword.getText().length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Password must be at least 6 characters long.");
            txtPassword.requestFocus();
            return false;
        }

        return true;
    }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    
    // Enhanced alert styling
    DialogPane dialogPane = alert.getDialogPane();
    dialogPane.setStyle(
        "-fx-background-color: white; " +
        "-fx-font-family: 'Segoe UI'; " +
        "-fx-font-size: 14px; " +
        "-fx-padding: 20;"
    );
    
    // Style the content text
    dialogPane.lookup(".content.label").setStyle(
        "-fx-font-size: 15px; " +
        "-fx-text-fill: #1e293b; " +
        "-fx-font-weight: 600; " +
        "-fx-padding: 15 0;"
    );
    
    // Style buttons
    if (type == Alert.AlertType.INFORMATION) {
        dialogPane.lookupButton(ButtonType.OK).setStyle(
            "-fx-background-color: #10b981; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 10 30; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        );
    } else if (type == Alert.AlertType.ERROR) {
        dialogPane.lookupButton(ButtonType.OK).setStyle(
            "-fx-background-color: #ef4444; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 10 30; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        );
    }
    
    alert.showAndWait();
}

    @FXML
    private void handleStartShift() {
        if (currentUser != null) {
            Timestamp startTime = Timestamp.valueOf(LocalDateTime.now());
            if (userDAO.startShift(currentUser.getId(), startTime)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Shift started for " + currentUser.getUsername());
                loadUserData(); // Refresh to update shift status
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to start shift.");
            }
        }
    }

    @FXML
    private void handleEndShift() {
        if (currentUser != null) {
            if (userDAO.endShift(currentUser.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Shift ended for " + currentUser.getUsername());
                loadUserData(); // Refresh to update shift status
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to end shift. No active shift found.");
            }
        }
    }

    @FXML
    private void handleUpdateShift() {
        if (currentUser != null && cmbShiftStatus.getValue() != null) {
            String status = cmbShiftStatus.getValue();
            if (userDAO.updateShiftStatus(currentUser.getId(), status)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Shift status updated to " + status + " for " + currentUser.getUsername());
                loadUserData(); // Refresh to update shift status
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update shift status.");
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a status.");
        }
    }

    private void handleManageShift(User user) {
        currentUser = user;
        dialogTitle.setText("Manage Shift for " + user.getUsername());
        cmbShiftStatus.setValue(userDAO.getCurrentShiftStatus(user.getId()));
        btnStartShift.setDisable(cmbShiftStatus.getValue() != null && !cmbShiftStatus.getValue().equals("off-duty"));
        btnEndShift.setDisable(cmbShiftStatus.getValue() == null || cmbShiftStatus.getValue().equals("off-duty"));
        showDialog();
    }

    private void setupShiftControls() {
        btnStartShift.setDisable(true);
        btnEndShift.setDisable(true);
        if (currentUser != null) {
            String currentStatus = userDAO.getCurrentShiftStatus(currentUser.getId());
            cmbShiftStatus.setValue(currentStatus);
            btnStartShift.setDisable(currentStatus != null && !currentStatus.equals("off-duty"));
            btnEndShift.setDisable(currentStatus == null || currentStatus.equals("off-duty"));
        }
    }
        }
