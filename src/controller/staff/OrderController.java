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
import utils.SweetAlert; // ← Thêm import
import model.Product;
import model.Table;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private List<Product> allProducts = new ArrayList<>();
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
        String unselectedStyle = "-fx-background-color: #e5e7eb; -fx-text-fill: #4a5568; -fx-font-size: 15px; -fx-font-weight: 600; -fx-background-radius: 12; -fx-pref-width: 200; -fx-pref-height: 50; -fx-cursor: hand; -fx-padding: 0 10 0 10;";

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
        txtSearchProduct.textProperty()
                .addListener((obs, oldVal, newVal) -> filterProducts(newVal.trim().toLowerCase()));
    }

    private void filterProducts(String searchText) {
        menuGrid.getChildren().clear();
        int col = 0, row = 0;

        for (Product product : allProducts) {
            if (searchText.isEmpty() || product.getName().toLowerCase().contains(searchText)) {
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
        showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Tables refreshed!");
    }

    private void loadTables() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, name, floor, seats, status FROM tables ORDER BY floor, name";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            Map<Integer, List<Table>> floorTables = new HashMap<>();
            while (rs.next()) {
                int floor = rs.getInt("floor");
                Table table = new Table(
                        rs.getInt("id"),
                        rs.getString("name"),
                        floor,
                        rs.getInt("seats"),
                        rs.getString("status"));
                floorTables.computeIfAbsent(floor, k -> new ArrayList<>()).add(table);
            }

            displayTables(floor1Grid, floorTables.get(1));
            displayTables(floor2Grid, floorTables.get(2));
            displayTables(floor3Grid, floorTables.get(3));
        } catch (SQLException e) {
            showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to load tables: " + e.getMessage());
        }
    }

    private void displayTables(GridPane grid, List<Table> tables) {
        if (tables == null) return;
        grid.getChildren().clear();
        grid.setHgap(15); grid.setVgap(15);
        int col = 0, row = 0, columnsPerRow = 5;

        for (Table table : tables) {
            VBox tableBox = createTableCard(table);
            grid.add(tableBox, col, row);
            col++;
            if (col >= columnsPerRow) { col = 0; row++; }
        }
    }

    private VBox createTableCard(Table table) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setPrefSize(120, 140); card.setMaxSize(120, 140);

        String bgColor, borderColor, textColor, statusBadgeColor;
        boolean isClickable = false;

        switch (table.getStatus()) {
            case "available" -> { bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f0f9ff 100%)"; borderColor = "#10b981"; textColor = "#059669"; statusBadgeColor = "#d1fae5"; isClickable = true; }
            case "occupied" -> { bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #fef2f2 100%)"; borderColor = "#ef4444"; textColor = "#dc2626"; statusBadgeColor = "#fee2e2"; }
            case "reserved" -> { bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #fffbeb 100%)"; borderColor = "#f59e0b"; textColor = "#d97706"; statusBadgeColor = "#fed7aa"; }
            case "cleaning" -> { bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f3f4f6 100%)"; borderColor = "#6b7280"; textColor = "#4b5563"; statusBadgeColor = "#e5e7eb"; }
            default -> { bgColor = "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f3f4f6 100%)"; borderColor = "#9ca3af"; textColor = "#6b7280"; statusBadgeColor = "#e5e7eb"; }
        }

        card.setStyle(String.format(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: %s; -fx-border-width: 2; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);",
                borderColor));

        ImageView tableIcon = new ImageView();
        try { Image icon = new Image(getClass().getResourceAsStream("/resources/img/table-icon.png")); tableIcon.setImage(icon); } catch (Exception e) { System.err.println("Failed to load table icon"); }
        tableIcon.setFitWidth(40); tableIcon.setFitHeight(40); tableIcon.setPreserveRatio(true);
        if (!"available".equals(table.getStatus())) tableIcon.setOpacity(0.6);

        Label lblName = new Label(table.getName());
        lblName.setStyle(String.format("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: %s;", textColor));
        lblName.setMaxWidth(100); lblName.setWrapText(true); lblName.setAlignment(Pos.CENTER);

        HBox seatsBox = new HBox(4); seatsBox.setAlignment(Pos.CENTER);
        ImageView seatsIcon = new ImageView();
        try { Image icon = new Image(getClass().getResourceAsStream("/resources/img/user-solid-full.png")); seatsIcon.setImage(icon); seatsIcon.setFitWidth(12); seatsIcon.setFitHeight(12); seatsIcon.setPreserveRatio(true); } catch (Exception e) { System.err.println("Failed to load people icon"); }
        Label lblSeats = new Label(table.getSeats() + " seats");
        lblSeats.setStyle(String.format("-fx-font-size: 11px; -fx-text-fill: %s; -fx-font-weight: 600;", textColor));
        seatsBox.getChildren().addAll(seatsIcon, lblSeats);

        Label lblStatus = new Label(table.getStatus().toUpperCase());
        lblStatus.setStyle(String.format("-fx-font-size: 9px; -fx-text-fill: %s; -fx-font-weight: 700; -fx-background-color: %s; -fx-padding: 3 8; -fx-background-radius: 10;", textColor, statusBadgeColor));

        card.getChildren().addAll(tableIcon, lblName, seatsBox, lblStatus);

        if (isClickable) {
            card.setOnMouseClicked(e -> selectTable(table));
            card.setOnMouseEntered(e -> card.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 12; -fx-border-color: %s; -fx-border-width: 2; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 12, 0, 0, 3); -fx-cursor: hand; -fx-scale-x: 1.03; -fx-scale-y: 1.03;",
                    bgColor, borderColor)));
            card.setOnMouseExited(e -> card.setStyle(String.format(
                    "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: %s; -fx-border-width: 2; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2); -fx-scale-x: 1.0; -fx-scale-y: 1.0;",
                    borderColor)));
        } else {
            card.setOpacity(0.7);
        }

        return card;
    }

    private void selectTable(Table table) {
        selectedTableId = table.getId();  // ← getId()
        lblSelectedTable.setText("Table: " + table.getName() + " (" + table.getSeats() + " seats)");
        showMenu();
        showSweetAlert(SweetAlert.AlertType.SUCCESS, "Table Selected", "Table " + table.getName() + " selected!");
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
                String[] drinkTypes = drinkTypesStr != null ? drinkTypesStr.split(",") : new String[] { "hot" };
                Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        new HashSet<>(Arrays.asList(drinkTypes)),
                        rs.getString("image")
                );
                allProducts.add(product);
            }
        } catch (SQLException e) {
            showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to load products: " + e.getMessage());
        }
    }

    private void displayProducts() {
        menuGrid.getChildren().clear();
        int col = 0, row = 0;
        for (Product product : allProducts) {
            VBox productBox = createProductCard(product);
            menuGrid.add(productBox, col, row);
            col++;
            if (col >= 3) { col = 0; row++; }
        }
    }

    private VBox createProductCard(Product product) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: #BDC3C7; -fx-border-radius: 8; -fx-border-width: 1;");
        box.setPrefSize(150, 200);

        ImageView imgView = new ImageView();
        String imagePath = product.getImage() != null && !product.getImage().isEmpty() ? product.getImage() : "img/temp_icon.png";
        String fullPath = Paths.get("src/resources/" + imagePath).toAbsolutePath().toString();
        try {
            File file = new File(fullPath);
            if (file.exists()) {
                imgView.setImage(new Image("file:" + fullPath));
            } else {
                loadDefaultImage(imgView);
            }
        } catch (Exception e) {
            loadDefaultImage(imgView);
        }
        imgView.setFitWidth(80); imgView.setFitHeight(80); imgView.setPreserveRatio(true);

        Label lblName = new Label(product.getName());
        lblName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        lblName.setWrapText(true); lblName.setAlignment(Pos.CENTER);

        box.getChildren().addAll(imgView, lblName);
        box.setOnMouseClicked(e -> addToOrder(product, product.getDrinkTypes().iterator().next(), product.getPrice(), ""));
        box.setOnMouseEntered(e -> box.setStyle(box.getStyle() + "-fx-background-color: #ECF0F1;"));
        box.setOnMouseExited(e -> box.setStyle(box.getStyle().replace("-fx-background-color: #ECF0F1;", "-fx-background-color: white;")));

        return box;
    }

    private void loadDefaultImage(ImageView imgView) {
        try {
            String path = "src/resources/img/temp_icon.png";
            File file = new File(path);
            if (file.exists()) imgView.setImage(new Image(file.toURI().toString()));
        } catch (Exception e) { System.err.println("Default image failed"); }
    }

    private void addToOrder(Product product, String drinkType, double price, String note) {
        String key = product.getId() + "-" + drinkType + "-" + note.hashCode();
        if (orderItems.containsKey(key)) {
            orderItems.get(key).setQuantity(orderItems.get(key).getQuantity() + 1);
        } else {
            OrderItem item = new OrderItem(0, 0, product.getId(), drinkType, 1, price, note);
            item.setProductName(product.getName());
            item.setAvailableDrinkTypes(new HashSet<>(product.getDrinkTypes()));
            item.setImage(product.getImage() != null && !product.getImage().isEmpty() ? product.getImage() : "img/temp_icon.png");
            orderItems.put(key, item);
        }
        updateOrderDisplay();
    }

    private void updateOrderDisplay() {
        orderItemsContainer.getChildren().clear();
        double subtotal = 0; int itemCount = 0;
        for (OrderItem item : orderItems.values()) {
            orderItemsContainer.getChildren().add(createOrderItemBox(item));
            subtotal += item.getPrice() * item.getQuantity();
            itemCount += item.getQuantity();
        }
        double vat = subtotal * 0.1, total = subtotal + vat;
        lblSubtotal.setText("$" + df.format(subtotal));
        lblVAT.setText("$" + df.format(vat));
        lblTotal.setText("$" + df.format(total));
        lblItemCount.setText(itemCount + " items");
        btnPayment.setDisable(orderItems.isEmpty());
    }

    private HBox createOrderItemBox(OrderItem item) {
        HBox box = new HBox(10); box.setAlignment(Pos.TOP_LEFT); box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #ECF0F1; -fx-background-radius: 5;");

        ImageView imgView = new ImageView();
        String imagePath = item.getImage() != null && !item.getImage().isEmpty() ? item.getImage() : "img/temp_icon.png";
        String fullPath = Paths.get("src/resources/" + imagePath).toAbsolutePath().toString();
        try {
            File file = new File(fullPath);
            if (file.exists()) {
                imgView.setImage(new Image("file:" + fullPath));
            } else {
                loadDefaultImage(imgView);
            }
        } catch (Exception e) {
            loadDefaultImage(imgView);
        }
        imgView.setFitWidth(50); imgView.setFitHeight(50); imgView.setPreserveRatio(true);

        VBox infoBox = new VBox(4); infoBox.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(infoBox, Priority.ALWAYS);
        Label lblName = new Label(item.getProductName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a1a1a;");

        HBox drinkTypeBox = new HBox(5);
        Label lblTypeLabel = new Label("Type: "); lblTypeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        ComboBox<String> comboDrinkType = new ComboBox<>(); comboDrinkType.getItems().addAll(item.getAvailableDrinkTypes());
        comboDrinkType.setValue(item.getDrinkType()); comboDrinkType.setStyle("-fx-font-size: 12px; -fx-pref-width: 100;");
        comboDrinkType.setOnAction(e -> {
            String oldKey = findItemKey(item);
            item.setDrinkType(comboDrinkType.getValue());
            if (oldKey != null) { orderItems.remove(oldKey); orderItems.put(item.getProductId() + "-" + item.getDrinkType() + "-" + item.getNote().hashCode(), item); updateOrderDisplay(); }
        });
        drinkTypeBox.getChildren().addAll(lblTypeLabel, comboDrinkType);

        Label lblPrice = new Label("$" + df.format(item.getPrice()));
        lblPrice.setStyle("-fx-font-size: 13px; -fx-text-fill: #667eea; -fx-font-weight: 600;");

        HBox noteBox = new HBox(5);
        Label lblNoteLabel = new Label("Note: "); lblNoteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        TextField txtNote = new TextField(item.getNote()); txtNote.setPromptText("Add note...");
        txtNote.setStyle("-fx-font-size: 12px; -fx-pref-width: 150;");
        txtNote.textProperty().addListener((obs, oldVal, newVal) -> {
            String oldKey = findItemKey(item);
            item.setNote(newVal.trim());
            if (oldKey != null) { orderItems.remove(oldKey); orderItems.put(item.getProductId() + "-" + item.getDrinkType() + "-" + item.getNote().hashCode(), item); updateOrderDisplay(); }
        });
        noteBox.getChildren().addAll(lblNoteLabel, txtNote);

        infoBox.getChildren().addAll(lblName, drinkTypeBox, lblPrice, noteBox);

        VBox quantityBox = new VBox(6); quantityBox.setAlignment(Pos.CENTER);
        Button btnMinus = new Button("-"); btnMinus.setStyle("-fx-background-color: #fee; -fx-text-fill: #e53e3e; -fx-font-size: 16px; -fx-font-weight: bold; -fx-pref-width: 36; -fx-pref-height: 36; -fx-background-radius: 8; -fx-cursor: hand;");
        btnMinus.setOnAction(e -> { if (item.getQuantity() > 1) { item.setQuantity(item.getQuantity() - 1); updateOrderDisplay(); } });
        Label lblQty = new Label(String.valueOf(item.getQuantity())); lblQty.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;"); lblQty.setAlignment(Pos.CENTER); lblQty.setMinWidth(40);
        Button btnPlus = new Button("+"); btnPlus.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #10b981; -fx-font-size: 16px; -fx-font-weight: bold; -fx-pref-width: 36; -fx-pref-height: 36; -fx-background-radius: 8; -fx-cursor: hand;");
        btnPlus.setOnAction(e -> { item.setQuantity(item.getQuantity() + 1); updateOrderDisplay(); });
        quantityBox.getChildren().addAll(btnPlus, lblQty, btnMinus);

        Button btnDelete = new Button(); ImageView deleteIcon = new ImageView();
        try { Image icon = new Image(getClass().getResourceAsStream("/resources/img/trash-can-solid-full.png")); deleteIcon.setImage(icon); deleteIcon.setFitWidth(20); deleteIcon.setFitHeight(20); deleteIcon.setPreserveRatio(true); } catch (Exception e) { System.err.println("Failed to load delete icon"); }
        btnDelete.setGraphic(deleteIcon); btnDelete.setStyle("-fx-background-color: #fee; -fx-pref-width: 40; -fx-pref-height: 40; -fx-background-radius: 10; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> { String key = findItemKey(item); if (key != null) { orderItems.remove(key); updateOrderDisplay(); showSweetAlert(SweetAlert.AlertType.INFO, "Removed", "Item removed"); } });

        box.getChildren().addAll(imgView, infoBox, quantityBox, btnDelete);
        return box;
    }

    private String findItemKey(OrderItem item) {
        for (Map.Entry<String, OrderItem> entry : orderItems.entrySet()) {
            if (entry.getValue() == item) return entry.getKey();
        }
        return null;
    }

    // Payment Processing
    @FXML
    private void handlePayment(ActionEvent event) {
        if (orderItems.isEmpty()) { showSweetAlert(SweetAlert.AlertType.WARNING, "No Items", "No items in order!"); return; }
        if ("dine-in".equals(orderType) && selectedTableId == null) { showSweetAlert(SweetAlert.AlertType.WARNING, "Select Table", "Please select a table!"); return; }
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
                "-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView titleIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/money-bill-wave-solid-full.png"));
            titleIcon.setImage(icon);
            titleIcon.setFitWidth(32);
            titleIcon.setFitHeight(32);
            titleIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load payment icon");
        }

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

        Label dateLabel = new Label(
                "Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568;");

        orderInfo.getChildren().addAll(orderTypeLabel, tableLabel, staffLabel, dateLabel);

        // Items List
        ScrollPane itemsScroll = new ScrollPane();
        itemsScroll.setFitToWidth(true);
        itemsScroll.setPrefHeight(200);
        itemsScroll.setStyle(
                "-fx-background: transparent; -fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");

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
        String selectedStyle = "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-pref-width: 160; -fx-pref-height: 40; -fx-cursor: hand; -fx-padding: 0 10 0 10;";
        String unselectedStyle = "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 10; -fx-pref-width: 160; -fx-pref-height: 40; -fx-cursor: hand; -fx-padding: 0 10 0 10;";

        // Cash Button
        ToggleButton cashBtn = new ToggleButton();
        HBox cashContent = new HBox(5);
        cashContent.setAlignment(Pos.CENTER);
        ImageView cashIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/money-bill-wave-solid-full.png"));
            cashIcon.setImage(icon);
            cashIcon.setFitWidth(20);
            cashIcon.setFitHeight(20);
            cashIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load cash icon");
        }
        cashContent.getChildren().addAll(cashIcon, new Label("Cash"));
        cashBtn.setGraphic(cashContent);

