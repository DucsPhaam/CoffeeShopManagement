package controller.manager;

import dao.TableDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import model.Table;

import java.util.List;

public class TableController {

    @FXML private TabPane floorTabPane;
    @FXML private Pane floor1Pane;
    @FXML private Pane floor2Pane;
    @FXML private Pane floor3Pane;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterStatus;
    @FXML private Label lblTotalTables;

    @FXML private VBox dialogPane;
    @FXML private Pane overlay;
    @FXML private Label dialogTitle;
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cmbFloor;
    @FXML private TextField txtSeats;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private Button btnSave;

    private TableDAO tableDAO;
    private ObservableList<Table> tableList;
    private ObservableList<Table> filteredList;

    private Table currentTable;
    private boolean isEditMode = false;

    private static final int COLUMNS = 4;
    private static final double TABLE_SIZE = 140.0;
    private static final double TABLE_HEIGHT = 160.0;
    private static final double PADDING = 30.0; // Tăng khoảng cách từ 20 lên 30

    @FXML
    public void initialize() {
        tableDAO = new TableDAO();

        tableList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        loadTableData();
        setupSearchAndFilter();
    }

    private void loadTableData() {
        tableList.clear();
        tableList.addAll(tableDAO.getAllTables());
        filteredList.setAll(tableList);
        updateFloorPanes();
        updateStatistics();
    }

    private void updateFloorPanes() {
        floor1Pane.getChildren().clear();
        floor2Pane.getChildren().clear();
        floor3Pane.getChildren().clear();

        ObservableList<Table> floor1Tables = FXCollections.observableArrayList();
        ObservableList<Table> floor2Tables = FXCollections.observableArrayList();
        ObservableList<Table> floor3Tables = FXCollections.observableArrayList();

        for (Table table : filteredList) {
            switch (table.getFloor()) {
                case 1 -> floor1Tables.add(table);
                case 2 -> floor2Tables.add(table);
                case 3 -> floor3Tables.add(table);
            }
        }

        arrangeTables(floor1Pane, floor1Tables);
        arrangeTables(floor2Pane, floor2Tables);
        arrangeTables(floor3Pane, floor3Tables);
    }

    private void arrangeTables(Pane floorPane, ObservableList<Table> tables) {
        int tableCount = tables.size();
        int rows = (int) Math.ceil(tableCount / (double) COLUMNS);
        double totalWidth = COLUMNS * TABLE_SIZE + (COLUMNS - 1) * PADDING;
        double totalHeight = rows * TABLE_HEIGHT + (rows - 1) * PADDING;

        floorPane.setPrefSize(totalWidth, totalHeight);
        floorPane.setStyle("-fx-background-color: #f8f9fa;");

        for (int i = 0; i < tableCount; i++) {
            Table table = tables.get(i);
            VBox tableBox = createTableBox(table);

            int row = i / COLUMNS;
            int col = i % COLUMNS;
            double x = col * (TABLE_SIZE + PADDING);
            double y = row * (TABLE_HEIGHT + PADDING);

            tableBox.setLayoutX(x);
            tableBox.setLayoutY(y);
            floorPane.getChildren().add(tableBox);
        }
    }

