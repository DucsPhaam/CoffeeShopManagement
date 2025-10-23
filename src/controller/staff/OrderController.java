package controller.staff;

import dao.DatabaseConnection;
import dao.OrderDAO;
import dao.ProductIngredientDAO;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.OrderItem;
import utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for managing coffee shop orders in a JavaFX application.
 * Handles table selection, menu display, order processing, and payment.
 */
public class OrderController implements Initializable {

    // FXML Components
    @FXML private Label lblStaffName, lblDateTime, lblSelectedTable, lblItemCount;
    @FXML private Label lblSubtotal, lblVAT, lblTotal, lblNotification;
    @FXML private ToggleButton btnDineIn, btnTakeaway;
    @FXML private VBox tableSelectionPane, menuPane, orderItemsContainer, notificationPane;
    @FXML private TabPane floorTabPane;
    @FXML private GridPane floor1Grid, floor2Grid, floor3Grid, menuGrid;
    @FXML private TextField txtSearchProduct;
    @FXML private Button btnPayment, btnClearOrder, btnPrintBill, btnViewOrders, btnDashboard, btnLogout;
    @FXML private BorderPane mainBorderPane;

    // Data Members
    private ToggleGroup orderTypeGroup;
    private String orderType = "dine-in";
    private String paymentMethod = "cash";
    private Map<String, OrderItem> orderItems = new LinkedHashMap<>();
    private List<ProductData> allProducts = new ArrayList<>();
    private final DecimalFormat df = new DecimalFormat("#,##0.00");
    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductIngredientDAO piDAO = new ProductIngredientDAO();
    private int currentStaffId;
    private Integer selectedTableId = null;
    private VBox paymentOverlay;
    private VBox confirmationOverlay;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentStaffId = SessionManager.getCurrentUserId();
        lblStaffName.setText(SessionManager.getCurrentUsername());

        setupToggleGroups();
        updateDateTime();
        setupSearchFilter();
        loadTables();
        loadProducts();