// Card Button
        ToggleButton cardBtn = new ToggleButton();
        HBox cardContent = new HBox(5);
        cardContent.setAlignment(Pos.CENTER);
        ImageView cardIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/money-bill-wave-solid-full.png"));
            cardIcon.setImage(icon);
            cardIcon.setFitWidth(20);
            cardIcon.setFitHeight(20);
            cardIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load card icon");
        }
        cardContent.getChildren().addAll(cardIcon, new Label("Card"));
        cardBtn.setGraphic(cardContent);

// E-Wallet Button
        ToggleButton ewalletBtn = new ToggleButton();
        HBox ewalletContent = new HBox(5);
        ewalletContent.setAlignment(Pos.CENTER);
        ImageView ewalletIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/wallet-solid-full.png"));
            ewalletIcon.setImage(icon);
            ewalletIcon.setFitWidth(20);
            ewalletIcon.setFitHeight(20);
            ewalletIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load e-wallet icon");
        }
        ewalletContent.getChildren().addAll(ewalletIcon, new Label("E-Wallet"));
        ewalletBtn.setGraphic(ewalletContent);

// QR Button
        ToggleButton qrBtn = new ToggleButton();
        HBox qrContent = new HBox(5);
        qrContent.setAlignment(Pos.CENTER);
        ImageView qrIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/qrcode-solid-full.png"));
            qrIcon.setImage(icon);
            qrIcon.setFitWidth(20);
            qrIcon.setFitHeight(20);
            qrIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load QR icon");
        }
        qrContent.getChildren().addAll(qrIcon, new Label("QR"));
        qrBtn.setGraphic(qrContent);

        cashBtn.setToggleGroup(paymentToggleGroup);
        cardBtn.setToggleGroup(paymentToggleGroup);
        ewalletBtn.setToggleGroup(paymentToggleGroup);
        qrBtn.setToggleGroup(paymentToggleGroup);

        if (paymentMethod.equals("cash"))
            cashBtn.setSelected(true);
        else if (paymentMethod.equals("card"))
            cardBtn.setSelected(true);
        else if (paymentMethod.equals("e-wallet"))
            ewalletBtn.setSelected(true);
        else if (paymentMethod.equals("qr-code"))
            qrBtn.setSelected(true);

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
                changeLabel.setStyle(change >= 0 ? "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #10b981;"
                        : "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
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
                if (newVal == cardBtn)
                    paymentMethod = "card";
                else if (newVal == ewalletBtn)
                    paymentMethod = "e-wallet";
                else if (newVal == qrBtn)
                    paymentMethod = "qr-code";
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
                "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-pref-width: 180; -fx-pref-height: 45; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 8, 0, 0, 2);");
        confirmBtn.setOnAction(e -> {
            double paidAmount;
            double discount;
            try {
                discount = txtDiscount.getText().isEmpty() ? 0 : Double.parseDouble(txtDiscount.getText());
                if (discount < 0) {
                    showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Discount cannot be negative!");
                    return;
                }
            } catch (NumberFormatException ex) {
                showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Invalid discount format!");
                return;
            }

            double total = initialTotal - discount;
            if (paymentMethod.equals("cash")) {
                String amountText = txtAmountReceived.getText().trim();
                if (amountText.isEmpty()) {
                    showSweetAlert(SweetAlert.AlertType.WARNING,"warning", "⚠ Please enter amount received!");
                    return;
                }
                try {
                    paidAmount = Double.parseDouble(amountText);
                    if (paidAmount < total) {
                        showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Insufficient amount received!");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Invalid amount format!");
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
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-pref-width: 120; -fx-pref-height: 45; -fx-background-radius: 10; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> hidePaymentOverlay());

        buttonBox.getChildren().addAll(confirmBtn, cancelBtn);

        paymentCard.getChildren().addAll(
                header, sep1, orderInfo, itemsScroll, sep2,
                financialBox, paymentMethodLabel, paymentMethodBox,
                cashPaymentBox, buttonBox);

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
                showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Insufficient ingredients!");
                return;
            }

            int orderId = orderDAO.createOrder(selectedTableId, currentStaffId, orderType);
            if (orderId == -1) {
                conn.rollback();
                showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Failed to create order!");
                return;
            }

            for (OrderItem item : orderItems.values()) {
                boolean success = orderDAO.addOrderItem(
                        orderId, item.getProductId(), item.getDrinkType(),
                        item.getQuantity(), item.getPrice(), item.getNote());
                if (!success) {
                    conn.rollback();
                    showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Failed to add items!");
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
                    orderId, total, vat, paidAmount, changeReturned);

            if (!paymentSuccess) {
                conn.rollback();
                showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Payment failed!");
                return;
            }

            conn.commit();
            showSuccessConfirmation(orderId, total, paidAmount, changeReturned);

            orderItems.clear(); updateOrderDisplay(); selectedTableId = null; lblSelectedTable.setText(""); menuPane.setVisible(false); menuPane.setManaged(false); btnDineIn.setSelected(true);
            loadTables();

        } catch (SQLException e) {
            e.printStackTrace();
            showSweetAlert(SweetAlert.AlertType.WARNING,"error", "✗ Error: " + e.getMessage());
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
                "-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 0);");

        ImageView successIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/circle-check-solid-full.png"));
            successIcon.setImage(icon);
            successIcon.setFitWidth(80);
            successIcon.setFitHeight(80);
            successIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load success icon");
        }

        Label titleLabel = new Label("Payment Successful!");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        Label orderIdLabel = new Label("Order #" + orderId);
        orderIdLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #667eea; -fx-background-color: #e0e7ff; -fx-padding: 8 20; -fx-background-radius: 20;");

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
                "-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-pref-width: 200; -fx-pref-height: 50; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 10, 0, 0, 3);");
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
        if (orderItems.isEmpty()) { showSweetAlert(SweetAlert.AlertType.WARNING, "No Items", "No items to print!"); return; }
        VBox billContent = createBillContent();
        printNode(billContent);
    }
    private VBox createBillContent() {
        VBox bill = new VBox(10);
        bill.setPadding(new Insets(20));
        bill.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER);

        ImageView coffeeIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/mug-hot-solid-full.png"));
            coffeeIcon.setImage(icon);
            coffeeIcon.setFitWidth(24);
            coffeeIcon.setFitHeight(24);
            coffeeIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load coffee icon");
        }

        Label title = new Label("COFFEE SHOP BILL");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        titleBox.getChildren().addAll(coffeeIcon, title);
        bill.getChildren().add(titleBox); // Thay vì add(title)

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
            Label itemLabel = new Label(
                    item.getQuantity() + " x " + item.getProductName() + " (" + item.getDrinkType() + ")");
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
        if (job == null) { showSweetAlert(SweetAlert.AlertType.ERROR, "Printer Error", "No printer available!"); return; }
        if (job.showPrintDialog(btnPrintBill.getScene().getWindow())) {
            try {
                boolean success = job.printPage(node);
                if (success) { job.endJob(); showSweetAlert(SweetAlert.AlertType.SUCCESS, "Success", "Bill printed successfully!"); }
                else showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to print!");
            } catch (Exception e) { showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Printing error: " + e.getMessage()); }
        }
    }

    // Order Management
    @FXML
    private void handleClearOrder(ActionEvent event) {
        if (orderItems.isEmpty()) return;
        showConfirmation("Clear Order?", "This will remove all items from the current order.", this::clearOrder, null);
    }

    private void clearOrder() {
        orderItems.clear(); updateOrderDisplay(); selectedTableId = null; lblSelectedTable.setText(""); menuPane.setVisible(false); menuPane.setManaged(false); btnDineIn.setSelected(true);
        showSweetAlert(SweetAlert.AlertType.INFO, "Cleared", "Order cleared!");
    }

    // Navigation
    @FXML
    private void handleDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/common/sidebar.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            Scene scene = new Scene(root); stage.setScene(scene); stage.setFullScreen(true); stage.setFullScreenExitHint(""); stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); stage.show();
        } catch (IOException e) {
            showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to load dashboard!");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        showConfirmation("Confirm Logout", "Are you sure you want to logout?", () -> {
            try {
                SessionManager.logout();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/auth/login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) btnLogout.getScene().getWindow();
                Scene scene = new Scene(root); stage.setScene(scene); stage.show();
            } catch (IOException ex) {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to logout!");
            }
        }, null);
    }

    // ← SWEETALERT CHUNG
    private void showSweetAlert(SweetAlert.AlertType type, String title, String content) {
        try {
            Pane root = (Pane) mainBorderPane.getScene().getRoot();
            SweetAlert.showAlert(root, type, title, content, null);
        } catch (Exception e) {
            Alert a = new Alert(convertToAlertType(type));
            a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
            a.showAndWait();
        }
    }

    private void showConfirmation(String title, String content, Runnable onConfirm, Runnable onCancel) {
        try {
            Pane root = (Pane) mainBorderPane.getScene().getRoot();
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