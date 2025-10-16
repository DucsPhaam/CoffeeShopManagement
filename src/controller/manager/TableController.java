package controller.manager;

import dao.TableDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
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

    private static final int COLUMNS = 4; // Số cột cố định
    private static final double TABLE_SIZE = 120.0; // Kích thước mỗi bàn (bao gồm padding)
    private static final double PADDING = 20.0; // Khoảng cách giữa các bàn

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

        // Phân loại bàn theo tầng
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

        // Sắp xếp bàn đều trên từng tầng
        arrangeTables(floor1Pane, floor1Tables);
        arrangeTables(floor2Pane, floor2Tables);
        arrangeTables(floor3Pane, floor3Tables);
    }

    private void arrangeTables(Pane floorPane, ObservableList<Table> tables) {
        int tableCount = tables.size();
        int rows = (int) Math.ceil(tableCount / (double) COLUMNS);
        double totalWidth = COLUMNS * TABLE_SIZE + (COLUMNS - 1) * PADDING;
        double totalHeight = rows * TABLE_SIZE + (rows - 1) * PADDING;

        // Căn giữa pane
        floorPane.setPrefSize(totalWidth, totalHeight);
        floorPane.setStyle("-fx-background-color: #f8f9fa;");

        for (int i = 0; i < tableCount; i++) {
            Table table = tables.get(i);
            VBox tableBox = createTableBox(table);

            int row = i / COLUMNS;
            int col = i % COLUMNS;
            double x = col * (TABLE_SIZE + PADDING);
            double y = row * (TABLE_SIZE + PADDING);

            tableBox.setLayoutX(x);
            tableBox.setLayoutY(y);
            floorPane.getChildren().add(tableBox);
        }
    }

    private VBox createTableBox(Table table) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-radius: 5; -fx-cursor: hand; -fx-border-radius: 5; -fx-border-width: 2;");
        box.setPrefSize(100, 100); // Kích thước nội dung bên trong
        String bgColor = switch (table.getStatus()) {
            case "available" -> "#2ECC71";
            case "occupied" -> "#E74C3C";
            case "reserved" -> "#F39C12";
            case "cleaning" -> "#95A5A6";
            default -> "#BDC3C7";
        };
        box.setStyle(box.getStyle() + "-fx-background-color: " + bgColor + "; -fx-border-color: #2C3E50;");
        Label lblName = new Label(table.getName());
        lblName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lblSeats = new Label("👥 " + table.getSeats() + " seats");
        lblSeats.setStyle("-fx-font-size: 11px; -fx-text-fill: white;");
        Label lblStatus = new Label(table.getStatus().toUpperCase());
        lblStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: white;");
        box.getChildren().addAll(lblName, lblSeats, lblStatus);

        box.setOnMouseClicked((MouseEvent e) -> {
            if (e.getClickCount() == 2) {
                handleEditTable(table);
            }
        });

        return box;
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