        btnPayment.setDisable(true);
        startDateTimeUpdater();
    }

    // Setup Methods
    private void setupToggleGroups() {
        orderTypeGroup = new ToggleGroup();
        btnDineIn.setToggleGroup(orderTypeGroup);
        btnTakeaway.setToggleGroup(orderTypeGroup);
        btnDineIn.setSelected(true);
        setupOrderTypeToggle();
    }

    private void setupOrderTypeToggle() {
        String selectedStyle = "-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-pref-width: 200; -fx-pref-height: 50; -fx-cursor: hand;";
        String unselectedStyle = "-fx-background-color: #e5e7eb; -fx-text-fill: #4a5568; -fx-font-size: 15px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-pref-width: 200; -fx-pref-height: 50; -fx-cursor: hand;";

        orderTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == btnDineIn) {
                btnDineIn.setStyle(selectedStyle);
                btnTakeaway.setStyle(unselectedStyle);
                orderType = "dine-in";
                tableSelectionPane.setVisible(true);
                tableSelectionPane.setManaged(true);
                menuPane.setVisible(false);
                menuPane.setManaged(false);
                selectedTableId = null;
                lblSelectedTable.setText("");
            } else if (newVal == btnTakeaway) {
                btnTakeaway.setStyle(selectedStyle);
                btnDineIn.setStyle(unselectedStyle);
                orderType = "takeaway";
                tableSelectionPane.setVisible(false);
                tableSelectionPane.setManaged(false);
                selectedTableId = null;
                lblSelectedTable.setText("(Takeaway)");
                showMenu();
            }
        });
    }

    private void startDateTimeUpdater() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateDateTime()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updateDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy HH:mm:ss");
        lblDateTime.setText(LocalDateTime.now().format(formatter));
    }

    private void setupSearchFilter() {
        txtSearchProduct.textProperty().addListener((obs, oldVal, newVal) -> filterProducts(newVal.trim().toLowerCase()));
    }

    private void filterProducts(String searchText) {
        menuGrid.getChildren().clear();
        int col = 0, row = 0;

        for (ProductData product : allProducts) {
            if (searchText.isEmpty() || product.name.toLowerCase().contains(searchText)) {
                VBox productBox = createProductCard(product);
                menuGrid.add(productBox, col, row);
                col++;
                if (col >= 3) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    // Table Management
    @FXML
    private void handleRefreshTables(ActionEvent event) {
        loadTables();
        showNotification("success", "✓ Tables refreshed!");
    }

    private void loadTables() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, name, floor, seats, status FROM tables ORDER BY floor, name";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            Map<Integer, List<TableData>> floorTables = new HashMap<>();
            while (rs.next()) {
                int floor = rs.getInt("floor");
                TableData table = new TableData(
                        rs.getInt("id"),
                        rs.getString("name"),
                        floor,
                        rs.getInt("seats"),
                        rs.getString("status")
                );
                floorTables.computeIfAbsent(floor, k -> new ArrayList<>()).add(table);
            }

            displayTables(floor1Grid, floorTables.get(1));
            displayTables(floor2Grid, floorTables.get(2));
            displayTables(floor3Grid, floorTables.get(3));
        } catch (SQLException e) {
            showNotification("error", "✗ Failed to load tables: " + e.getMessage());
        }
    }

    private void displayTables(GridPane grid, List<TableData> tables) {
        if (tables == null) return;
        grid.getChildren().clear();
        int col = 0, row = 0;

        for (TableData table : tables) {
            VBox tableBox = createTableCard(table);
            grid.add(tableBox, col, row);
            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createTableCard(TableData table) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18));
        card.setPrefSize(140, 160);

        String bgColor, borderColor, textColor, statusBadgeColor;
        boolean isClickable = false;

        switch (table.status) {
            case "available":
                bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f0f9ff 100%)";
                borderColor = "#10b981";
                textColor = "#059669";
                statusBadgeColor = "#d1fae5";
                isClickable = true;
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

        card.setStyle(String.format(
                "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: %s; -fx-border-width: 3; -fx-border-radius: 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);",
                borderColor
        ));

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

        if (!table.status.equals("available")) {
            tableIcon.setOpacity(0.6);
        }

        Label lblName = new Label(table.name);
        lblName.setStyle(String.format(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: %s;",
                textColor
        ));

        HBox seatsBox = new HBox(5);
        seatsBox.setAlignment(Pos.CENTER);
        Label seatsIcon = new Label("👥");
        seatsIcon.setStyle("-fx-font-size: 14px;");
        Label lblSeats = new Label(table.seats + " seats");
        lblSeats.setStyle(String.format(
                "-fx-font-size: 13px; -fx-text-fill: %s; -fx-font-weight: 600;",
                textColor
        ));
        seatsBox.getChildren().addAll(seatsIcon, lblSeats);

        Label lblStatus = new Label(table.status.toUpperCase());
        lblStatus.setStyle(String.format(
                "-fx-font-size: 10px; -fx-text-fill: %s; -fx-font-weight: 700; -fx-background-color: %s; -fx-padding: 4 10; -fx-background-radius: 12;",
                textColor, statusBadgeColor
        ));

        card.getChildren().addAll(tableIcon, lblName, seatsBox, lblStatus);

        if (isClickable) {
            card.setOnMouseClicked(e -> selectTable(table));
            card.setOnMouseEntered(e -> card.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 18; -fx-border-color: %s; -fx-border-width: 3; -fx-border-radius: 18; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 16, 0, 0, 4); -fx-cursor: hand; -fx-scale-x: 1.05; -fx-scale-y: 1.05;",
                    bgColor, borderColor
            )));
            card.setOnMouseExited(e -> card.setStyle(String.format(
                    "-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: %s; -fx-border-width: 3; -fx-border-radius: 18; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3); -fx-scale-x: 1.0; -fx-scale-y: 1.0;",
                    borderColor
            )));
        } else {
            card.setOpacity(0.7);
        }

        return card;
    }

    private void selectTable(TableData table) {
        selectedTableId = table.id;
        lblSelectedTable.setText("Table: " + table.name + " (" + table.seats + " seats)");
        showMenu();
        showNotification("success", "✓ Table " + table.name + " selected!");
    }

    private void showMenu() {
        menuPane.setVisible(true);
        menuPane.setManaged(true);
        displayProducts();
    }

    // Product Management
    private void loadProducts() {
        allProducts.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, name, price, drink_types, image FROM products ORDER BY name";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String drinkTypesStr = rs.getString("drink_types");
                String[] drinkTypes = drinkTypesStr != null ? drinkTypesStr.split(",") : new String[]{"hot"};
                ProductData product = new ProductData(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        new HashSet<>(Arrays.asList(drinkTypes)),
                        rs.getString("image")
                );
                allProducts.add(product);
            }
        } catch (SQLException e) {
            showNotification("error", "✗ Failed to load products: " + e.getMessage());
        }
    }

    private void displayProducts() {
        menuGrid.getChildren().clear();
        int col = 0, row = 0;

        for (ProductData product : allProducts) {
            VBox productBox = createProductCard(product);
            menuGrid.add(productBox, col, row);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createProductCard(ProductData product) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16));
        box.setPrefSize(160, 220);
        box.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; " +
                        "-fx-border-color: #e5e7eb; -fx-border-width: 2; -fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); -fx-cursor: hand;"
        );

        ImageView imgView = new ImageView();
        String imagePath = product.image != null && !product.image.isEmpty() ? product.image : "img/temp_icon.png";
        try {
            Image image = new Image(getClass().getResourceAsStream("/resources/" + imagePath));
            imgView.setImage(image);
        } catch (Exception e) {
            loadDefaultImage(imgView);
        }
        imgView.setFitWidth(90);
        imgView.setFitHeight(90);
        imgView.setPreserveRatio(true);

        Label lblName = new Label(product.name);
        lblName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        lblName.setWrapText(true);
        lblName.setAlignment(Pos.CENTER);
        lblName.setMaxWidth(140);

        Label lblPrice = new Label("$" + df.format(product.price));
        lblPrice.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #667eea;");

        box.getChildren().addAll(imgView, lblName, lblPrice);

        box.setOnMouseClicked(e -> {
            String drinkType = product.drinkTypes.iterator().next();
            String note = "";
            addToOrder(product, drinkType, product.price, note);
            showNotification("success", "✓ " + product.name + " added!");
        });

        box.setOnMouseEntered(e -> box.setStyle(
                box.getStyle() + "-fx-background-color: #f9fafb; -fx-scale-x: 1.03; -fx-scale-y: 1.03;"
        ));
        box.setOnMouseExited(e -> box.setStyle(
                box.getStyle() + "-fx-background-color: white; -fx-scale-x: 1.0; -fx-scale-y: 1.0;"
        ));

        return box;
    }

    private void loadDefaultImage(ImageView imgView) {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/resources/img/temp_icon.png"));
            imgView.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("Error loading default image: " + e.getMessage());
        }
    }

    private void addToOrder(ProductData product, String drinkType, double price, String note) {
        String key = product.id + "-" + drinkType + "-" + note.hashCode();

        if (orderItems.containsKey(key)) {
            OrderItem item = orderItems.get(key);
            item.setQuantity(item.getQuantity() + 1);
        } else {
            OrderItem item = new OrderItem(0, 0, product.id, drinkType, 1, price, note);
            item.setProductName(product.name);
            item.setAvailableDrinkTypes(new HashSet<>(product.drinkTypes));
            item.setImage(product.image != null && !product.image.isEmpty() ? product.image : "img/temp_icon.png");
            orderItems.put(key, item);
        }

        updateOrderDisplay();
    }

    private void updateOrderDisplay() {
        orderItemsContainer.getChildren().clear();
        double subtotal = 0;
        int itemCount = 0;

        for (OrderItem item : orderItems.values()) {
            HBox itemBox = createOrderItemBox(item);
            orderItemsContainer.getChildren().add(itemBox);
            subtotal += item.getPrice() * item.getQuantity();
            itemCount += item.getQuantity();
        }

        double vat = subtotal * 0.1;
        double total = subtotal + vat;

        lblSubtotal.setText("$" + df.format(subtotal));
        lblVAT.setText("$" + df.format(vat));
        lblTotal.setText("$" + df.format(total));
        lblItemCount.setText(itemCount + " items");

        btnPayment.setDisable(orderItems.isEmpty());
    }

    private HBox createOrderItemBox(OrderItem item) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12));
        box.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-border-color: #e5e7eb; -fx-border-width: 2; -fx-border-radius: 12;"
        );

        ImageView imgView = new ImageView();
        String imagePath = item.getImage() != null && !item.getImage().isEmpty() ?
                item.getImage() : "img/temp_icon.png";
        try {
            Image image = new Image(getClass().getResourceAsStream("/resources/" + imagePath));
            imgView.setImage(image);
        } catch (Exception e) {
            loadDefaultImage(imgView);
        }
        imgView.setFitWidth(60);
        imgView.setFitHeight(60);
        imgView.setPreserveRatio(true);

        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblName = new Label(item.getProductName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a1a1a;");

        HBox drinkTypeBox = new HBox(5);
        Label lblTypeLabel = new Label("Type: ");
        lblTypeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        ComboBox<String> comboDrinkType = new ComboBox<>();
        comboDrinkType.getItems().addAll(item.getAvailableDrinkTypes());
        comboDrinkType.setValue(item.getDrinkType());
        comboDrinkType.setStyle("-fx-font-size: 12px; -fx-pref-width: 100;");
        comboDrinkType.setOnAction(e -> {
            String oldKey = findItemKey(item);
            item.setDrinkType(comboDrinkType.getValue());
            if (oldKey != null) {
                orderItems.remove(oldKey);
                String newKey = item.getProductId() + "-" + item.getDrinkType() + "-" + item.getNote().hashCode();
                orderItems.put(newKey, item);
                updateOrderDisplay();
            }
        });
        drinkTypeBox.getChildren().addAll(lblTypeLabel, comboDrinkType);

        Label lblPrice = new Label("$" + df.format(item.getPrice()));
        lblPrice.setStyle("-fx-font-size: 13px; -fx-text-fill: #667eea; -fx-font-weight: 600;");

        HBox noteBox = new HBox(5);
        Label lblNoteLabel = new Label("Note: ");
        lblNoteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        TextField txtNote = new TextField(item.getNote());
        txtNote.setPromptText("Add note...");
        txtNote.setStyle("-fx-font-size: 12px; -fx-pref-width: 150;");
        txtNote.textProperty().addListener((obs, oldVal, newVal) -> {
            String oldKey = findItemKey(item);
            item.setNote(newVal.trim());
            if (oldKey != null) {
                orderItems.remove(oldKey);
                String newKey = item.getProductId() + "-" + item.getDrinkType() + "-" + item.getNote().hashCode();
                orderItems.put(newKey, item);
                updateOrderDisplay();
            }
        });
        noteBox.getChildren().addAll(lblNoteLabel, txtNote);

        infoBox.getChildren().addAll(lblName, drinkTypeBox, lblPrice, noteBox);

        VBox quantityBox = new VBox(6);
        quantityBox.setAlignment(Pos.CENTER);

        Button btnMinus = new Button("-");
        btnMinus.setStyle(
                "-fx-background-color: #fee; -fx-text-fill: #e53e3e; -fx-font-size: 16px; -fx-font-weight: bold; -fx-pref-width: 36; -fx-pref-height: 36; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnMinus.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                updateOrderDisplay();
            }
        });

        Label lblQty = new Label(String.valueOf(item.getQuantity()));
        lblQty.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        lblQty.setAlignment(Pos.CENTER);
        lblQty.setMinWidth(40);

        Button btnPlus = new Button("+");
        btnPlus.setStyle(
                "-fx-background-color: #d1fae5; -fx-text-fill: #10b981; -fx-font-size: 16px; -fx-font-weight: bold; -fx-pref-width: 36; -fx-pref-height: 36; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnPlus.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            updateOrderDisplay();
        });

        quantityBox.getChildren().addAll(btnPlus, lblQty, btnMinus);

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle(
                "-fx-background-color: #fee; -fx-text-fill: #e53e3e; -fx-font-size: 16px; -fx-pref-width: 40; -fx-pref-height: 40; -fx-background-radius: 10; -fx-cursor: hand;"
        );
        btnDelete.setOnAction(e -> {
            String key = findItemKey(item);
            if (key != null) {
                orderItems.remove(key);
                updateOrderDisplay();
                showNotification("info", "✓ Item removed");
            }
        });

        box.getChildren().addAll(imgView, infoBox, quantityBox, btnDelete);
        return box;
    }

    private String findItemKey(OrderItem item) {
        for (Map.Entry<String, OrderItem> entry : orderItems.entrySet()) {
            if (entry.getValue() == item) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Payment Processing
    @FXML
    private void handlePayment(ActionEvent event) {
        if (orderItems.isEmpty()) {
            showNotification("warning", "⚠ No items in order!");
            return;
        }

        if (orderType.equals("dine-in") && selectedTableId == null) {
            showNotification("warning", "⚠ Please select a table!");
            return;
        }

        showPaymentOverlay();
    }

    private void showPaymentOverlay() {
        paymentOverlay = new VBox(20);
        paymentOverlay.setAlignment(Pos.CENTER);
        paymentOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");
        paymentOverlay.setPrefSize(mainBorderPane.getWidth(), mainBorderPane.getHeight());

        VBox paymentCard = new VBox(20);
        paymentCard.setAlignment(Pos.TOP_CENTER);
        paymentCard.setPadding(new Insets(30));
        paymentCard.setMaxWidth(500);
        paymentCard.setMaxHeight(650);
        paymentCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);"
        );

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleIcon = new Label("💳");
        titleIcon.setStyle("-fx-font-size: 28px;");
        Label title = new Label("Payment");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        header.getChildren().addAll(titleIcon, title);

        Separator sep1 = new Separator();

        // Order Info
        VBox orderInfo = new VBox(8);
        Label orderTypeLabel = new Label("Order Type: " + orderType.toUpperCase());
        orderTypeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568;");

        Label tableLabel = new Label(selectedTableId != null ? lblSelectedTable.getText() : "(Takeaway)");
        tableLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568; -fx-font-weight: 600;");

        Label staffLabel = new Label("Staff: " + lblStaffName.getText());
        staffLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568;");

        Label dateLabel = new Label("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568;");

        orderInfo.getChildren().addAll(orderTypeLabel, tableLabel, staffLabel, dateLabel);

        // Items List
        ScrollPane itemsScroll = new ScrollPane();
        itemsScroll.setFitToWidth(true);
        itemsScroll.setPrefHeight(200);
        itemsScroll.setStyle("-fx-background: transparent; -fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");

        VBox itemsList = new VBox(8);
        itemsList.setPadding(new Insets(10));

        for (OrderItem item : orderItems.values()) {
            HBox itemLine = new HBox(10);
            itemLine.setAlignment(Pos.CENTER_LEFT);

            Label qtyLabel = new Label(item.getQuantity() + " x");
            qtyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #667eea; -fx-min-width: 35;");

            VBox itemDetails = new VBox(2);
            Label nameLabel = new Label(item.getProductName() + " (" + item.getDrinkType() + ")");
            nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1a1a1a;");
            itemDetails.getChildren().add(nameLabel);

            if (!item.getNote().isEmpty()) {
                Label noteLabel = new Label("Note: " + item.getNote());
                noteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
                itemDetails.getChildren().add(noteLabel);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label priceLabel = new Label("$" + df.format(item.getPrice() * item.getQuantity()));
            priceLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

            itemLine.getChildren().addAll(qtyLabel, itemDetails, spacer, priceLabel);
            itemsList.getChildren().add(itemLine);
        }

        itemsScroll.setContent(itemsList);

        Separator sep2 = new Separator();

        // Financial Summary
        VBox financialBox = new VBox(10);
        financialBox.setPadding(new Insets(10));
        financialBox.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 10;");

        HBox subtotalLine = new HBox();
        subtotalLine.setAlignment(Pos.CENTER);
        Label subtotalLabel = new Label("Subtotal:");
        subtotalLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label subtotalValue = new Label(lblSubtotal.getText());
        subtotalValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1a1a1a;");
        subtotalLine.getChildren().addAll(subtotalLabel, spacer1, subtotalValue);

        HBox vatLine = new HBox();
        vatLine.setAlignment(Pos.CENTER);
        Label vatLabel = new Label("VAT (10%):");
        vatLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        Label vatValue = new Label(lblVAT.getText());
        vatValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1a1a1a;");
        vatLine.getChildren().addAll(vatLabel, spacer2, vatValue);

        HBox discountLine = new HBox();
        discountLine.setAlignment(Pos.CENTER);
        Label discountLabel = new Label("Discount:");
        discountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);
        TextField txtDiscount = new TextField("0.00");
        txtDiscount.setStyle("-fx-font-size: 14px; -fx-pref-width: 100; -fx-background-radius: 8;");
        txtDiscount.setPromptText("Enter discount...");
        discountLine.getChildren().addAll(discountLabel, spacer3, txtDiscount);

        Separator sep3 = new Separator();
        sep3.setStyle("-fx-background-color: #d1d5db;");

        HBox totalLine = new HBox();
        totalLine.setAlignment(Pos.CENTER);
        Label totalLabel = new Label("TOTAL:");
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Region spacer4 = new Region();
        HBox.setHgrow(spacer4, Priority.ALWAYS);
        Label totalValue = new Label();
        totalValue.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #667eea;");

        double subtotal = Double.parseDouble(lblSubtotal.getText().replace("$", "").replace(",", ""));
        double vat = Double.parseDouble(lblVAT.getText().replace("$", "").replace(",", ""));
        double initialTotal = subtotal + vat;

        totalValue.setText("$" + df.format(initialTotal));
        txtDiscount.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double discount = Double.parseDouble(newVal);
                double newTotal = initialTotal - discount;
                totalValue.setText("$" + df.format(Math.max(0, newTotal)));
                if (discount < 0) {
                    txtDiscount.setText("0.00");
                    totalValue.setText("$" + df.format(initialTotal));
                }
            } catch (NumberFormatException e) {
                totalValue.setText("$" + df.format(initialTotal));
            }
        });

        totalLine.getChildren().addAll(totalLabel, spacer4, totalValue);
        financialBox.getChildren().addAll(subtotalLine, vatLine, discountLine, sep3, totalLine);

        // Payment Method Selection
        Label paymentMethodLabel = new Label("Select Payment Method:");
        paymentMethodLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #4a5568;");

        HBox paymentMethodBox = new HBox(10);
        paymentMethodBox.setAlignment(Pos.CENTER);

        ToggleGroup paymentToggleGroup = new ToggleGroup();
        String selectedStyle = "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 40; -fx-cursor: hand;";
        String unselectedStyle = "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-pref-width: 100; -fx-pref-height: 40; -fx-cursor: hand;";

        ToggleButton cashBtn = new ToggleButton("💵 Cash");
        ToggleButton cardBtn = new ToggleButton("💳 Card");
        ToggleButton ewalletBtn = new ToggleButton("📱 E-Wallet");
        ToggleButton qrBtn = new ToggleButton("📡 QR");

        cashBtn.setToggleGroup(paymentToggleGroup);
        cardBtn.setToggleGroup(paymentToggleGroup);
        ewalletBtn.setToggleGroup(paymentToggleGroup);
        qrBtn.setToggleGroup(paymentToggleGroup);

        if (paymentMethod.equals("cash")) cashBtn.setSelected(true);
        else if (paymentMethod.equals("card")) cardBtn.setSelected(true);
        else if (paymentMethod.equals("e-wallet")) ewalletBtn.setSelected(true);
        else if (paymentMethod.equals("qr-code")) qrBtn.setSelected(true);

        cashBtn.setStyle(paymentMethod.equals("cash") ? selectedStyle : unselectedStyle);
        cardBtn.setStyle(paymentMethod.equals("card") ? selectedStyle : unselectedStyle);
        ewalletBtn.setStyle(paymentMethod.equals("e-wallet") ? selectedStyle : unselectedStyle);
        qrBtn.setStyle(paymentMethod.equals("qr-code") ? selectedStyle : unselectedStyle);

        paymentMethodBox.getChildren().addAll(cashBtn, cardBtn, ewalletBtn, qrBtn);

        // Cash Payment Fields
        VBox cashPaymentBox = new VBox(10);
        cashPaymentBox.setPadding(new Insets(10));
        cashPaymentBox.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 10;");

        Label amountReceivedLabel = new Label("Amount Received:");
        amountReceivedLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #92400e;");

        TextField txtAmountReceived = new TextField();
        txtAmountReceived.setPromptText("Enter amount...");
        txtAmountReceived.setStyle("-fx-font-size: 14px; -fx-pref-height: 40; -fx-background-radius: 8;");

        Label changeLabel = new Label("Change: $0.00");
        changeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        txtAmountReceived.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double received = Double.parseDouble(newVal);
                double discount = txtDiscount.getText().isEmpty() ? 0 : Double.parseDouble(txtDiscount.getText());
                double total = initialTotal - discount;
                double change = received - total;
                changeLabel.setText("Change: $" + df.format(Math.max(0, change)));
                changeLabel.setStyle(change >= 0 ?
                        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10b981;" :
                        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
            } catch (NumberFormatException e) {
                changeLabel.setText("Change: $0.00");
                changeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
            }
        });

        cashPaymentBox.getChildren().addAll(amountReceivedLabel, txtAmountReceived, changeLabel);
        cashPaymentBox.setVisible(paymentMethod.equals("cash"));
        cashPaymentBox.setManaged(paymentMethod.equals("cash"));

        paymentToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == cashBtn) {
                paymentMethod = "cash";
                cashPaymentBox.setVisible(true);
                cashPaymentBox.setManaged(true);
            } else {
                cashPaymentBox.setVisible(false);
                cashPaymentBox.setManaged(false);
                if (newVal == cardBtn) paymentMethod = "card";
                else if (newVal == ewalletBtn) paymentMethod = "e-wallet";
                else if (newVal == qrBtn) paymentMethod = "qr-code";
            }

            cashBtn.setStyle(newVal == cashBtn ? selectedStyle : unselectedStyle);
            cardBtn.setStyle(newVal == cardBtn ? selectedStyle : unselectedStyle);
            ewalletBtn.setStyle(newVal == ewalletBtn ? selectedStyle : unselectedStyle);
            qrBtn.setStyle(newVal == qrBtn ? selectedStyle : unselectedStyle);
        });

        // Action Buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("Process Payment");
        confirmBtn.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-pref-width: 180; -fx-pref-height: 45; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 8, 0, 0, 2);"
        );
        confirmBtn.setOnAction(e -> {
            double paidAmount;
            double discount;
            try {
                discount = txtDiscount.getText().isEmpty() ? 0 : Double.parseDouble(txtDiscount.getText());
                if (discount < 0) {
                    showNotification("error", "✗ Discount cannot be negative!");
                    return;
                }
            } catch (NumberFormatException ex) {
                showNotification("error", "✗ Invalid discount format!");
                return;
            }

            double total = initialTotal - discount;
            if (paymentMethod.equals("cash")) {
                String amountText = txtAmountReceived.getText().trim();
                if (amountText.isEmpty()) {
                    showNotification("warning", "⚠ Please enter amount received!");
                    return;
                }
                try {
                    paidAmount = Double.parseDouble(amountText);
                    if (paidAmount < total) {
                        showNotification("error", "✗ Insufficient amount received!");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showNotification("error", "✗ Invalid amount format!");
                    return;
                }
            } else {
                paidAmount = total;
            }
            hidePaymentOverlay();
            payment(paidAmount, discount);
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-pref-width: 120; -fx-pref-height: 45; -fx-background-radius: 10; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> hidePaymentOverlay());

        buttonBox.getChildren().addAll(confirmBtn, cancelBtn);

        paymentCard.getChildren().addAll(
                header, sep1, orderInfo, itemsScroll, sep2,
                financialBox, paymentMethodLabel, paymentMethodBox,
                cashPaymentBox, buttonBox
        );

        paymentOverlay.getChildren().add(paymentCard);

        paymentOverlay.setOnMouseClicked(e -> {
            if (e.getTarget() == paymentOverlay) {
                hidePaymentOverlay();
            }
        });

        StackPane root = (StackPane) mainBorderPane.getParent();
        if (root == null) {
            root = new StackPane(mainBorderPane);
            Scene scene = mainBorderPane.getScene();
            scene.setRoot(root);
        }
        root.getChildren().add(paymentOverlay);
    }

    private void hidePaymentOverlay() {
        if (paymentOverlay != null) {
            StackPane root = (StackPane) paymentOverlay.getParent();
            if (root != null) {
                root.getChildren().remove(paymentOverlay);
            }
            paymentOverlay = null;
        }
    }

    private void payment(double paidAmount, double discount) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            Map<Integer, Double> totalIngredientsNeeded = new HashMap<>();
            for (OrderItem item : orderItems.values()) {
                Map<Integer, Double> ingNeeded = piDAO.getIngredientsByProduct(item.getProductId());
                for (Map.Entry<Integer, Double> entry : ingNeeded.entrySet()) {
                    totalIngredientsNeeded.merge(entry.getKey(), entry.getValue() * item.getQuantity(), Double::sum);
                }
            }

            boolean sufficient = piDAO.consumeIngredients(conn, totalIngredientsNeeded);
            if (!sufficient) {
                conn.rollback();
                showNotification("error", "✗ Insufficient ingredients!");
                return;
            }

            int orderId = orderDAO.createOrder(selectedTableId, currentStaffId, orderType);
            if (orderId == -1) {
                conn.rollback();
                showNotification("error", "✗ Failed to create order!");
                return;
            }

            for (OrderItem item : orderItems.values()) {
                boolean success = orderDAO.addOrderItem(
                        orderId, item.getProductId(), item.getDrinkType(),
                        item.getQuantity(), item.getPrice(), item.getNote()
                );
                if (!success) {
                    conn.rollback();
                    showNotification("error", "✗ Failed to add items!");
                    return;
                }
            }

            if (selectedTableId != null) {
                String updateTable = "UPDATE tables SET status = 'occupied' WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateTable)) {
                    stmt.setInt(1, selectedTableId);
                    stmt.executeUpdate();
                }
            }

            double subtotal = orderDAO.calculateOrderTotal(orderId);
            double vat = subtotal * 0.1;
            double total = subtotal + vat - discount;
            double changeReturned = paymentMethod.equals("cash") ? paidAmount - total : 0;

            boolean paymentSuccess = orderDAO.processPayment(
                    orderId, total, vat, paidAmount, changeReturned
            );

            if (!paymentSuccess) {
                conn.rollback();
                showNotification("error", "✗ Payment failed!");
                return;
            }

            conn.commit();
            showSuccessConfirmation(orderId, total, paidAmount, changeReturned);

            clearOrder();
            loadTables();

        } catch (SQLException e) {
            e.printStackTrace();
            showNotification("error", "✗ Error: " + e.getMessage());
        }
    }

    private void showSuccessConfirmation(int orderId, double total, double paidAmount, double change) {
        confirmationOverlay = new VBox(20);
        confirmationOverlay.setAlignment(Pos.CENTER);
        confirmationOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");

        VBox confirmCard = new VBox(25);
        confirmCard.setAlignment(Pos.CENTER);
        confirmCard.setPadding(new Insets(40));
        confirmCard.setMaxWidth(450);
        confirmCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);"
        );

        Label successIcon = new Label("✓");
        successIcon.setStyle("-fx-font-size: 80px; -fx-text-fill: #10b981; -fx-font-weight: bold;");

        Label titleLabel = new Label("Payment Successful!");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        Label orderIdLabel = new Label("Order #" + orderId);
        orderIdLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #667eea; -fx-background-color: #e0e7ff; -fx-padding: 8 20; -fx-background-radius: 20;");

        Separator sep = new Separator();
        sep.setMaxWidth(300);

        VBox detailsBox = new VBox(12);
        detailsBox.setAlignment(Pos.CENTER);
        detailsBox.setPadding(new Insets(15));
        detailsBox.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 12;");

        Label paymentMethodLabel = new Label("Payment Method: " + paymentMethod.toUpperCase());
        paymentMethodLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568; -fx-font-weight: 600;");

        Label totalLabel = new Label("Total: $" + df.format(total));
        totalLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #1a1a1a; -fx-font-weight: bold;");

        if (paymentMethod.equals("cash")) {
            Label paidLabel = new Label("Paid: $" + df.format(paidAmount));
            paidLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568;");

            Label changeLabel = new Label("Change: $" + df.format(change));
            changeLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #10b981; -fx-font-weight: bold;");

            detailsBox.getChildren().addAll(paymentMethodLabel, totalLabel, paidLabel, changeLabel);
        } else {
            detailsBox.getChildren().addAll(paymentMethodLabel, totalLabel);
        }

        Button closeBtn = new Button("Done");
        closeBtn.setStyle(
                "-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-pref-width: 200; -fx-pref-height: 50; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 10, 0, 0, 3);"
        );
        closeBtn.setOnAction(e -> hideSuccessConfirmation());

        confirmCard.getChildren().addAll(successIcon, titleLabel, orderIdLabel, sep, detailsBox, closeBtn);

        confirmationOverlay.getChildren().add(confirmCard);

        StackPane root = (StackPane) mainBorderPane.getParent();
        root.getChildren().add(confirmationOverlay);

        PauseTransition autoClose = new PauseTransition(Duration.seconds(5));
        autoClose.setOnFinished(e -> hideSuccessConfirmation());
        autoClose.play();
    }

    private void hideSuccessConfirmation() {
        if (confirmationOverlay != null) {
            StackPane root = (StackPane) confirmationOverlay.getParent();
            if (root != null) {
                root.getChildren().remove(confirmationOverlay);
            }
            confirmationOverlay = null;
        }
    }

    // Bill Printing
    @FXML
    private void handlePrintBill(ActionEvent event) {
        if (orderItems.isEmpty()) {
            showNotification("warning", "⚠ No items to print!");
            return;
        }

        VBox billContent = createBillContent();
        printNode(billContent);
    }

    private VBox createBillContent() {
        VBox bill = new VBox(10);
        bill.setPadding(new Insets(20));
        bill.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        Label title = new Label("☕ COFFEE SHOP BILL");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label date = new Label("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        date.setStyle("-fx-font-size: 12px;");

        Label staff = new Label("Staff: " + lblStaffName.getText());
        staff.setStyle("-fx-font-size: 12px;");

        Label orderInfo = new Label("Order Type: " + orderType.toUpperCase());
        if (selectedTableId != null) {
            orderInfo.setText(orderInfo.getText() + " | " + lblSelectedTable.getText());
        }
        orderInfo.setStyle("-fx-font-size: 12px;");

        bill.getChildren().addAll(title, date, staff, orderInfo, new Separator());

        for (OrderItem item : orderItems.values()) {
            HBox itemLine = new HBox();
            itemLine.setSpacing(10);
            Label itemLabel = new Label(item.getQuantity() + " x " + item.getProductName() + " (" + item.getDrinkType() + ")");
            itemLabel.setPrefWidth(200);
            Label priceLabel = new Label("$" + df.format(item.getPrice() * item.getQuantity()));
            priceLabel.setStyle("-fx-font-weight: bold;");
            itemLine.getChildren().addAll(itemLabel, priceLabel);
            bill.getChildren().add(itemLine);

            if (!item.getNote().isEmpty()) {
                Label noteLabel = new Label("  Note: " + item.getNote());
                noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
                bill.getChildren().add(noteLabel);
            }
        }

        bill.getChildren().add(new Separator());

        Label subtotalLabel = new Label("Subtotal: " + lblSubtotal.getText());
        Label vatLabel = new Label("VAT (10%): " + lblVAT.getText());
        Label totalLabel = new Label("TOTAL: " + lblTotal.getText());
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label paymentLabel = new Label("Payment Method: " + paymentMethod.toUpperCase());
        paymentLabel.setStyle("-fx-font-size: 12px;");

        bill.getChildren().addAll(subtotalLabel, vatLabel, totalLabel, new Separator(), paymentLabel);

        Label thankYou = new Label("Thank you for your visit!");
        thankYou.setStyle("-fx-font-size: 12px; -fx-font-style: italic;");
        bill.getChildren().add(thankYou);

        return bill;
    }

    private void printNode(VBox node) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showNotification("error", "✗ No printer available!");
            return;
        }

        if (job.showPrintDialog(btnPrintBill.getScene().getWindow())) {
            try {
                boolean success = job.printPage(node);
                if (success) {
                    job.endJob();
                    showNotification("success", "🖨 Bill printed successfully!");
                } else {
                    showNotification("error", "✗ Failed to print!");
                }
            } catch (Exception e) {
                showNotification("error", "✗ Printing error: " + e.getMessage());
            }
        }
    }

    // Order Management
    @FXML
    private void handleClearOrder(ActionEvent event) {
        if (orderItems.isEmpty()) {
            return;
        }

        showClearOrderConfirmation();
    }

    private void showClearOrderConfirmation() {
        VBox confirmOverlay = new VBox(20);
        confirmOverlay.setAlignment(Pos.CENTER);
        confirmOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");

        VBox confirmBox = new VBox(20);
        confirmBox.setAlignment(Pos.CENTER);
        confirmBox.setPadding(new Insets(30));
        confirmBox.setMaxWidth(400);
        confirmBox.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);"
        );

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size: 60px;");

        Label title = new Label("Clear Order?");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        Label message = new Label("This will remove all items from the current order.");
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-text-alignment: center;");
        message.setWrapText(true);
        message.setMaxWidth(350);

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("Yes, Clear");
        confirmBtn.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-pref-width: 140; -fx-pref-height: 45; -fx-background-radius: 10; -fx-cursor: hand;"
        );
        confirmBtn.setOnAction(e -> {
            clearOrder();
            StackPane root = (StackPane) confirmOverlay.getParent();
            root.getChildren().remove(confirmOverlay);
            showNotification("info", "✓ Order cleared!");
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: #e5e7eb; -fx-text-fill: #4a5568; -fx-font-size: 14px; -fx-font-weight: bold; -fx-pref-width: 140; -fx-pref-height: 45; -fx-background-radius: 10; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> {
            StackPane root = (StackPane) confirmOverlay.getParent();
            root.getChildren().remove(confirmOverlay);
        });

        buttonBox.getChildren().addAll(confirmBtn, cancelBtn);

        confirmBox.getChildren().addAll(icon, title, message, buttonBox);
        confirmOverlay.getChildren().add(confirmBox);

        StackPane root = (StackPane) mainBorderPane.getParent();
        if (root == null) {
            root = new StackPane(mainBorderPane);
            Scene scene = mainBorderPane.getScene();
            scene.setRoot(root);
        }
        root.getChildren().add(confirmOverlay);
    }

    private void clearOrder() {
        orderItems.clear();
        updateOrderDisplay();
        selectedTableId = null;
        lblSelectedTable.setText("");
        menuPane.setVisible(false);
        menuPane.setManaged(false);
        btnDineIn.setSelected(true);
    }

    @FXML
    private void handleViewOrders(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/view/staff/view_orders.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("View All Orders");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showNotification("error", "✗ Failed to load orders view: " + e.getMessage());
        }
    }

    // Navigation
    @FXML
    private void handleDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/common/sidebar.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showNotification("error", "✗ Failed to load dashboard!");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        showLogoutConfirmation();
    }

    private void showLogoutConfirmation() {
        VBox logoutOverlay = new VBox(20);
        logoutOverlay.setAlignment(Pos.CENTER);
        logoutOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75);");

        VBox logoutBox = new VBox(20);
        logoutBox.setAlignment(Pos.CENTER);
        logoutBox.setPadding(new Insets(30));
        logoutBox.setMaxWidth(400);
        logoutBox.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);"
        );

        Label icon = new Label("🚪");
        icon.setStyle("-fx-font-size: 60px;");

        Label title = new Label("Confirm Logout");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        Label message = new Label("Are you sure you want to logout?");
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280; -fx-text-alignment: center;");
        message.setWrapText(true);
        message.setMaxWidth(350);

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button confirmBtn = new Button("Yes, Logout");
        confirmBtn.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-pref-width: 140; -fx-pref-height: 45; -fx-background-radius: 10; -fx-cursor: hand;"
        );
        confirmBtn.setOnAction(e -> {
            try {
                SessionManager.logout();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/auth/login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) btnLogout.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                showNotification("error", "✗ Failed to logout!");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color: #e5e7eb; -fx-text-fill: #4a5568; -fx-font-size: 14px; -fx-font-weight: bold; -fx-pref-width: 140; -fx-pref-height: 45; -fx-background-radius: 10; -fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> {
            StackPane root = (StackPane) logoutOverlay.getParent();
            root.getChildren().remove(logoutOverlay);
        });

        buttonBox.getChildren().addAll(confirmBtn, cancelBtn);

        logoutBox.getChildren().addAll(icon, title, message, buttonBox);
        logoutOverlay.getChildren().add(logoutBox);

        StackPane root = (StackPane) mainBorderPane.getParent();
        if (root == null) {
            root = new StackPane(mainBorderPane);
            Scene scene = mainBorderPane.getScene();
            scene.setRoot(root);
        }
        root.getChildren().add(logoutOverlay);
    }

    private void showNotification(String type, String message) {
        String bgColor;
        switch (type.toLowerCase()) {
            case "success":
                bgColor = "#10b981";
                break;
            case "error":
                bgColor = "#ef4444";
                break;
            case "warning":
                bgColor = "#f59e0b";
                break;
            case "info":
                bgColor = "#3b82f6";
                break;
            default:
                bgColor = "#6b7280";
        }

        lblNotification.setText(message);
        lblNotification.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600;");
        notificationPane.setStyle(
                "-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);"
        );

        notificationPane.setVisible(true);
        notificationPane.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            notificationPane.setVisible(false);
            notificationPane.setManaged(false);
        });
        pause.play();
    }

    // Inner Classes
    class TableData {
        int id;
        String name;
        int floor;
        int seats;
        String status;

        TableData(int id, String name, int floor, int seats, String status) {
            this.id = id;
            this.name = name;
            this.floor = floor;
            this.seats = seats;
            this.status = status;
        }
    }

    class ProductData {
        int id;
        String name;
        double price;
        Set<String> drinkTypes;
        String image;

        ProductData(int id, String name, double price, Set<String> drinkTypes, String image) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.drinkTypes = drinkTypes;
            this.image = image;
        }
    }
}