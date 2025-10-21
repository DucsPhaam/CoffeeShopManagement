package controller.manager;

import dao.InventoryDAO;
import dao.TransactionDAO;
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
import model.Inventory;
import model.Transaction;
import utils.SessionManager;
import javafx.scene.text.Font;
import java.util.List;

public class InventoryController {

    @FXML private TableView<Inventory> tableInventory;
    @FXML private TableColumn<Inventory, Integer> colId;
    @FXML private TableColumn<Inventory, String> colName;
    @FXML private TableColumn<Inventory, Double> colQuantity;
    @FXML private TableColumn<Inventory, String> colUnit;
    @FXML private TableColumn<Inventory, Double> colMinStock;
    @FXML private TableColumn<Inventory, String> colStatus;
    @FXML private TableColumn<Inventory, Void> colActions;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterStock;
    @FXML private Label lblTotalItems;
    @FXML private Label lblLowStockItems;

    @FXML private VBox dialogPane;
    @FXML private VBox addStockPane;
    @FXML private Pane overlay;
    @FXML private Label dialogTitle;
    @FXML private Label lblItemName;
    @FXML private TextField txtName;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtUnit;
    @FXML private TextField txtMinStock;
    @FXML private TextField txtCostPerUnit;
    @FXML private Button btnSave;
    @FXML private TextField txtAddQuantity;
    @FXML private TextField txtAddCostPerUnit;
    @FXML private Button btnAddStock;

    private InventoryDAO inventoryDAO;
    private TransactionDAO transactionDAO;
    private ObservableList<Inventory> inventoryList;
    private ObservableList<Inventory> filteredList;

    private Inventory currentInventory;
    private Inventory currentItemForStock;
    private boolean isEditMode = false;
    private double oldQuantity = 0.0;

    @FXML
    public void initialize() {
        inventoryDAO = new InventoryDAO();
        transactionDAO = new TransactionDAO();

        inventoryList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        setupTableColumns();
        loadInventoryData();
        setupSearchAndFilter();
    }

