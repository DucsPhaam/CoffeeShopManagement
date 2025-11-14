package controller.common;

import dao.DatabaseConnection;
import dao.TransactionDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.OrderItem;
import model.Transaction;
import utils.SweetAlert;
import utils.SessionManager;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CashbookController {

    @FXML
    private TableView<Transaction> tableTransactions;
    @FXML
    private TableColumn<Transaction, String> colId;
    @FXML
    private TableColumn<Transaction, String> colType;
    @FXML
    private TableColumn<Transaction, Double> colAmount;
    @FXML
    private TableColumn<Transaction, String> colReason;
    @FXML
    private TableColumn<Transaction, Integer> colCreatedBy;
    @FXML
    private TableColumn<Transaction, String> colCreatedAt;
    @FXML
    private TableColumn<Transaction, Void> colActions;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbFilterType;
    @FXML
    private Label lblTotalTransactions;
    @FXML
    private Label lblBalance;

    @FXML
    private VBox dialogPane;
    @FXML
    private Pane overlay;
    @FXML
    private Label dialogTitle;
    @FXML
    private ComboBox<String> cmbType;
    @FXML
    private TextField txtAmount;
    @FXML
    private TextField txtReason;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnViewMore;

    @FXML
    private Label lblTotalIncome;
    @FXML
    private Label lblTotalExpense;
    @FXML
    private Label lblProfitLoss;
    @FXML
    private Label lblCurrentBalance;

    @FXML
    private DatePicker dpFromDate;
    @FXML
    private DatePicker dpToDate;
    @FXML
    private Label lblDate;

    private TransactionDAO transactionDAO;
    private ObservableList<Transaction> transactionList;
    private ObservableList<Transaction> filteredList;

    private Transaction currentTransaction;
    private boolean isEditMode = false;

    private int currentPage = 0;
    private final int PAGE_SIZE = 10;
    private boolean hasMoreRecords = true;

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

            loadInitialTransactionData();
            System.out.println("✓ Initial transaction data loaded");

            setupSearchAndFilter();
            System.out.println("✓ Search and filter setup complete");

            updateDateLabel();
            System.out.println("✓ Date label updated");

            System.out.println("=== CashbookController initialized successfully ===");

        } catch (Exception e) {
            System.err.println("!!! Error initializing CashbookController !!!");
            e.printStackTrace();
            showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to initialize Cashbook: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        colCreatedAt.setCellValueFactory(cellData -> new SimpleStringProperty(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cellData.getValue().getCreatedAt())));

        // Format amount
        colId.setCellValueFactory(cellData -> {
            ObservableList<Transaction> items = tableTransactions.getItems();
            int index = items.indexOf(cellData.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });

        // Format amount
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

        // Actions column
        colActions.setCellFactory(col -> new TableCell<Transaction, Void>() {
            private final Button btnView = new Button("View");
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox actionBox = new HBox(8);

            {
                // VIEW BUTTON - Purple
                btnView.getStyleClass().add("action-button-view");
                btnView.setStyle(
                        "-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 5; -fx-min-width: 70;");
                btnView.setOnMouseEntered(e -> btnView.setStyle(btnView.getStyle().replace("#8b5cf6", "#7c3aed")));
                btnView.setOnMouseExited(e -> btnView.setStyle(btnView.getStyle().replace("#7c3aed", "#8b5cf6")));

                // EDIT BUTTON - Blue
                btnEdit.getStyleClass().add("action-button-edit");
                btnEdit.setStyle(
                        "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 5; -fx-min-width: 70;");
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(btnEdit.getStyle().replace("#3b82f6", "#2563eb")));
                btnEdit.setOnMouseExited(e -> btnEdit.setStyle(btnEdit.getStyle().replace("#2563eb", "#3b82f6")));

                // DELETE BUTTON - Red
                btnDelete.getStyleClass().add("action-button-delete");
                btnDelete.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 14; -fx-background-radius: 5; -fx-min-width: 70;");
                btnDelete
                        .setOnMouseEntered(e -> btnDelete.setStyle(btnDelete.getStyle().replace("#ef4444", "#dc2626")));
                btnDelete.setOnMouseExited(e -> btnDelete.setStyle(btnDelete.getStyle().replace("#dc2626", "#ef4444")));

                actionBox.setAlignment(Pos.CENTER);
                actionBox.getChildren().addAll(btnView, btnEdit, btnDelete);

                btnView.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    handleViewOrderDetails(transaction);
                });

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
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    actionBox.getChildren().clear();
                    if (transaction.getOrderId() != null && transaction.getOrderId() > 0) {
                        actionBox.getChildren().addAll(btnView, btnEdit, btnDelete);
                    } else {
                        actionBox.getChildren().addAll(btnEdit, btnDelete);
                    }
                    setGraphic(actionBox);
                }
            }
        });

        colActions.setMinWidth(250);
        colActions.setPrefWidth(250);
    }

    private void loadInitialTransactionData() {
        currentPage = 0;
        transactionList.clear();
        filteredList.clear();
        hasMoreRecords = true;

        loadNextPage();
        updateViewMoreButton();
    }

    private void loadNextPage() {
        if (!hasMoreRecords) return;

        List<Transaction> newTransactions = transactionDAO.getTransactionsPaginated(currentPage * PAGE_SIZE, PAGE_SIZE);
        if (newTransactions.isEmpty()) {
            hasMoreRecords = false;
        } else {
            transactionList.addAll(newTransactions);
            currentPage++;
        }
        applyFilters(); // Sẽ sort lại
    }

    private void applyFilters() {
        String searchText = txtSearch.getText().toLowerCase().trim();
        String typeFilter = cmbFilterType.getValue();
        Date fromDate = dpFromDate.getValue() != null ? Date.valueOf(dpFromDate.getValue()) : null;
        Date toDate = dpToDate.getValue() != null ? Date.valueOf(dpToDate.getValue()) : null;

        filteredList.clear();

        for (Transaction t : transactionList) {
            boolean matchSearch = searchText.isEmpty() || t.getReason().toLowerCase().contains(searchText);
            boolean matchType = typeFilter == null || "All".equals(typeFilter)
                    || t.getType().equalsIgnoreCase(typeFilter);
            boolean matchDate = (fromDate == null || !t.getCreatedAt().before(fromDate)) &&
                    (toDate == null || !t.getCreatedAt().after(toDate));

            if (matchSearch && matchType && matchDate) {
                filteredList.add(t);
            }
        }

        filteredList.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));
        tableTransactions.setItems(filteredList);
        updateStatistics();

        int totalCount = transactionDAO.getTotalCount();
        hasMoreRecords = (currentPage * PAGE_SIZE) < totalCount;
        updateViewMoreButton();
    }

    private void updateStatistics() {
        if (lblTotalTransactions == null)
            return;

        lblTotalTransactions.setText("Total: " + filteredList.size() + " transactions");

        double totalIncome = 0, totalExpense = 0;
        for (Transaction t : filteredList) {
            if ("income".equalsIgnoreCase(t.getType()))
                totalIncome += t.getAmount();
            else if ("expense".equalsIgnoreCase(t.getType()))
                totalExpense += t.getAmount();
        }
        double profitLoss = totalIncome - totalExpense;
        double balance = transactionDAO.calculateBalance();

        DecimalFormat df = new DecimalFormat("#,##0.00");
        lblTotalIncome.setText("$" + df.format(totalIncome));
        lblTotalExpense.setText("$" + df.format(totalExpense));
        lblProfitLoss.setText("$" + df.format(profitLoss));
        lblCurrentBalance.setText("$" + df.format(balance));
        lblBalance.setText("Balance: $ " + df.format(balance));
    }

    private void setupSearchAndFilter() {
        cmbFilterType.getItems().setAll("All", "Income", "Expense");
        cmbFilterType.setValue("All");

        cmbType.getItems().setAll("Income", "Expense");
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

    @FXML
    private void handleRefresh() {
        loadInitialTransactionData();
        txtSearch.clear();
        cmbFilterType.setValue("All");
        dpFromDate.setValue(null);
        dpToDate.setValue(null);
        updateDateLabel();
    }

    @FXML
    private void handleViewMore() {
        loadNextPage();
    }

    private void updateViewMoreButton() {
        if (btnViewMore != null) {
            btnViewMore.setVisible(hasMoreRecords);
            btnViewMore.setManaged(hasMoreRecords);
        }
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        lblDate.setText(sdf.format(new java.util.Date()));
    }

    @FXML
    private void handleAddTransaction() {
        isEditMode = false;
        currentTransaction = null;
        dialogTitle.setText("Add New Transaction");
        cmbType.setValue(null);
        txtAmount.clear();
        txtReason.clear();
        showDialog();
    }

    private void handleEditTransaction(Transaction transaction) {
        isEditMode = true;
        currentTransaction = transaction;
        dialogTitle.setText("Edit Transaction");
        cmbType.setValue(transaction.getType());
        txtAmount.setText(String.format("%.2f", transaction.getAmount()));
        txtReason.setText(transaction.getReason());
        showDialog();
    }

    private void handleDeleteTransaction(Transaction transaction) {
        showConfirmation("Confirm Delete",
                "Are you sure you want to delete this transaction?\nID: " + transaction.getId(), () -> {
                    if (transactionDAO.deleteTransaction(transaction.getId())) {
                        showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Transaction deleted successfully.");
                        loadInitialTransactionData();
                    } else {
                        showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to delete transaction.");
                    }
                }, null);
    }

    @FXML
    private void handleSaveTransaction() {
        if (!validateInput())
            return;

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
                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Transaction updated successfully.");
                loadInitialTransactionData();
                handleCloseDialog();
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to update transaction.");
            }
        } else {
            Transaction newTransaction = new Transaction();
            newTransaction.setType(type);
            newTransaction.setAmount(amount);
            newTransaction.setReason(reason);
            newTransaction.setCreatedBy(createdBy);

            if (transactionDAO.addTransaction(newTransaction)) {
                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Transaction added successfully.");
                loadInitialTransactionData();
                handleCloseDialog();
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to add transaction.");
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
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Please select a type.");
            cmbType.requestFocus();
            return false;
        }

        try {
            double amount = Double.parseDouble(txtAmount.getText().trim());
            if (amount <= 0) {
                showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Amount must be greater than 0.");
                txtAmount.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Invalid amount format.");
            txtAmount.requestFocus();
            return false;
        }

        if (txtReason.getText().trim().isEmpty()) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "Warning", "Please enter a reason.");
            txtReason.requestFocus();
            return false;
        }

        return true;
    }

    private void showSweetAlert(SweetAlert.AlertType type, String title, String content) {
        try {
            if (tableTransactions.getScene() == null)
                throw new NullPointerException("Scene is null");
            Pane rootPane = (Pane) tableTransactions.getScene().getRoot();
            SweetAlert.showAlert(rootPane, type, title, content, null);
        } catch (Exception e) {
            System.err.println("SweetAlert failed: " + e.getMessage());
            Alert alert = new Alert(convertToAlertType(type));
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        }
    }

    private void showConfirmation(String title, String content, Runnable onConfirm, Runnable onCancel) {
        try {
            if (tableTransactions.getScene() == null)
                throw new NullPointerException("Scene is null");
            Pane rootPane = (Pane) tableTransactions.getScene().getRoot();
            SweetAlert.showConfirmation(rootPane, title, content, onConfirm, onCancel);
        } catch (Exception e) {
            System.err.println("SweetConfirmation failed: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK && onConfirm != null) {
                    onConfirm.run();
                } else if (onCancel != null) {
                    onCancel.run();
                }
            });
        }
    }

    private Alert.AlertType convertToAlertType(SweetAlert.AlertType sweetType) {
        return switch (sweetType) {
            case SUCCESS -> Alert.AlertType.INFORMATION;
            case ERROR -> Alert.AlertType.ERROR;
            case WARNING -> Alert.AlertType.WARNING;
            case QUESTION -> Alert.AlertType.CONFIRMATION;
            default -> Alert.AlertType.INFORMATION;
        };
    }

    private void handleViewOrderDetails(Transaction transaction) {
        if (transaction.getOrderId() == null || transaction.getOrderId() <= 0) {
            showSweetAlert(SweetAlert.AlertType.INFO, "No Order", "This transaction is not linked to an order.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String orderQuery = "SELECT o.*, u.username as staff_name " +
                    "FROM orders o " +
                    "JOIN users u ON o.staff_id = u.id " +
                    "WHERE o.id = ?";
            PreparedStatement orderStmt = conn.prepareStatement(orderQuery);
            orderStmt.setInt(1, transaction.getOrderId());
            ResultSet orderRs = orderStmt.executeQuery();

            if (!orderRs.next()) {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Order not found.");
                return;
            }

            String staffName = orderRs.getString("staff_name");
            String orderType = orderRs.getString("order_type");
            Integer tableId = orderRs.getObject("table_id") != null ? orderRs.getInt("table_id") : null;
            String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(orderRs.getTimestamp("created_at"));

            String itemsQuery = "SELECT oi.*, p.name as product_name, p.image " +
                    "FROM order_items oi " +
                    "JOIN products p ON oi.product_id = p.id " +
                    "WHERE oi.order_id = ?";
            PreparedStatement itemsStmt = conn.prepareStatement(itemsQuery);
            itemsStmt.setInt(1, transaction.getOrderId());
            ResultSet itemsRs = itemsStmt.executeQuery();

            List<OrderItem> orderItems = new ArrayList<>();
            while (itemsRs.next()) {
                OrderItem item = new OrderItem();
                item.setId(itemsRs.getInt("id"));
                item.setProductId(itemsRs.getInt("product_id"));
                item.setProductName(itemsRs.getString("product_name"));
                item.setDrinkType(itemsRs.getString("drink_type"));
                item.setQuantity(itemsRs.getInt("quantity"));
                item.setPrice(itemsRs.getDouble("price"));
                item.setNote(itemsRs.getString("note"));
                item.setImage(itemsRs.getString("image"));
                orderItems.add(item);
            }

            showOrderDetailsDialog(transaction.getOrderId(), staffName, orderType, tableId,
                    createdAt, orderItems, transaction.getAmount());

        } catch (SQLException e) {
            e.printStackTrace();
            showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to load order details: " + e.getMessage());
        }
    }

    private void showOrderDetailsDialog(int orderId, String staffName, String orderType,
                                        Integer tableId, String createdAt,
                                        List<OrderItem> items, double totalAmount) {

        // TẠO STAGE MỚI CHO DIALOG
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(tableTransactions.getScene().getWindow());
        dialogStage.setTitle("Order Details #" + orderId);
        dialogStage.setResizable(false);

        // Tính kích thước dialog (80% màn hình cha)
        Scene parentScene = tableTransactions.getScene();
        double width = Math.min(700, parentScene.getWidth() * 0.8);
        double height = Math.min(700, parentScene.getHeight() * 0.75);

        // ROOT: StackPane để căn giữa
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        root.setPadding(new Insets(20));

        // DIALOG BOX
        VBox dialog = new VBox(16);
        dialog.setMaxSize(width, height);
        dialog.setPrefSize(width, height);
        dialog.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 8);");
        dialog.setPadding(new Insets(24));

        // === HEADER ===
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView icon = new ImageView();
        try {
            icon.setImage(new Image(getClass().getResourceAsStream("/resources/img/clipboard-list-solid-full.png")));
            icon.setFitWidth(24);
            icon.setFitHeight(24);
        } catch (Exception e) {
            /* ignore */ }

        Label title = new Label("Order Details #" + orderId);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 20px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialogStage.close());

        header.getChildren().addAll(icon, title, spacer, closeBtn);

        // === INFO GRID ===
        GridPane info = new GridPane();
        info.setHgap(16);
        info.setVgap(8);
        info.setPadding(new Insets(16));
        info.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12;");

        info.addRow(0, label("Staff:"), value(staffName));
        info.addRow(1, label("Type:"), value(orderType.toUpperCase()));
        info.addRow(2, label("Table:"), value(tableId != null ? "Table " + tableId : "Takeaway"));
        info.addRow(3, label("Date:"), value(createdAt));

        // === ITEMS ===
        Label itemsTitle = new Label("Order Items");
        itemsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(220);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: #f9fafb;");

        VBox itemsList = new VBox(10);
        itemsList.setPadding(new Insets(10));
        DecimalFormat df = new DecimalFormat("#,##0.00");

        for (OrderItem item : items) {
            HBox box = new HBox(10);
            box.setPadding(new Insets(10));
            box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1;");

            // --- ẢNH ---
            ImageView img = new ImageView();
            String path = item.getImage() != null && !item.getImage().isEmpty() ? item.getImage() : "img/temp_icon.png";
            try {
                File f = new File(Paths.get("src/resources/" + path).toAbsolutePath().toString());
                if (f.exists()) {
                    img.setImage(new Image("file:" + f.getAbsolutePath()));
                } else {
                    InputStream is = getClass().getResourceAsStream("/resources/img/temp_icon.png");
                    if (is != null) img.setImage(new Image(is));
                }
            } catch (Exception ex) {
                InputStream is = getClass().getResourceAsStream("/resources/img/temp_icon.png");
                if (is != null) img.setImage(new Image(is));
            }
            img.setFitWidth(44);
            img.setFitHeight(44);
            img.setPreserveRatio(true);

            // --- THÔNG TIN (TÊN + SỐ LƯỢNG + NOTE) ---
            VBox infoBox = new VBox(2);
            HBox.setHgrow(infoBox, Priority.ALWAYS);  // ← BẮT BUỘC
            infoBox.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(item.getProductName() +
                    (item.getDrinkType() != null && !item.getDrinkType().isEmpty() ? " (" + item.getDrinkType() + ")" : ""));
            name.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #1a1a1a;");

            Label qty = new Label(item.getQuantity() + " × $" + df.format(item.getPrice()));
            qty.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #64748b;");

            infoBox.getChildren().addAll(name, qty);

            if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
                Label note = new Label("Note: " + item.getNote());
                note.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
                infoBox.getChildren().add(note);
            }

            // --- GIÁ TIỀN ---
            Label price = new Label("$" + df.format(item.getPrice() * item.getQuantity()));
            price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
            price.setAlignment(Pos.CENTER_RIGHT);

            // --- CĂN CHỈNH: Ảnh | Tên (chiếm hết) | Giá ---
            box.getChildren().addAll(img, infoBox, price);
            HBox.setHgrow(infoBox, Priority.ALWAYS);  // ← LẶP LẠI ĐỂ CHẮC CHẮN
            HBox.setMargin(price, new Insets(0, 10, 0, 0)); // Đẩy giá ra chút

            itemsList.getChildren().add(box);
        }
        scroll.setContent(itemsList);

        // === TOTAL ===
        HBox totalBox = new HBox(16);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setPadding(new Insets(16));
        totalBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12;");
        totalBox.getChildren().addAll(
                new Label("TOTAL:") {
                    {
                        setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                    }
                },
                new Label("$" + df.format(totalAmount)) {
                    {
                        setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
                    }
                });

        // === CLOSE BUTTON ===
        Button close = new Button("Close");
        close.setStyle(
                "-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 120; -fx-pref-height: 40; -fx-background-radius: 10;");
        close.setOnAction(e -> dialogStage.close());

        HBox btnBox = new HBox(close);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(12, 0, 0, 0));

        // GOM DIALOG
        dialog.getChildren().addAll(header, new Separator(), info, itemsTitle, scroll, new Separator(), totalBox,
                btnBox);

        // Đặt dialog vào giữa root
        root.getChildren().add(dialog);
        StackPane.setAlignment(dialog, Pos.CENTER);

        // Tạo Scene và show
        Scene dialogScene = new Scene(root, parentScene.getWidth(), parentScene.getHeight());
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }

    // Helper methods
    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: 600; -fx-text-fill: #475569;");
        return l;
    }

    private Label value(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #1e293b;");
        return l;
    }
}