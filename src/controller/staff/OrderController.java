package controller.staff;

import dao.DatabaseConnection;
import dao.OrderDAO;
import dao.ProductIngredientDAO;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

public class OrderController implements Initializable {
    @FXML private Label lblStaffName, lblDateTime, lblSelectedTable;
    @FXML private Label lblSubtotal, lblVAT, lblTotal;
    @FXML private ToggleButton btnDineIn, btnTakeaway;
    @FXML private VBox tableSelectionPane, menuPane, orderItemsContainer, notificationPane;
    @FXML private Label lblNotification;
    @FXML private TabPane floorTabPane;
    @FXML private GridPane floor1Grid, floor2Grid, floor3Grid;
    @FXML private GridPane menuGrid;
    @FXML private Button btnPayment, btnClearOrder, btnBack, btnDashboard, btnLogout;

    private ToggleGroup orderTypeGroup;
    private int currentStaffId;
    private Integer selectedTableId = null;
    private String orderType = "dine-in";
    private Map<String, OrderItem> orderItems = new LinkedHashMap<>();
    private DecimalFormat df = new DecimalFormat("#.##");
    private OrderDAO orderDAO = new OrderDAO();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentStaffId = SessionManager.getCurrentUserId();
        lblStaffName.setText("Staff: " + SessionManager.getCurrentUsername());
        orderTypeGroup = new ToggleGroup();
        btnDineIn.setToggleGroup(orderTypeGroup);
        btnTakeaway.setToggleGroup(orderTypeGroup);
        btnDineIn.setSelected(true);
        updateDateTime();
        setupOrderTypeToggle();
        loadTables();
        btnPayment.setDisable(true);
    }

    private void updateDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        lblDateTime.setText(LocalDateTime.now().format(formatter));
    }

    private void setupOrderTypeToggle() {
        orderTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            String selectedStyle = "-fx-background-color: #48bb78; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600;";
            String unselectedStyle = "-fx-background-color: #f7fafc; -fx-text-fill: #718096; -fx-font-size: 14px; -fx-font-weight: 600;";
            if (newVal == btnDineIn) {
                btnDineIn.setStyle(selectedStyle + " -fx-background-radius: 10 0 0 10; -fx-pref-width: 180; -fx-pref-height: 45;");
                btnTakeaway.setStyle(unselectedStyle + " -fx-background-radius: 0 10 10 0; -fx-pref-width: 180; -fx-pref-height: 45;");
                orderType = "dine-in";
                tableSelectionPane.setVisible(true);
                tableSelectionPane.setManaged(true);
                menuPane.setVisible(false);
                menuPane.setManaged(false);
                selectedTableId = null;
                lblSelectedTable.setText("");
            } else if (newVal == btnTakeaway) {
                btnTakeaway.setStyle(selectedStyle + " -fx-background-radius: 0 10 10 0; -fx-pref-width: 180; -fx-pref-height: 45;");
                btnDineIn.setStyle(unselectedStyle + " -fx-background-radius: 10 0 0 10; -fx-pref-width: 180; -fx-pref-height: 45;");
                orderType = "takeaway";
                tableSelectionPane.setVisible(false);
                tableSelectionPane.setManaged(false);
                selectedTableId = null;
                lblSelectedTable.setText("(Takeaway)");
                showMenu();
            }
        });
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
            showNotification("error", "Lỗi: Không tải được danh sách bàn: " + e.getMessage());
        }
    }

    private void displayTables(GridPane grid, List<TableData> tables) {
        if (tables == null) return;
        grid.getChildren().clear();
        int col = 0, row = 0;
        for (TableData table : tables) {
            VBox tableBox = createTableButton(table);
            grid.add(tableBox, col, row);
            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createTableButton(TableData table) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-radius: 5; -fx-cursor: hand; -fx-border-radius: 5; -fx-border-width: 2;");
        box.setPrefSize(100, 100);
        String bgColor = switch (table.status) {
            case "available" -> "#2ECC71";
            case "occupied" -> "#E74C3C";
            case "reserved" -> "#F39C12";
            case "cleaning" -> "#95A5A6";
            default -> "#BDC3C7";
        };
        box.setStyle(box.getStyle() + "-fx-background-color: " + bgColor + "; -fx-border-color: #2C3E50;");
        Label lblName = new Label(table.name);
        lblName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lblSeats = new Label("👥 " + table.seats + " seats");
        lblSeats.setStyle("-fx-font-size: 11px; -fx-text-fill: white;");
        Label lblStatus = new Label(table.status.toUpperCase());
        lblStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: white;");
        box.getChildren().addAll(lblName, lblSeats, lblStatus);
        if (table.status.equals("available")) {
            box.setOnMouseClicked(e -> selectTable(table));
            box.setOnMouseEntered(e -> box.setStyle(box.getStyle() + "-fx-opacity: 0.8;"));
            box.setOnMouseExited(e -> box.setStyle(box.getStyle() + "-fx-opacity: 1.0;"));
        } else {
            box.setOpacity(0.6);
        }
        return box;
    }

    private void selectTable(TableData table) {
        selectedTableId = table.id;
        lblSelectedTable.setText("Table: " + table.name + " (" + table.seats + " seats)");
        showMenu();
    }

    private void showMenu() {
        menuPane.setVisible(true);
        menuPane.setManaged(true);
        loadProducts();
    }

    private void loadProducts() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, name, price, drink_types, image FROM products";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            menuGrid.getChildren().clear();
            int col = 0, row = 0;
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
                VBox productBox = createProductCard(product);
                menuGrid.add(productBox, col, row);
                col++;
                if (col >= 3) {
                    col = 0;
                    row++;
                }
            }
        } catch (SQLException e) {
            showNotification("error", "Lỗi: Không tải được danh sách sản phẩm: " + e.getMessage());
        }
    }

    private VBox createProductCard(ProductData product) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: #BDC3C7; -fx-border-radius: 8; -fx-border-width: 1;");
        box.setPrefSize(150, 200);
        ImageView imgView = new ImageView();
        String imagePath = product.image != null && !product.image.isEmpty() ? product.image : "img/temp_icon.png";
        try {
            Image image = new Image(getClass().getResourceAsStream("/resources/" + imagePath));
            imgView.setImage(image);
        } catch (Exception e) {
            loadDefaultImage(imgView);
        }
        imgView.setFitWidth(80);
        imgView.setFitHeight(80);
        imgView.setPreserveRatio(true);
        Label lblName = new Label(product.name);
        lblName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        lblName.setWrapText(true);
        lblName.setAlignment(Pos.CENTER);
        box.getChildren().addAll(imgView, lblName);
        box.setOnMouseClicked(e -> addToOrder(product, product.drinkTypes.iterator().next(), product.price, ""));
        box.setOnMouseEntered(e -> box.setStyle(box.getStyle() + "-fx-background-color: #ECF0F1;"));
        box.setOnMouseExited(e -> box.setStyle(box.getStyle() + "-fx-background-color: white;"));
        return box;
    }

    private void loadDefaultImage(ImageView imgView) {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/resources/img/temp_icon.png"));
            imgView.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("Lỗi tải ảnh mặc định: " + e.getMessage());
        }
    }

    private void addToOrder(ProductData product, String drinkType, double price, String note) {
        String key = product.id + "-" + drinkType;
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
        for (OrderItem item : orderItems.values()) {
            HBox itemBox = createOrderItemBox(item);
            orderItemsContainer.getChildren().add(itemBox);
            subtotal += item.getPrice() * item.getQuantity();
        }
        double vat = subtotal * 0.1;
        double total = subtotal + vat;
        lblSubtotal.setText("$" + df.format(subtotal));
        lblVAT.setText("$" + df.format(vat));
        lblTotal.setText("$" + df.format(total));
        btnPayment.setDisable(orderItems.isEmpty());
    }

    private HBox createOrderItemBox(OrderItem item) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #ECF0F1; -fx-background-radius: 5;");
        // Image
        ImageView imgView = new ImageView();
        String imagePath = item.getImage() != null && !item.getImage().isEmpty() ? item.getImage() : "img/temp_icon.png";
        try {
            Image image = new Image(getClass().getResourceAsStream("/resources/" + imagePath));
            imgView.setImage(image);
        } catch (Exception e) {
            loadDefaultImage(imgView);
        }
        imgView.setFitWidth(50);
        imgView.setFitHeight(50);
        imgView.setPreserveRatio(true);
        VBox infoBox = new VBox(3);
        Label lblName = new Label(item.getProductName());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label lblPrice = new Label("$" + df.format(item.getPrice()));
        lblPrice.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F8C8D;");
        ComboBox<String> drinkTypeCombo = new ComboBox<>();
        drinkTypeCombo.getItems().addAll(item.getAvailableDrinkTypes());
        drinkTypeCombo.setValue(item.getDrinkType());
        drinkTypeCombo.setStyle("-fx-font-size: 12px; -fx-pref-width: 100px;");
        drinkTypeCombo.setOnAction(e -> {
            String newType = drinkTypeCombo.getValue();
            String oldKey = item.getProductId() + "-" + item.getDrinkType();
            item.setDrinkType(newType);
            String newKey = item.getProductId() + "-" + newType;
            orderItems.remove(oldKey);
            orderItems.put(newKey, item);
            updateOrderDisplay();
        });
        TextField noteField = new TextField(item.getNote());
        noteField.setPromptText("Thêm ghi chú...");
        noteField.setStyle("-fx-font-size: 12px; -fx-pref-width: 150px;");
        noteField.textProperty().addListener((obs, oldVal, newVal) -> {
            item.setNote(newVal.trim());
        });
        infoBox.getChildren().addAll(lblName, lblPrice, drinkTypeCombo, noteField);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox quantityBox = new VBox(5);
        quantityBox.setAlignment(Pos.CENTER);
        Button btnMinus = new Button("-");
        btnMinus.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-pref-width: 30; -fx-pref-height: 30;");
        btnMinus.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                updateOrderDisplay();
            }
        });
        Label lblQty = new Label(String.valueOf(item.getQuantity()));
        lblQty.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-min-width: 30; -fx-alignment: center;");
        lblQty.setAlignment(Pos.CENTER);
        Button btnPlus = new Button("+");
        btnPlus.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-pref-width: 30; -fx-pref-height: 30;");
        btnPlus.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            updateOrderDisplay();
        });
        quantityBox.getChildren().addAll(btnMinus, lblQty, btnPlus);
        Button btnDelete = new Button("🗑");
        btnDelete.setStyle("-fx-background-color: #95A5A6; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 35; -fx-pref-height: 30;");
        btnDelete.setOnAction(e -> {
            String key = item.getProductId() + "-" + item.getDrinkType();
            orderItems.remove(key);
            updateOrderDisplay();
        });
        box.getChildren().addAll(imgView, infoBox, spacer, quantityBox, btnDelete);
        return box;
    }

    @FXML
    private void handlePayment(ActionEvent event) {
        if (orderItems.isEmpty()) {
            showNotification("warning", "Cảnh báo: Không có món nào trong đơn hàng!");
            return;
        }
        if (orderType.equals("dine-in") && selectedTableId == null) {
            showNotification("warning", "Cảnh báo: Vui lòng chọn bàn!");
            return;
        }
        ProductIngredientDAO piDAO = new ProductIngredientDAO();
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            int orderId = orderDAO.createOrder(selectedTableId, currentStaffId, orderType);
            if (orderId == -1) {
                conn.rollback();
                showNotification("error", "Lỗi: Không tạo được đơn hàng!");
                return;
            }
            // Collect ingredients needed
            Map<Integer, Double> totalIngredientsNeeded = new HashMap<>();
            for (OrderItem item : orderItems.values()) {
                Map<Integer, Double> ingNeeded = piDAO.getIngredientsByProduct(item.getProductId());
                for (Map.Entry<Integer, Double> entry : ingNeeded.entrySet()) {
                    totalIngredientsNeeded.merge(entry.getKey(), entry.getValue() * item.getQuantity(), Double::sum);
                }
            }
            // Check and consume ingredients
            boolean sufficient = piDAO.consumeIngredients(conn, totalIngredientsNeeded);
            if (!sufficient) {
                conn.rollback();
                showNotification("error", "Lỗi: Nguyên liệu không đủ để thực hiện đơn hàng!");
                return;
            }
            // Add items to order_items
            for (OrderItem item : orderItems.values()) {
                boolean success = orderDAO.addOrderItem(orderId, item.getProductId(), item.getDrinkType(), item.getQuantity(), item.getPrice(), item.getNote());
                if (!success) {
                    conn.rollback();
                    showNotification("error", "Lỗi: Không thêm được món vào đơn hàng!");
                    return;
                }
            }
            if (selectedTableId != null) {
                String updateTable = "UPDATE tables SET status = 'occupied' WHERE id = ?";
                try (PreparedStatement tableStmt = conn.prepareStatement(updateTable)) {
                    tableStmt.setInt(1, selectedTableId);
                    tableStmt.executeUpdate();
                }
            }
            double subtotal = orderDAO.calculateOrderTotal(orderId);
            double vat = subtotal * 0.1;
            double total = subtotal + vat;
            // Assume amount_received is total for simplicity; in real app, prompt for payment
            double amountReceived = total; // Placeholder, replace with actual input
            double changeReturned = amountReceived - total;
            boolean paymentSuccess = orderDAO.processPayment(orderId, total, vat, amountReceived, changeReturned);
            if (!paymentSuccess) {
                conn.rollback();
                showNotification("error", "Lỗi: Không xử lý được thanh toán!");
                return;
            }
            conn.commit();
            showNotification("success", "Thành công: Đơn hàng đã được tạo và thanh toán! Mã đơn hàng: " + orderId);
            clearOrder();
            loadTables();
        } catch (SQLException e) {
            e.printStackTrace();
            showNotification("error", "Lỗi: Không tạo được đơn hàng: " + e.getMessage());
        }
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/common/sidebar.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint(""); // Hide "Press ESC..." hint
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // Disable ESC key
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/auth/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showNotification("error", "Lỗi: Không thể tải màn hình đăng nhập!");
        }
    }

    @FXML
    private void handleClearOrder(ActionEvent event) {
        clearOrder();
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
    private void handleBack(ActionEvent event) {
        System.out.println("Quay lại menu chính");
    }

    private void showNotification(String type, String message) {
        String bgColor;
        switch (type.toLowerCase()) {
            case "success":
                bgColor = "#48bb78";
                break;
            case "error":
                bgColor = "#e53e3e";
                break;
            case "warning":
                bgColor = "#f6ad55";
                break;
            default:
                bgColor = "#4a5568";
        }
        lblNotification.setText(message);
        lblNotification.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        notificationPane.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: #cbd5e0; -fx-border-radius: 10; -fx-border-width: 2;");
        notificationPane.setVisible(true);
        notificationPane.setManaged(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            notificationPane.setVisible(false);
            notificationPane.setManaged(false);
        });
        pause.play();
    }

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

    // Update OrderItem class to include additional fields
    class OrderItem {
        private int orderId;
        private int tableId;
        private int productId;
        private String drinkType;
        private int quantity;
        private double price;
        private String note;
        private String productName;
        private Set<String> availableDrinkTypes;
        private String image;

        public OrderItem(int orderId, int tableId, int productId, String drinkType, int quantity, double price, String note) {
            this.orderId = orderId;
            this.tableId = tableId;
            this.productId = productId;
            this.drinkType = drinkType;
            this.quantity = quantity;
            this.price = price;
            this.note = note != null ? note : "";
            this.availableDrinkTypes = new HashSet<>();
            this.image = "img/temp_icon.png"; // Default image
        }

        // Getters and setters
        public int getOrderId() { return orderId; }
        public void setOrderId(int orderId) { this.orderId = orderId; }
        public int getTableId() { return tableId; }
        public void setTableId(int tableId) { this.tableId = tableId; }
        public int getProductId() { return productId; }
        public void setProductId(int productId) { this.productId = productId; }
        public String getDrinkType() { return drinkType; }
        public void setDrinkType(String drinkType) { this.drinkType = drinkType; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Set<String> getAvailableDrinkTypes() { return availableDrinkTypes; }
        public void setAvailableDrinkTypes(Set<String> availableDrinkTypes) { this.availableDrinkTypes = availableDrinkTypes; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }
}