    private VBox createTableBox(Table table) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18));
        card.setPrefSize(140, 160);

        String bgColor, borderColor, textColor, statusBadgeColor;
        switch (table.getStatus()) {
            case "available":
                bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f0f9ff 100%)";
                borderColor = "#10b981";
                textColor = "#059669";
                statusBadgeColor = "#d1fae5";
                break;
            case "occupied":
                bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #fef2f2 100%)";
                borderColor = "#ef4444";
                textColor = "#dc2626";
                statusBadgeColor = "#fee2e2";
                break;
            case "reserved":
                bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #fffbeb 100%)";
                borderColor = "#f59e0b";
                textColor = "#d97706";
                statusBadgeColor = "#fed7aa";
                break;
            case "cleaning":
                bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f3f4f6 100%)";
                borderColor = "#6b7280";
                textColor = "#4b5563";
                statusBadgeColor = "#e5e7eb";
                break;
            default:
                bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f3f4f6 100%)";
                borderColor = "#9ca3af";
                textColor = "#6b7280";
                statusBadgeColor = "#e5e7eb";
        }

        // Style mặc định cho card
        String defaultStyle = String.format(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 18; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3); " +
                        "-fx-cursor: hand;",
                borderColor
        );

        card.setStyle(defaultStyle);

        ImageView tableIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/table-icon.png"));
            tableIcon.setImage(icon);
        } catch (Exception e) {
            Label fallbackIcon = new Label("🪑");
            fallbackIcon.setStyle("-fx-font-size: 32px;");
            card.getChildren().add(fallbackIcon);
        }

        tableIcon.setFitWidth(50);
        tableIcon.setFitHeight(50);
        tableIcon.setPreserveRatio(true);
        tableIcon.setOpacity(0.6);

        Label lblName = new Label(table.getName());
        lblName.setStyle(String.format(
                "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: %s;",
                textColor
        ));

        HBox seatsBox = new HBox(5);
        seatsBox.setAlignment(Pos.CENTER);
        Label seatsIcon = new Label("👥");
        seatsIcon.setStyle("-fx-font-size: 14px;");
        Label lblSeats = new Label(table.getSeats() + " seats");
        lblSeats.setStyle(String.format(
                "-fx-font-size: 13px; " +
                        "-fx-text-fill: %s; " +
                        "-fx-font-weight: 600;",
                textColor
        ));
        seatsBox.getChildren().addAll(seatsIcon, lblSeats);

        Label lblStatus = new Label(table.getStatus().toUpperCase());
        lblStatus.setStyle(String.format(
                "-fx-font-size: 10px; " +
                        "-fx-text-fill: %s; " +
                        "-fx-font-weight: 700; " +
                        "-fx-background-color: %s; " +
                        "-fx-padding: 4 10; " +
                        "-fx-background-radius: 12;",
                textColor, statusBadgeColor
        ));

        card.getChildren().addAll(tableIcon, lblName, seatsBox, lblStatus);

        // Double click để edit - áp dụng cho tất cả status
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                handleEditTable(table);
            }
        });

        // Hiệu ứng hover cho TẤT CẢ các status
        final String finalBorderColor = borderColor;
        final String finalBgColor = bgColor;

        card.setOnMouseEntered(e -> {
            card.setStyle(String.format(
                    "-fx-background-color: %s; " +
                            "-fx-background-radius: 18; " +
                            "-fx-border-color: %s; " +
                            "-fx-border-width: 3; " +
                            "-fx-border-radius: 18; " +
                            "-fx-effect: dropshadow(gaussian, %s, 16, 0, 0, 4); " +
                            "-fx-cursor: hand; " +
                            "-fx-scale-x: 1.05; " +
                            "-fx-scale-y: 1.05;",
                    finalBgColor,
                    finalBorderColor,
                    getShadowColor(table.getStatus())
            ));
        });

        card.setOnMouseExited(e -> {
            card.setStyle(defaultStyle);
        });

        return card;
    }

    // Helper method để lấy màu shadow tương ứng với status
    private String getShadowColor(String status) {
        switch (status) {
            case "available":
                return "rgba(16, 185, 129, 0.3)";
            case "occupied":
                return "rgba(239, 68, 68, 0.3)";
            case "reserved":
                return "rgba(245, 158, 11, 0.3)";
            case "cleaning":
                return "rgba(107, 114, 128, 0.3)";
            default:
                return "rgba(156, 163, 175, 0.3)";
        }
    }

    private void updateStatistics() {
        if (lblTotalTables != null) {
            lblTotalTables.setText("Total: " + filteredList.size() + " tables");
        }
    }

    private void setupSearchAndFilter() {
        cmbFilterStatus.setValue("All");
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
        String statusFilter = cmbFilterStatus.getValue();

        filteredList.clear();

        for (Table table : tableList) {
            boolean matchSearch = searchText.isEmpty() ||
                    table.getName().toLowerCase().contains(searchText);

            boolean matchStatus = statusFilter.equals("All") ||
                    table.getStatus().equalsIgnoreCase(statusFilter);

            if (matchSearch && matchStatus) {
                filteredList.add(table);
            }
        }

        updateFloorPanes();
        updateStatistics();
    }

    @FXML
    private void handleRefresh() {
        loadTableData();
        txtSearch.clear();
        cmbFilterStatus.setValue("All");
    }

    @FXML
    private void handleAddTable() {
        isEditMode = false;
        currentTable = null;
        dialogTitle.setText("Add New Table");

        txtName.clear();
        cmbFloor.setValue(null);
        txtSeats.clear();
        cmbStatus.setValue(null);

        showDialog();
    }

    private void handleEditTable(Table table) {
        isEditMode = true;
        currentTable = table;
        dialogTitle.setText("Edit Table");

        txtName.setText(table.getName());
        cmbFloor.setValue(String.valueOf(table.getFloor()));
        txtSeats.setText(String.valueOf(table.getSeats()));
        cmbStatus.setValue(table.getStatus());

        showDialog();
    }

    @FXML
    private void handleSaveTable() {
        if (!validateInput()) {
            return;
        }

        String name = txtName.getText().trim();
        int floor = Integer.parseInt(cmbFloor.getValue());
        int seats = Integer.parseInt(txtSeats.getText().trim());
        String status = cmbStatus.getValue();

        if (isEditMode && currentTable != null) {
            currentTable.setName(name);
            currentTable.setFloor(floor);
            currentTable.setSeats(seats);
            currentTable.setStatus(status);

            if (tableDAO.updateTable(currentTable)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Table updated successfully.");
                loadTableData();
                handleCloseDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update table.");
            }
        } else {
            Table newTable = new Table();
            newTable.setName(name);
            newTable.setFloor(floor);
            newTable.setSeats(seats);
            newTable.setStatus(status);

            if (tableDAO.addTable(newTable)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Table added successfully.");
                loadTableData();
                handleCloseDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add table.");
            }
        }
    }

    private void handleDeleteTable(Table table) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Are you sure you want to delete this table?");
        confirmAlert.setContentText("Table: " + table.getName());

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            if (tableDAO.deleteTable(table.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Table deleted successfully.");
                loadTableData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete table.");
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
        if (txtName.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please enter table name.");
            txtName.requestFocus();
            return false;
        }

        if (cmbFloor.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select floor.");
            cmbFloor.requestFocus();
            return false;
        }

        try {
            int seats = Integer.parseInt(txtSeats.getText().trim());
            if (seats <= 0) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Seats must be greater than 0.");
                txtSeats.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Invalid seats format.");
            txtSeats.requestFocus();
            return false;
        }

        if (cmbStatus.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select status.");
            cmbStatus.requestFocus();
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}