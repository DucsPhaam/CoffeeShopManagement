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
import utils.SweetAlert; // ← Thêm import
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
    @FXML private Button btnViewMore;
    @FXML private Label lblTotalItemsInTable;   // ← mới
    @FXML private Label lblLowStockInTable;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;
    private boolean hasMoreData = true;

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

        // Status column
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
                    if ("Low Stock".equals(item)) {
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
                btnEdit.getStyleClass().add("action-button-edit");
                btnEdit.setFont(Font.font("Segoe UI Semibold", 12));
                btnEdit.setStyle(
                    "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 5; -fx-border-color: transparent; -fx-cursor: hand;"
                );
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(btnEdit.getStyle() + "-fx-background-color: #2563eb;"));
                btnEdit.setOnMouseExited(e -> btnEdit.setStyle(btnEdit.getStyle() + "-fx-background-color: #3b82f6;"));

                btnAddStock.getStyleClass().add("action-button-view");
                btnAddStock.setFont(Font.font("Segoe UI Semibold", 12));
                btnAddStock.setStyle(
                    "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 5; -fx-border-color: transparent; -fx-cursor: hand;"
                );
                btnAddStock.setOnMouseEntered(e -> btnAddStock.setStyle(btnAddStock.getStyle() + "-fx-background-color: #059669;"));
                btnAddStock.setOnMouseExited(e -> btnAddStock.setStyle(btnAddStock.getStyle() + "-fx-background-color: #10b981;"));

                btnDelete.getStyleClass().add("action-button-delete");
                btnDelete.setFont(Font.font("Segoe UI Semibold", 12));
                btnDelete.setStyle(
                    "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 5; -fx-border-color: transparent; -fx-cursor: hand;"
                );
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(btnDelete.getStyle() + "-fx-background-color: #dc2626;"));
                btnDelete.setOnMouseExited(e -> btnDelete.setStyle(btnDelete.getStyle() + "-fx-background-color: #ef4444;"));

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(btnEdit, btnAddStock, btnDelete);

                btnEdit.setOnAction(e -> handleEditInventory(getTableView().getItems().get(getIndex())));
                btnAddStock.setOnAction(e -> handleAddStock(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteInventory(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });

        // Format quantity & min stock
        colQuantity.setCellFactory(col -> new TableCell<Inventory, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
                setFont(Font.font("Segoe UI", 13));
                setStyle("-fx-text-fill: #1e293b;");
            }
        });

        colMinStock.setCellFactory(col -> new TableCell<Inventory, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
                setFont(Font.font("Segoe UI", 13));
                setStyle("-fx-text-fill: #1e293b;");
            }
        });
    }

    private void loadInventoryData() {
        currentPage = 0;
        inventoryList.clear();
        filteredList.clear();
        hasMoreData = true;

        List<Inventory> firstPage = inventoryDAO.getInventoryPage(0, PAGE_SIZE);
        inventoryList.addAll(firstPage);
        filteredList.setAll(firstPage);

        int total = inventoryDAO.getTotalCount();
        hasMoreData = (currentPage + 1) * PAGE_SIZE < total;

        tableInventory.setItems(filteredList);
        updateStatistics();
        updateViewMoreButton();
    }

    @FXML
    private void handleLoadMore() {
        if (!hasMoreData) return;

        currentPage++;
        List<Inventory> nextPage = inventoryDAO.getInventoryPage(currentPage * PAGE_SIZE, PAGE_SIZE);

        if (nextPage.isEmpty()) {
            hasMoreData = false;
        } else {
            inventoryList.addAll(nextPage);
            applyFilters();
        }

        int total = inventoryDAO.getTotalCount();
        hasMoreData = (currentPage + 1) * PAGE_SIZE < total;

        updateStatistics();
        updateViewMoreButton();
    }

    private void updateViewMoreButton() {
        if (btnViewMore != null) {
            btnViewMore.setVisible(hasMoreData);
            btnViewMore.setManaged(hasMoreData);
        }
    }

    private void updateStatistics() {
        int total = filteredList.size();
        int lowStockCount = (int) filteredList.stream()
                .filter(item -> item.getQuantity() < item.getMinStock())
                .count();

        // Thống kê trên đầu
        lblTotalItems.setText(total + " items");
        lblLowStockItems.setText("Low Stock: " + lowStockCount);

        // Thống kê dưới bảng
        lblTotalItemsInTable.setText("Total: " + total + " items");
        lblLowStockInTable.setText("Low Stock: " + lowStockCount);
    }

    private void setupSearchAndFilter() {
        cmbFilterStock.setValue("All");
        cmbFilterStock.getItems().setAll("All", "Low Stock");
    }

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }

    private void applyFilters() {
        String searchText = txtSearch.getText().toLowerCase().trim();
        String stockFilter = cmbFilterStock.getValue();

        filteredList.clear();

        for (Inventory inv : inventoryList) {
            boolean matchSearch = searchText.isEmpty() || inv.getName().toLowerCase().contains(searchText);
            boolean matchStock = "All".equals(stockFilter) ||
                    ("Low Stock".equals(stockFilter) && inv.getQuantity() <= inv.getMinStock());

            if (matchSearch && matchStock) {
                filteredList.add(inv);
            }
        }

        tableInventory.setItems(filteredList);
        updateStatistics();
        updateViewMoreButton();
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
        oldQuantity = inv.getQuantity();
        dialogTitle.setText("Edit Item");
        txtName.setText(inv.getName());
        txtQuantity.setText(String.format("%.2f", inv.getQuantity()));
        txtUnit.setText(inv.getUnit());
        txtMinStock.setText(String.format("%.2f", inv.getMinStock()));
        txtCostPerUnit.setText(String.format("%.2f", inv.getCostPerUnit()));
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
        if (!validateAddStockInput()) return;

        double addQty = Double.parseDouble(txtAddQuantity.getText().trim());
        double costPerUnit = Double.parseDouble(txtAddCostPerUnit.getText().trim());
        double newQty = currentItemForStock.getQuantity() + addQty;
        double expense = addQty * costPerUnit;

        currentItemForStock.setQuantity(newQty);

        if (inventoryDAO.updateInventory(currentItemForStock)) {
            insertExpenseTransaction(expense, "Added " + addQty + " " + currentItemForStock.getUnit() + " of " + currentItemForStock.getName());
            showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Stock added successfully.");
            loadInventoryData();
            handleCloseAddStockDialog();
        } else {
            showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to add stock.");
        }
    }

    private boolean validateAddStockInput() {
        try {
            double qty = Double.parseDouble(txtAddQuantity.getText().trim());
            if (qty <= 0) {
                showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Add quantity must be greater than 0.");
                txtAddQuantity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Invalid quantity format.");
            txtAddQuantity.requestFocus();
            return false;
        }

        try {
            double cost = Double.parseDouble(txtAddCostPerUnit.getText().trim());
            if (cost < 0) {
                showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Cost per unit cannot be negative.");
                txtAddCostPerUnit.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Invalid cost format.");
            txtAddCostPerUnit.requestFocus();
            return false;
        }
        return true;
    }

    // ← DÙNG SWEETALERT CHO XÓA
    private void handleDeleteInventory(Inventory inv) {
        showConfirmation("Confirm Delete", "Are you sure you want to delete this item?\nItem: " + inv.getName(), () -> {
            if (inventoryDAO.deleteInventory(inv.getId())) {
                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Item deleted successfully.");
                loadInventoryData();
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to delete item. It may be in use.");
            }
        }, null);
    }

    @FXML
    private void handleSaveInventory() {
        if (!validateInput()) return;

        String name = txtName.getText().trim();
        double qty = Double.parseDouble(txtQuantity.getText().trim());
        String unit = txtUnit.getText().trim();
        double minStock = Double.parseDouble(txtMinStock.getText().trim());
        double costPerUnit = Double.parseDouble(txtCostPerUnit.getText().trim());

        double addedQty = qty - oldQuantity;
        double expense = addedQty * costPerUnit;

        if (isEditMode && currentInventory != null) {
            currentInventory.setName(name);
            currentInventory.setQuantity(qty);
            currentInventory.setUnit(unit);
            currentInventory.setMinStock(minStock);
            currentInventory.setCostPerUnit(costPerUnit);

            if (inventoryDAO.updateInventory(currentInventory)) {
                if (addedQty > 0) {
                    insertExpenseTransaction(expense, "Purchased additional " + addedQty + " " + unit + " of " + name);
                }
                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Item updated successfully.");
                loadInventoryData();
                handleCloseDialog();
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to update item.");
            }
        } else {
            Inventory newInv = new Inventory();
            newInv.setName(name);
            newInv.setQuantity(qty);
            newInv.setUnit(unit);
            newInv.setMinStock(minStock);
            newInv.setCostPerUnit(costPerUnit);

            if (inventoryDAO.addInventory(newInv)) {
                if (qty > 0) {
                    insertExpenseTransaction(qty * costPerUnit, "Purchased initial " + qty + " " + unit + " of " + name);
                }
                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Item added successfully.");
                loadInventoryData();
                handleCloseDialog();
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to add item.");
            }
        }
    }

    private void insertExpenseTransaction(double amount, String reason) {
        Transaction t = new Transaction();
        t.setType("expense");
        t.setAmount(amount);
        t.setReason(reason);
        t.setCreatedBy(SessionManager.getCurrentUserId());
        transactionDAO.addTransaction(t);
    }

    @FXML private void handleCloseDialog() { hideDialog(); }
    @FXML private void handleCloseAddStockDialog() { hideAddStockDialog(); }

    private void showDialog() {
        overlay.setVisible(true); overlay.setManaged(true);
        dialogPane.setVisible(true); dialogPane.setManaged(true);
    }

    private void hideDialog() {
        overlay.setVisible(false); overlay.setManaged(false);
        dialogPane.setVisible(false); dialogPane.setManaged(false);
    }

    private void showAddStockDialog() {
        overlay.setVisible(true); overlay.setManaged(true);
        addStockPane.setVisible(true); addStockPane.setManaged(true);
    }

    private void hideAddStockDialog() {
        overlay.setVisible(false); overlay.setManaged(false);
        addStockPane.setVisible(false); addStockPane.setManaged(false);
    }

    private boolean validateInput() {
        if (txtName.getText().trim().isEmpty()) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Please enter item name.");
            txtName.requestFocus(); return false;
        }
        if (txtUnit.getText().trim().isEmpty()) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Please enter unit.");
            txtUnit.requestFocus(); return false;
        }

        try { double q = Double.parseDouble(txtQuantity.getText().trim());
            if (q < 0) { showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Quantity cannot be negative."); txtQuantity.requestFocus(); return false; }
        } catch (NumberFormatException e) { showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Invalid quantity."); txtQuantity.requestFocus(); return false; }

        try { double m = Double.parseDouble(txtMinStock.getText().trim());
            if (m < 0) { showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Min stock cannot be negative."); txtMinStock.requestFocus(); return false; }
        } catch (NumberFormatException e) { showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Invalid min stock."); txtMinStock.requestFocus(); return false; }

        try { double c = Double.parseDouble(txtCostPerUnit.getText().trim());
            if (c < 0) { showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Cost cannot be negative."); txtCostPerUnit.requestFocus(); return false; }
        } catch (NumberFormatException e) { showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Invalid cost."); txtCostPerUnit.requestFocus(); return false; }

        return true;
    }

    // ← SWEETALERT CHUNG
    private void showSweetAlert(SweetAlert.AlertType type, String title, String content) {
        try {
            if (tableInventory.getScene() == null) throw new NullPointerException();
            Pane root = (Pane) tableInventory.getScene().getRoot();
            SweetAlert.showAlert(root, type, title, content, null);
        } catch (Exception e) {
            Alert a = new Alert(convertToAlertType(type));
            a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
            a.showAndWait();
        }
    }

    private void showConfirmation(String title, String content, Runnable onConfirm, Runnable onCancel) {
        try {
            if (tableInventory.getScene() == null) throw new NullPointerException();
            Pane root = (Pane) tableInventory.getScene().getRoot();
            SweetAlert.showConfirmation(root, title, content, onConfirm, onCancel);
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
            if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK && onConfirm != null) onConfirm.run();
            else if (onCancel != null) onCancel.run();
        }
    }

    private Alert.AlertType convertToAlertType(SweetAlert.AlertType t) {
        return switch (t) {
            case SUCCESS -> Alert.AlertType.INFORMATION;
            case ERROR -> Alert.AlertType.ERROR;
            case WARNING -> Alert.AlertType.WARNING;
            default -> Alert.AlertType.INFORMATION;
        };
    }
}
