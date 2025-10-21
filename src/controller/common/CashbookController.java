package controller.common;

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
import model.Transaction;
import utils.SessionManager;

import java.util.List;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class CashbookController {

    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, Integer> colId;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, Double> colAmount;
    @FXML private TableColumn<Transaction, String> colReason;
    @FXML private TableColumn<Transaction, Integer> colCreatedBy;
    @FXML private TableColumn<Transaction, String> colCreatedAt;
    @FXML private TableColumn<Transaction, Void> colActions;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterType;
    @FXML private Label lblTotalTransactions;
    @FXML private Label lblBalance;

    @FXML private VBox dialogPane;
    @FXML private Pane overlay;
    @FXML private Label dialogTitle;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField txtAmount;
    @FXML private TextField txtReason;
    @FXML private Button btnSave;

    @FXML private Label lblTotalIncome;
    @FXML private Label lblTotalExpense;
    @FXML private Label lblProfitLoss;
    @FXML private Label lblCurrentBalance;

    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;
    @FXML private Label lblDate;

    private TransactionDAO transactionDAO;
    private ObservableList<Transaction> transactionList;
    private ObservableList<Transaction> filteredList;

    private Transaction currentTransaction;
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        try {
            System.out.println("=== Initializing CashbookController ===");
            
            transactionDAO = new TransactionDAO();
            System.out.println("✓ TransactionDAO initialized");
            
            transactionList = FXCollections.observableArrayList();
            filteredList = FXCollections.observableArrayList();
            System.out.println("✓ Observable lists initialized");
            
            setupTableColumns();
            System.out.println("✓ Table columns setup complete");
            
            loadTransactionData();
            System.out.println("✓ Transaction data loaded");
            
            setupSearchAndFilter();
            System.out.println("✓ Search and filter setup complete");
            
            updateDateLabel();
            System.out.println("✓ Date label updated");
            
            System.out.println("=== CashbookController initialized successfully ===");
            
        } catch (Exception e) {
            System.err.println("!!! Error initializing CashbookController !!!");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to initialize Cashbook interface: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        colCreatedAt.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt().toString()));

        // Format amount column
        colAmount.setCellFactory(col -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", item));
                }
            }
        });

        // Actions column (Edit/Delete)
        colActions.setCellFactory(col -> new TableCell<Transaction, Void>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox actionBox = new HBox(10);

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
                actionBox.getChildren().addAll(btnEdit, btnDelete);

                btnEdit.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    handleEditTransaction(transaction);
                });

                btnDelete.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    handleDeleteTransaction(transaction);
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

    private void loadTransactionData() {
        transactionList.clear();
        transactionList.addAll(transactionDAO.getAllTransactions());
        filteredList.setAll(transactionList);
        tableTransactions.setItems(filteredList);
        updateStatistics();
    }

    private void updateStatistics() {
        if (lblTotalTransactions != null && lblBalance != null && lblTotalIncome != null &&
                lblTotalExpense != null && lblProfitLoss != null && lblCurrentBalance != null) {
            lblTotalTransactions.setText("Total: " + filteredList.size() + " transactions");

            double totalIncome = 0;
            double totalExpense = 0;
            for (Transaction t : filteredList) {
                if ("income".equalsIgnoreCase(t.getType())) {
                    totalIncome += t.getAmount();
                } else if ("expense".equalsIgnoreCase(t.getType())) {
                    totalExpense += t.getAmount();
                }
            }
            double profitLoss = totalIncome - totalExpense;
            double balance = transactionDAO.calculateBalance();

            lblTotalIncome.setText(String.format("$%.2f", totalIncome));
            lblTotalExpense.setText(String.format("$%.2f", totalExpense));
            lblProfitLoss.setText(String.format("$%.2f", profitLoss));
            lblCurrentBalance.setText(String.format("$%.2f", balance));
            lblBalance.setText("Balance: $ " + String.format("%.2f", balance));
        }
    }

    private void setupSearchAndFilter() {
        cmbFilterType.setValue("All");
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleFilter() {
        applyFilters();
    }

    @FXML
    private void handleFilterDate() {
        applyFilters();
    }

    private void applyFilters() {
        String searchText = txtSearch.getText().toLowerCase().trim();
        String typeFilter = cmbFilterType.getValue();
        Date fromDate = dpFromDate.getValue() != null ? Date.valueOf(dpFromDate.getValue()) : null;
        Date toDate = dpToDate.getValue() != null ? Date.valueOf(dpToDate.getValue()) : null;

        filteredList.clear();

        for (Transaction transaction : transactionList) {
            boolean matchSearch = searchText.isEmpty() ||
                    transaction.getReason().toLowerCase().contains(searchText);

            boolean matchType = typeFilter.equals("All") ||
                    transaction.getType().equalsIgnoreCase(typeFilter);

            boolean matchDate = (fromDate == null || !transaction.getCreatedAt().before(fromDate)) &&
                    (toDate == null || !transaction.getCreatedAt().after(toDate));

            if (matchSearch && matchType && matchDate) {
                filteredList.add(transaction);
            }
        }

        tableTransactions.setItems(filteredList);
        updateStatistics();
    }

    @FXML
    private void handleRefresh() {
        loadTransactionData();
        txtSearch.clear();
        cmbFilterType.setValue("All");
        dpFromDate.setValue(null);
        dpToDate.setValue(null);
        updateDateLabel();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        lblDate.setText(sdf.format(new java.util.Date()));
    }

    @FXML
    private void handleAddTransaction() {
        isEditMode = false;
        currentTransaction = null;
        if (dialogTitle != null) {
            dialogTitle.setText("Add New Transaction");
        }
        if (cmbType != null) {
            cmbType.setValue(null);
        }
        if (txtAmount != null) {
            txtAmount.clear();
        }
        if (txtReason != null) {
            txtReason.clear();
        }

        showDialog();
    }

    private void handleEditTransaction(Transaction transaction) {
        isEditMode = true;
        currentTransaction = transaction;
        if (dialogTitle != null) {
            dialogTitle.setText("Edit Transaction");
        }
        if (cmbType != null) {
            cmbType.setValue(transaction.getType());
        }
        if (txtAmount != null) {
            txtAmount.setText(String.format("%.2f", transaction.getAmount()));
        }
        if (txtReason != null) {
            txtReason.setText(transaction.getReason());
        }

        showDialog();
    }

    private void handleDeleteTransaction(Transaction transaction) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Are you sure you want to delete this transaction?");
        confirmAlert.setContentText("Transaction ID: " + transaction.getId());

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            if (transactionDAO.deleteTransaction(transaction.getId())) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Transaction deleted successfully.");
                loadTransactionData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete transaction.");
            }
        }
    }

    @FXML
    private void handleSaveTransaction() {
        if (!validateInput()) {
            return;
        }

        String type = cmbType.getValue();
        double amount = Double.parseDouble(txtAmount.getText().trim());
        String reason = txtReason.getText().trim();
        int createdBy = SessionManager.getCurrentUserId();

        if (isEditMode && currentTransaction != null) {
            currentTransaction.setType(type);
            currentTransaction.setAmount(amount);
            currentTransaction.setReason(reason);
            currentTransaction.setCreatedBy(createdBy);

            if (transactionDAO.updateTransaction(currentTransaction)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Transaction updated successfully.");
                loadTransactionData();
                handleCloseDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update transaction.");
            }
        } else {
            Transaction newTransaction = new Transaction();
            newTransaction.setType(type);
            newTransaction.setAmount(amount);
            newTransaction.setReason(reason);
            newTransaction.setCreatedBy(createdBy);

            if (transactionDAO.addTransaction(newTransaction)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Transaction added successfully.");
                loadTransactionData();
                handleCloseDialog();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add transaction.");
            }
        }
    }

    @FXML
    private void handleCloseDialog() {
        hideDialog();
    }

    private void showDialog() {
        if (overlay != null && dialogPane != null) {
            overlay.setVisible(true);
            overlay.setManaged(true);
            dialogPane.setVisible(true);
            dialogPane.setManaged(true);
        } else {
            System.err.println("overlay or dialogPane is null in showDialog");
        }
    }

    private void hideDialog() {
        if (overlay != null && dialogPane != null) {
            overlay.setVisible(false);
            overlay.setManaged(false);
            dialogPane.setVisible(false);
            dialogPane.setManaged(false);
        }
    }

    private boolean validateInput() {
        if (cmbType.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select type.");
            cmbType.requestFocus();
            return false;
        }

        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Amount must be greater than 0.");
                txtAmount.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Invalid amount format.");
            txtAmount.requestFocus();
            return false;
        }

        if (txtReason.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please enter reason.");
            txtReason.requestFocus();
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
