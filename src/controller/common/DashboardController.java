package controller.common;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.text.DecimalFormat; // Added import

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import model.Order;
import model.Inventory;
import model.ProductSales;
import utils.SessionManager;
import dao.DatabaseConnection;

public class DashboardController implements Initializable {

    @FXML private Label lblDate;
    @FXML private Button btnRefresh;
    @FXML private Label lblDailyRevenue;
    @FXML private Label lblDailyChange;
    @FXML private Label lblMonthlyRevenue;
    @FXML private Label lblMonthlyChange;
    @FXML private Label lblTodayOrders;
    @FXML private Label lblPaidOrders;
    @FXML private Label lblUnpaidOrders;
    @FXML private Label lblOccupiedTables;
    @FXML private Label lblTableStatus;
    @FXML private VBox alertSection;
    @FXML private Label lblLowStockWarning;
    @FXML private ComboBox<String> cbTimeFilter;
    @FXML private TableView<ProductSales> bestSellingTable;
    @FXML private TableColumn<ProductSales, String> colRank;
    @FXML private TableColumn<ProductSales, String> colProductName;
    @FXML private TableColumn<ProductSales, Integer> colQuantitySold;
    @FXML private TableColumn<ProductSales, Double> colRevenue;
    @FXML private TableView<Order> recentOrdersTable;
    @FXML private TableColumn<Order, Integer> colOrderId;
    @FXML private TableColumn<Order, String> colTable;
    @FXML private TableColumn<Order, String> colStaff;
    @FXML private TableColumn<Order, Double> colTotal;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private Label lblStaffOnline;
    @FXML private Label lblAvgOrderValue;
    @FXML private Label lblPeakHour;

    private Connection connectDb = DatabaseConnection.getConnection();
    private LocalDate currentDate = LocalDate.now();
    private YearMonth currentMonth = YearMonth.now();
    private DecimalFormat df = new DecimalFormat("#.##"); // Initialized DecimalFormat
    private ObservableList<ProductSales> salesList = FXCollections.observableArrayList();
    private ObservableList<Order> orderList = FXCollections.observableArrayList();
    private ObservableList<Inventory> inventoryList = FXCollections.observableArrayList();