    private void setupTableColumns() {
    colId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
    colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
    colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));

    // Status column with clear text
    colStatus.setCellValueFactory(cellData -> {
        Inventory inv = cellData.getValue();
        String status = inv.getQuantity() <= inv.getMinStock() ? "Low Stock" : "Normal";
        return new javafx.beans.property.SimpleStringProperty(status);
    });

    colStatus.setCellFactory(col -> new TableCell<Inventory, String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item);
                setFont(Font.font("Segoe UI Semibold", 12));
                if (item.equals("Low Stock")) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #10b981; -fx-font-weight: 700;");
                }
            }
        }
    });


        // Actions column
        colActions.setCellFactory(col -> new TableCell<Inventory, Void>() {
        private final Button btnEdit = new Button("Edit");
        private final Button btnAddStock = new Button("Add Stock");
        private final Button btnDelete = new Button("Delete");
        private final HBox actionBox = new HBox(8);

        {
            // EDIT BUTTON - Blue with clear text
            btnEdit.getStyleClass().add("action-button-edit");
            btnEdit.setFont(Font.font("Segoe UI Semibold", 12));
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

            // ADD STOCK BUTTON - Green with clear text
            btnAddStock.getStyleClass().add("action-button-view");
            btnAddStock.setFont(Font.font("Segoe UI Semibold", 12));
            btnAddStock.setStyle(
                "-fx-background-color: #10b981; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 6 14; " +
                "-fx-background-radius: 5; " +
                "-fx-border-color: transparent; " +
                "-fx-cursor: hand;"
            );
            btnAddStock.setOnMouseEntered(e -> btnAddStock.setStyle(
                btnAddStock.getStyle() + "-fx-background-color: #059669;"
            ));
            btnAddStock.setOnMouseExited(e -> btnAddStock.setStyle(
                btnAddStock.getStyle() + "-fx-background-color: #10b981;"
            ));

            // DELETE BUTTON - Red with clear text
            btnDelete.getStyleClass().add("action-button-delete");
            btnDelete.setFont(Font.font("Segoe UI Semibold", 12));
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
            actionBox.getChildren().addAll(btnEdit, btnAddStock, btnDelete);

            btnEdit.setOnAction(event -> {
                Inventory inv = getTableView().getItems().get(getIndex());
                handleEditInventory(inv);
            });

            btnAddStock.setOnAction(event -> {
                Inventory inv = getTableView().getItems().get(getIndex());
                handleAddStock(inv);
            });

            btnDelete.setOnAction(event -> {
                Inventory inv = getTableView().getItems().get(getIndex());
                handleDeleteInventory(inv);
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

    // Format quantity and min stock with clear font
    colQuantity.setCellFactory(col -> new TableCell<Inventory, Double>() {
        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(String.format("%.2f", item));
                setFont(Font.font("Segoe UI", 13));
                setStyle("-fx-text-fill: #1e293b;");
            }
        }
    });

    colMinStock.setCellFactory(col -> new TableCell<Inventory, Double>() {
        @Override
        protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(String.format("%.2f", item));
                setFont(Font.font("Segoe UI", 13));
                setStyle("-fx-text-fill: #1e293b;");
            }
        }
    });
}

    private void loadInventoryData() {
        inventoryList.clear();
        inventoryList.addAll(inventoryDAO.getAllInventory());
        filteredList.setAll(inventoryList);
        tableInventory.setItems(filteredList);
        updateStatistics();
    }

    private void updateStatistics() {
        int total = filteredList.size();
        int lowStock = (int) filteredList.stream().filter(inv -> inv.getQuantity() <= inv.getMinStock()).count();
        lblTotalItems.setText("Total: " + total + " items");
        lblLowStockItems.setText("Low Stock: " + lowStock);
    }

    private void setupSearchAndFilter() {
        cmbFilterStock.setValue("All");
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
        String stockFilter = cmbFilterStock.getValue();

        filteredList.clear();

        for (Inventory inv : inventoryList) {
            boolean matchSearch = searchText.isEmpty() ||
                    inv.getName().toLowerCase().contains(searchText);

            boolean matchStock = stockFilter.equals("All") ||
                    (stockFilter.equals("Low Stock") && inv.getQuantity() <= inv.getMinStock());

            if (matchSearch && matchStock) {
                filteredList.add(inv);
            }
        }

        tableInventory.setItems(filteredList);
        updateStatistics();
    }

    @FXML
    private void handleRefresh() {
        loadInventoryData();
        txtSearch.clear();
        cmbFilterStock.setValue("All");
    }

    @FXML
    private void handleAddInventory() {
        isEditMode = false;
        currentInventory = null;
        dialogTitle.setText("Add New Item");

        txtName.clear();
        txtQuantity.setText("0.00");
        txtUnit.clear();
        txtMinStock.setText("0.00");
        txtCostPerUnit.setText("0.00");

        showDialog();
    }

    private void handleEditInventory(Inventory inv) {
        isEditMode = true;
        currentInventory = inv;
        dialogTitle.setText("Edit Item");

        txtName.setText(inv.getName());
        txtQuantity.setText(String.format("%.2f", inv.getQuantity()));
        txtUnit.setText(inv.getUnit());
        txtMinStock.setText(String.format("%.2f", inv.getMinStock()));
        txtCostPerUnit.setText(String.format("%.2f", inv.getCostPerUnit()));

        oldQuantity = inv.getQuantity();

        showDialog();
    }

    private void handleAddStock(Inventory inv) {
        currentItemForStock = inv;
        lblItemName.setText(inv.getName());
        txtAddQuantity.setText("0.00");
        txtAddCostPerUnit.setText(String.format("%.2f", inv.getCostPerUnit()));

        showAddStockDialog();
    }

    @FXML
    private void handleAddStock() {
        if (!validateAddStockInput()) {
            return;
        }

        double addQuantity = Double.parseDouble(txtAddQuantity.getText().trim());
        double costPerUnit = Double.parseDouble(txtAddCostPerUnit.getText().trim());
        double newQuantity = currentItemForStock.getQuantity() + addQuantity;
        double expenseAmount = addQuantity * costPerUnit;

        currentItemForStock.setQuantity(newQuantity);

        if (inventoryDAO.updateInventory(currentItemForStock)) {
            insertExpenseTransaction(expenseAmount, "Added " + addQuantity + " " + currentItemForStock.getUnit() + " of " + currentItemForStock.getName());
            showAlert(Alert.AlertType.INFORMATION, "Success", "Stock added successfully.");
            loadInventoryData();
            handleCloseAddStockDialog();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to add stock.");
        }
    }

    private boolean validateAddStockInput() {
        try {
            double addQuantity = Double.parseDouble(txtAddQuantity.getText().trim());
            if (addQuantity <= 0) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Add quantity must be greater than 0.");
                txtAddQuantity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Invalid add quantity format.");
            txtAddQuantity.requestFocus();
            return false;
        }

        try {
            double costPerUnit = Double.parseDouble(txtAddCostPerUnit.getText().trim());
            if (costPerUnit < 0) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Cost per unit cannot be negative.");
                txtAddCostPerUnit.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Invalid cost per unit format.");
            txtAddCostPerUnit.requestFocus();
            return false;
        }

        return true;
    }

    private void handleDeleteInventory(Inventory inv) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Are you sure you want to delete this item?");
        confirmAlert.setContentText("Item: " + inv.getName());

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            if (inventoryDAO.deleteInventory(inv.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Item deleted successfully.");
                loadInventoryData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete item. It may be in use.");
            }
        }
    }

    @FXML
    private void handleSaveInventory() {
        if (!validateInput()) {
            return;
        }

        String name = txtName.getText().trim();
        double quantity = Double.parseDouble(txtQuantity.getText().trim());
        String unit = txtUnit.getText().trim();
        double minStock = Double.parseDouble(txtMinStock.getText().trim());
        double costPerUnit = Double.parseDouble(txtCostPerUnit.getText().trim());

        double addedQuantity = quantity - oldQuantity;
        double expenseAmount = addedQuantity * costPerUnit;

        if (isEditMode && currentInventory != null) {
            currentInventory.setName(name);
            currentInventory.setQuantity(quantity);
            currentInventory.setUnit(unit);
            currentInventory.setMinStock(minStock);
            currentInventory.setCostPerUnit(costPerUnit);

            if (inventoryDAO.updateInventory(currentInventory)) {
                if (addedQuantity > 0) {
                    insertExpenseTransaction(expenseAmount, "Purchased additional " + addedQuantity + " " + unit + " of " + name);
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Item updated successfully.");
                loadInventoryData();
                handleCloseDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update item.");
            }
        } else {
            Inventory newInventory = new Inventory();
            newInventory.setName(name);
            newInventory.setQuantity(quantity);
            newInventory.setUnit(unit);
            newInventory.setMinStock(minStock);
            newInventory.setCostPerUnit(costPerUnit);

            if (inventoryDAO.addInventory(newInventory)) {
                if (quantity > 0) {
                    insertExpenseTransaction(quantity * costPerUnit, "Purchased initial " + quantity + " " + unit + " of " + name);
                }
                showAlert(Alert.AlertType.INFORMATION, "Success", "Item added successfully.");
                loadInventoryData();
                handleCloseDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add item.");
            }
        }
    }

    private void insertExpenseTransaction(double amount, String reason) {
        Transaction transaction = new Transaction();
        transaction.setType("expense");
        transaction.setAmount(amount);
        transaction.setReason(reason);
        transaction.setCreatedBy(SessionManager.getCurrentUserId());
        transactionDAO.addTransaction(transaction);
    }

    @FXML
    private void handleCloseDialog() {
        hideDialog();
    }

    @FXML
    private void handleCloseAddStockDialog() {
        hideAddStockDialog();
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

    private void showAddStockDialog() {
        overlay.setVisible(true);
        overlay.setManaged(true);
        addStockPane.setVisible(true);
        addStockPane.setManaged(true);
    }

    private void hideAddStockDialog() {
        overlay.setVisible(false);
        overlay.setManaged(false);
        addStockPane.setVisible(false);
        addStockPane.setManaged(false);
    }

    private boolean validateInput() {
        if (txtName.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please enter item name.");
            txtName.requestFocus();
            return false;
        }

        if (txtUnit.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please enter unit.");
            txtUnit.requestFocus();
            return false;
        }

        try {
            double quantity = Double.parseDouble(txtQuantity.getText().trim());
            if (quantity < 0) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Quantity cannot be negative.");
                txtQuantity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Invalid quantity format.");
            txtQuantity.requestFocus();
            return false;
        }

        try {
            double minStock = Double.parseDouble(txtMinStock.getText().trim());
            if (minStock < 0) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Min stock cannot be negative.");
                txtMinStock.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Invalid min stock format.");
            txtMinStock.requestFocus();
            return false;
        }

        try {
            double costPerUnit = Double.parseDouble(txtCostPerUnit.getText().trim());
            if (costPerUnit < 0) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Cost per unit cannot be negative.");
                txtCostPerUnit.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Invalid cost per unit format.");
            txtCostPerUnit.requestFocus();
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