    private double previousDailyRevenue = 0;
    private double previousMonthlyRevenue = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateDateLabel();
        setupTableColumns();
        loadDashboardData();
        if (SessionManager.isManager()) {
            alertSection.setVisible(true);
        }
        cbTimeFilter.getSelectionModel().selectFirst(); // Set default selection
    }

    private void updateDateLabel() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("en", "US"));
        lblDate.setText(LocalDateTime.now().format(formatter));
    }

    private void setupTableColumns() {
        colRank.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(String.valueOf(bestSellingTable.getItems().indexOf(cellData.getValue()) + 1)));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colQuantitySold.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        colRevenue.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTable.setCellValueFactory(cellData -> {
            Integer tableId = cellData.getValue().getTableId();
            return new javafx.beans.property.SimpleStringProperty(tableId != null ? tableId.toString() : "Takeaway");
        });
        colStaff.setCellValueFactory(cellData -> {
            Integer staffId = cellData.getValue().getStaffId();
            return new javafx.beans.property.SimpleStringProperty(staffId != null ? "Staff " + staffId : "N/A");
        });
        colTotal.setCellValueFactory(cellData -> {
            Double totalPrice = cellData.getValue().getTotalPrice();
            return new javafx.beans.property.SimpleObjectProperty<>(totalPrice != null ? totalPrice : 0.0);
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadDashboardData() {
        loadDailyRevenue();
        loadMonthlyRevenue();
        loadOrdersCount();
        loadOccupiedTables();
        loadLowStock();
        loadBestSellingProducts();
        loadRecentOrders();
        loadStaffOnline();
        loadAvgOrderValue();
        loadPeakHour();
    }

    private void loadDailyRevenue() {
        String query = "SELECT SUM(total_price) as total FROM payments WHERE DATE(paid_at) = '" + currentDate + "'";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                double revenue = rs.getDouble("total");
                lblDailyRevenue.setText(df.format(revenue) + " $");
                calculateDailyChange(revenue);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblDailyRevenue.setText("0 $");
        }
    }

    private void calculateDailyChange(double currentRevenue) {
        LocalDate yesterday = currentDate.minusDays(1);
        String query = "SELECT SUM(total_price) as total FROM payments WHERE DATE(paid_at) = '" + yesterday + "'";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                double previousRevenue = rs.getDouble("total");
                double change = (previousRevenue > 0) ? ((currentRevenue - previousRevenue) / previousRevenue) * 100 : 0;
                lblDailyChange.setText((change >= 0 ? "↑ +" : "↓ ") + df.format(change) + "% compared to yesterday");
                lblDailyChange.setTextFill(change >= 0 ? javafx.scene.paint.Color.web("#A8F5A8") : javafx.scene.paint.Color.web("#FF6B6B"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMonthlyRevenue() {
        String query = "SELECT SUM(total_price) as total FROM payments WHERE YEAR(paid_at) = " + currentMonth.getYear() + " AND MONTH(paid_at) = " + currentMonth.getMonthValue();
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                double revenue = rs.getDouble("total");
                lblMonthlyRevenue.setText(df.format(revenue) + " $");
                calculateMonthlyChange(revenue);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblMonthlyRevenue.setText("0 $");
        }
    }

    private void calculateMonthlyChange(double currentRevenue) {
        YearMonth previousMonth = currentMonth.minusMonths(1);
        String query = "SELECT SUM(total_price) as total FROM payments WHERE YEAR(paid_at) = " + previousMonth.getYear() + " AND MONTH(paid_at) = " + previousMonth.getMonthValue();
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                double previousRevenue = rs.getDouble("total");
                double change = (previousRevenue > 0) ? ((currentRevenue - previousRevenue) / previousRevenue) * 100 : 0;
                lblMonthlyChange.setText((change >= 0 ? "↑ +" : "↓ ") + df.format(change) + "% compared to last month");
                lblMonthlyChange.setTextFill(change >= 0 ? javafx.scene.paint.Color.web("#A8F5A8") : javafx.scene.paint.Color.web("#FF6B6B"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadOrdersCount() {
        String query = "SELECT COUNT(id) as total, SUM(CASE WHEN status = 'paid' THEN 1 ELSE 0 END) as paid, SUM(CASE WHEN status = 'unpaid' THEN 1 ELSE 0 END) as unpaid FROM orders WHERE DATE(created_at) = '" + currentDate + "'";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                int total = rs.getInt("total");
                int paid = rs.getInt("paid");
                int unpaid = rs.getInt("unpaid");
                lblTodayOrders.setText(String.valueOf(total));
                lblPaidOrders.setText("✓ Paid: " + paid);
                lblUnpaidOrders.setText("⏳ Unpaid: " + unpaid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblTodayOrders.setText("0");
        }
    }

    private void loadOccupiedTables() {
        String query = "SELECT COUNT(id) as occupied, (SELECT COUNT(id) FROM tables) as total FROM tables WHERE status = 'occupied'";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                int occupied = rs.getInt("occupied");
                int total = rs.getInt("total");
                lblOccupiedTables.setText(occupied + "/" + total);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblOccupiedTables.setText("0/30");
        }
    }

    private void loadLowStock() {
        String query = "SELECT COUNT(id) as count FROM inventory WHERE quantity <= min_stock";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                int count = rs.getInt("count");
                lblLowStockWarning.setText("There are " + count + " items running low!");
                alertSection.setVisible(count > 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadBestSellingProducts() {
        salesList.clear();
        String query = "SELECT p.id AS productId, p.name, SUM(oi.quantity) AS quantitySold, SUM(oi.quantity * oi.price) AS totalRevenue " +
                "FROM order_items oi " +
                "JOIN products p ON oi.product_id = p.id " +
                "JOIN orders o ON oi.order_id = o.id " +
                "WHERE DATE(o.created_at) = '" + currentDate + "' " +
                "GROUP BY p.id, p.name " +
                "ORDER BY quantitySold DESC " +
                "LIMIT 10";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                salesList.add(new ProductSales(
                        rs.getInt("productId"),
                        rs.getString("name"),
                        rs.getInt("quantitySold"),
                        rs.getDouble("totalRevenue")
                ));
            }
            bestSellingTable.setItems(salesList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRecentOrders() {
        orderList.clear();
        String query = "SELECT o.id, o.table_id, o.staff_id, o.order_type, o.status, o.created_at, COALESCE(p.total_price, 0) as total_price " +
                "FROM orders o LEFT JOIN payments p ON o.id = p.order_id " +
                "ORDER BY o.created_at DESC LIMIT 10";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                orderList.add(new Order(
                        rs.getInt("id"),
                        rs.getInt("table_id"), // Adjusted to int if constructor expects int
                        rs.getInt("staff_id"),
                        rs.getString("order_type"),
                        rs.getString("status"),
                        rs.getString("created_at"),
                        rs.getDouble("total_price")
                ));
            }
            recentOrdersTable.setItems(orderList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStaffOnline() {
        String query = "SELECT COUNT(id) as online, (SELECT COUNT(id) FROM users) as total FROM users WHERE status = 'online'";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                int online = rs.getInt("online");
                int total = rs.getInt("total");
                lblStaffOnline.setText(online + "/" + total);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblStaffOnline.setText("0/0");
        }
    }

    private void loadAvgOrderValue() {
        String query = "SELECT AVG(total_price) as avg FROM payments WHERE DATE(paid_at) = '" + currentDate + "'";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                double avg = rs.getDouble("avg");
                lblAvgOrderValue.setText(df.format(avg) + " $");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblAvgOrderValue.setText("0 $");
        }
    }

    private void loadPeakHour() {
        String query = "SELECT HOUR(created_at) as hour, COUNT(id) as count FROM orders WHERE DATE(created_at) = '" + currentDate + "' GROUP BY hour ORDER BY count DESC LIMIT 1";
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                int hour = rs.getInt("hour");
                lblPeakHour.setText(hour + ":00 - " + (hour + 1) + ":00");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblPeakHour.setText("--:-- - --:--");
        }
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadDashboardData();
    }

    @FXML
    private void handleTimeFilter(ActionEvent event) {
        String selected = cbTimeFilter.getValue();
        String query = "";
        if ("This Week".equals(selected)) {
            query = "SELECT p.id AS productId, p.name, SUM(oi.quantity) AS quantitySold, SUM(oi.quantity * oi.price) AS totalRevenue " +
                    "FROM order_items oi " +
                    "JOIN products p ON oi.product_id = p.id " +
                    "JOIN orders o ON oi.order_id = o.id " +
                    "WHERE o.created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                    "GROUP BY p.id, p.name " +
                    "ORDER BY quantitySold DESC " +
                    "LIMIT 10";
        } else if ("This Month".equals(selected)) {
            query = "SELECT p.id AS productId, p.name, SUM(oi.quantity) AS quantitySold, SUM(oi.quantity * oi.price) AS totalRevenue " +
                    "FROM order_items oi " +
                    "JOIN products p ON oi.product_id = p.id " +
                    "JOIN orders o ON oi.order_id = o.id " +
                    "WHERE YEAR(o.created_at) = " + currentMonth.getYear() + " AND MONTH(o.created_at) = " + currentMonth.getMonthValue() + " " +
                    "GROUP BY p.id, p.name " +
                    "ORDER BY quantitySold DESC " +
                    "LIMIT 10";
        } else {
            query = "SELECT p.id AS productId, p.name, SUM(oi.quantity) AS quantitySold, SUM(oi.quantity * oi.price) AS totalRevenue " +
                    "FROM order_items oi " +
                    "JOIN products p ON oi.product_id = p.id " +
                    "JOIN orders o ON oi.order_id = o.id " +
                    "WHERE DATE(o.created_at) = '" + currentDate + "' " +
                    "GROUP BY p.id, p.name " +
                    "ORDER BY quantitySold DESC " +
                    "LIMIT 10";
        }
        loadBestSellingProducts(query);
    }

    private void loadBestSellingProducts(String query) {
        salesList.clear();
        try {
            Statement stmt = connectDb.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                salesList.add(new ProductSales(
                        rs.getInt("productId"),
                        rs.getString("name"),
                        rs.getInt("quantitySold"),
                        rs.getDouble("totalRevenue")
                ));
            }
            bestSellingTable.setItems(salesList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewLowStock(ActionEvent event) {
        // Placeholder for viewing low stock details
        System.out.println("View low stock details");
    }
}