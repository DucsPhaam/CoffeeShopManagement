package controller.manager;

import dao.*;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import model.*;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.print.PrinterJob;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import utils.SweetAlert;
import javafx.application.Platform;

public class ReportController {

    @FXML private Label lblCurrentDate;
    @FXML private ComboBox<String> cmbReportType;
    @FXML private ComboBox<String> cmbTimePeriod;
    @FXML private DatePicker dpFromDate;
    @FXML private DatePicker dpToDate;
    @FXML private Label lblFromDate;
    @FXML private Label lblToDate;

    // Summary Cards
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblRevenueChange;
    @FXML private Label lblTotalOrders;
    @FXML private Label lblOrdersChange;
    @FXML private Label lblNetProfit;
    @FXML private Label lblProfitMargin;
    @FXML private Label lblAvgOrderValue;
    @FXML private Label lblAvgChange;

    // Charts
    @FXML private Label lblChartTitle;
    @FXML private BarChart<String, Number> mainChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private PieChart pieChart;

    // Table
    @FXML private Label lblTableTitle;
    @FXML private TableView<Map<String, Object>> tableReport;
    @FXML private TextField txtTableSearch;
    @FXML private Label lblTableSummary;

    // Loading
    @FXML private StackPane loadingOverlay;
    @FXML private Button btnGenerateReport;

    private ObservableList<Map<String, Object>> reportData;
    private ObservableList<Map<String, Object>> filteredData;
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
    private DecimalFormat percentFormat = new DecimalFormat("#0.0%");

    // Lưu dữ liệu kỳ trước để tính toán % thay đổi
    private double previousRevenue = 0;
    private int previousOrders = 0;
    private double previousAvgOrder = 0;

    @FXML
    public void initialize() {
        reportData = FXCollections.observableArrayList();
        filteredData = FXCollections.observableArrayList();

        updateCurrentDate();
        setupDefaults();

        // Cải thiện hiển thị bar chart
        xAxis.setTickLabelRotation(45);
        mainChart.setBarGap(3);
        mainChart.setCategoryGap(20);

        // Defer report generation until after scene is set
        Platform.runLater(this::handleGenerateReport);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        // Convert AlertType to SweetAlert.AlertType
        SweetAlert.AlertType sweetType;
        switch (type) {
            case INFORMATION:
                sweetType = SweetAlert.AlertType.SUCCESS;
                break;
            case ERROR:
                sweetType = SweetAlert.AlertType.ERROR;
                break;
            case WARNING:
                sweetType = SweetAlert.AlertType.WARNING;
                break;
            case CONFIRMATION:
                sweetType = SweetAlert.AlertType.QUESTION;
                break;
            default:
                sweetType = SweetAlert.AlertType.INFO;
        }

        showSweetAlert(sweetType, title, message);
    }

    private void showSweetAlert(SweetAlert.AlertType type, String title, String message) {
        try {
            if (tableReport.getScene() == null) {
                throw new NullPointerException("Scene is null - falling back to standard alert");
            }
            Pane rootPane = (Pane) tableReport.getScene().getRoot();
            // Removed wrapping logic to avoid changing root during event handling
            SweetAlert.showAlert(rootPane, type, title, message);
        } catch (Exception e) {
            System.err.println("Failed to show SweetAlert: " + e.getMessage());
            // Fallback to regular alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    private void updateCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        lblCurrentDate.setText(sdf.format(new Date()));
    }

    private void setupDefaults() {
        cmbReportType.setValue("Revenue by Date");
        cmbTimePeriod.setValue("This Month");
    }

    @FXML
    private void handleReportTypeChange() {
        // UI adjustments based on report type
    }

    @FXML
    private void handleTimePeriodChange() {
        String period = cmbTimePeriod.getValue();
        boolean isCustom = "Custom Range".equals(period);

        lblFromDate.setVisible(isCustom);
        lblFromDate.setManaged(isCustom);
        dpFromDate.setVisible(isCustom);
        dpFromDate.setManaged(isCustom);

        lblToDate.setVisible(isCustom);
        lblToDate.setManaged(isCustom);
        dpToDate.setVisible(isCustom);
        dpToDate.setManaged(isCustom);
    }

    @FXML
    private void handleGenerateReport() {
        showLoading(true);

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            generateReport();
            showLoading(false);
        }));
        timeline.play();
    }

    private void generateReport() {
        String reportType = cmbReportType.getValue();

        switch (reportType) {
            case "Revenue by Date":
                generateRevenueByDateReport();
                break;
            case "Revenue by Staff":
                generateRevenueByStaffReport();
                break;
            case "Product Performance":
                generateProductPerformanceReport();
                break;
            case "Profit Analysis":
                generateProfitAnalysisReport();
                break;
            case "Inventory Usage":
                generateInventoryUsageReport();
                break;
            default:
                generateRevenueByDateReport();
        }
    }

    private void generateRevenueByDateReport() {
        reportData.clear();
        tableReport.getColumns().clear();

        DateRange dateRange = getDateRange();
        DateRange previousDateRange = getPreviousDateRange();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Lấy dữ liệu kỳ hiện tại
            String sql = "SELECT DATE(o.created_at) as order_date, " +
                    "COUNT(DISTINCT o.id) as total_orders, " +
                    "SUM(p.total_price) as revenue, " +
                    "SUM(p.total_price - p.vat) as revenue_no_vat " +
                    "FROM orders o " +
                    "JOIN payments p ON o.id = p.order_id " +
                    "WHERE o.created_at BETWEEN ? AND ? " +
                    "AND o.status = 'paid' " +
                    "GROUP BY DATE(o.created_at) " +
                    "ORDER BY order_date DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(dateRange.fromDate.getTime()));
            ps.setTimestamp(2, new Timestamp(dateRange.toDate.getTime()));

            ResultSet rs = ps.executeQuery();

            double totalRevenue = 0;
            int totalOrders = 0;

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("date", rs.getDate("order_date"));
                row.put("orders", rs.getInt("total_orders"));
                row.put("revenue", rs.getDouble("revenue"));
                row.put("revenue_no_vat", rs.getDouble("revenue_no_vat"));

                reportData.add(row);
                totalRevenue += rs.getDouble("revenue");
                totalOrders += rs.getInt("total_orders");
            }

            // Lấy dữ liệu kỳ trước
            calculatePreviousPeriodData(conn, previousDateRange);

            updateSummaryCards(totalRevenue, totalOrders);
            setupRevenueByDateTable();
            updateRevenueChart();
            updateRevenuePieChart();

            filteredData.setAll(reportData);
            tableReport.setItems(filteredData);
            lblTableSummary.setText("Showing " + filteredData.size() + " records");

            lblChartTitle.setText("Revenue Trend");
            lblTableTitle.setText("Revenue by Date Details");

        } catch (SQLException e) {
            e.printStackTrace();
            showSweetAlert(SweetAlert.AlertType.ERROR, "Error", "Failed to generate report: " + e.getMessage());
        }
    }

    private void generateRevenueByStaffReport() {
        reportData.clear();
        tableReport.getColumns().clear();

        DateRange dateRange = getDateRange();
        DateRange previousDateRange = getPreviousDateRange();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT u.id, u.username, u.role, " +
                    "COUNT(DISTINCT o.id) as total_orders, " +
                    "SUM(p.total_price) as revenue, " +
                    "AVG(p.total_price) as avg_order_value " +
                    "FROM users u " +
                    "LEFT JOIN orders o ON u.id = o.staff_id " +
                    "LEFT JOIN payments p ON o.id = p.order_id " +
                    "WHERE o.created_at BETWEEN ? AND ? " +
                    "AND o.status = 'paid' " +
                    "GROUP BY u.id, u.username, u.role " +
                    "ORDER BY revenue DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(dateRange.fromDate.getTime()));
            ps.setTimestamp(2, new Timestamp(dateRange.toDate.getTime()));

            ResultSet rs = ps.executeQuery();

            double totalRevenue = 0;
            int totalOrders = 0;

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("staff_id", rs.getInt("id"));
                row.put("username", rs.getString("username"));
                row.put("role", rs.getString("role"));
                row.put("orders", rs.getInt("total_orders"));
                row.put("revenue", rs.getDouble("revenue"));
                row.put("avg_order", rs.getDouble("avg_order_value"));

                reportData.add(row);
                totalRevenue += rs.getDouble("revenue");
                totalOrders += rs.getInt("total_orders");
            }

            calculatePreviousPeriodData(conn, previousDateRange);

            updateSummaryCards(totalRevenue, totalOrders);
            setupRevenueByStaffTable();
            updateStaffRevenueChart();
            updateStaffPieChart();

            filteredData.setAll(reportData);
            tableReport.setItems(filteredData);
            lblTableSummary.setText("Showing " + filteredData.size() + " staff members");

            lblChartTitle.setText("Revenue by Staff");
            lblTableTitle.setText("Staff Performance Details");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate report: " + e.getMessage());
        }
    }

    private void generateProductPerformanceReport() {
        reportData.clear();
        tableReport.getColumns().clear();

        DateRange dateRange = getDateRange();
        DateRange previousDateRange = getPreviousDateRange();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.id, p.name, p.price, " +
                    "SUM(oi.quantity) as total_sold, " +
                    "SUM(oi.quantity * oi.price) as revenue, " +
                    "COUNT(DISTINCT oi.order_id) as num_orders " +
                    "FROM products p " +
                    "JOIN order_items oi ON p.id = oi.product_id " +
                    "JOIN orders o ON oi.order_id = o.id " +
                    "WHERE o.created_at BETWEEN ? AND ? " +
                    "AND o.status = 'paid' " +
                    "GROUP BY p.id, p.name, p.price " +
                    "ORDER BY revenue DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(dateRange.fromDate.getTime()));
            ps.setTimestamp(2, new Timestamp(dateRange.toDate.getTime()));

            ResultSet rs = ps.executeQuery();

            double totalRevenue = 0;
            int totalSold = 0;
            int totalOrders = 0;

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("product_id", rs.getInt("id"));
                row.put("product_name", rs.getString("name"));
                row.put("unit_price", rs.getDouble("price"));
                row.put("quantity_sold", rs.getInt("total_sold"));
                row.put("revenue", rs.getDouble("revenue"));
                row.put("num_orders", rs.getInt("num_orders"));

                reportData.add(row);
                totalRevenue += rs.getDouble("revenue");
                totalSold += rs.getInt("total_sold");
                totalOrders += rs.getInt("num_orders");
            }

            calculatePreviousPeriodData(conn, previousDateRange);

            updateSummaryCards(totalRevenue, totalOrders);
            setupProductPerformanceTable();
            updateProductChart();
            updateProductPieChart();

            filteredData.setAll(reportData);
            tableReport.setItems(filteredData);
            lblTableSummary.setText("Showing " + filteredData.size() + " products | Total Sold: " + totalSold);

            lblChartTitle.setText("Product Performance");
            lblTableTitle.setText("Product Sales Details");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate report: " + e.getMessage());
        }
    }

    private void generateProfitAnalysisReport() {
        reportData.clear();
        tableReport.getColumns().clear();

        DateRange dateRange = getDateRange();
        DateRange previousDateRange = getPreviousDateRange();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT DATE(o.created_at) as order_date, " +
                    "SUM(p.total_price) as revenue, " +
                    "SUM(p.vat) as total_vat, " +
                    "COUNT(DISTINCT o.id) as orders " +
                    "FROM orders o " +
                    "JOIN payments p ON o.id = p.order_id " +
                    "WHERE o.created_at BETWEEN ? AND ? " +
                    "AND o.status = 'paid' " +
                    "GROUP BY DATE(o.created_at) " +
                    "ORDER BY order_date DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(dateRange.fromDate.getTime()));
            ps.setTimestamp(2, new Timestamp(dateRange.toDate.getTime()));

            ResultSet rs = ps.executeQuery();

            double totalRevenue = 0;
            double totalCost = 0;
            int totalOrders = 0;

            while (rs.next()) {
                double revenue = rs.getDouble("revenue");
                double estimatedCost = revenue * 0.35;
                double profit = revenue - estimatedCost;

                Map<String, Object> row = new HashMap<>();
                row.put("date", rs.getDate("order_date"));
                row.put("revenue", revenue);
                row.put("cost", estimatedCost);
                row.put("profit", profit);
                row.put("profit_margin", profit / revenue);
                row.put("orders", rs.getInt("orders"));

                reportData.add(row);
                totalRevenue += revenue;
                totalCost += estimatedCost;
                totalOrders += rs.getInt("orders");
            }

            calculatePreviousPeriodData(conn, previousDateRange);

            double netProfit = totalRevenue - totalCost;
            lblTotalRevenue.setText(currencyFormat.format(totalRevenue));
            lblTotalOrders.setText(String.valueOf(totalOrders));
            lblNetProfit.setText(currencyFormat.format(netProfit));
            lblProfitMargin.setText("Margin: " + percentFormat.format(netProfit / totalRevenue));
            lblAvgOrderValue.setText(currencyFormat.format(totalOrders > 0 ? totalRevenue / totalOrders : 0));

            updateSummaryCardChanges(totalRevenue, totalOrders);

            setupProfitAnalysisTable();
            updateProfitChart();
            updateProfitPieChart();

            filteredData.setAll(reportData);
            tableReport.setItems(filteredData);
            lblTableSummary.setText("Showing " + filteredData.size() + " records");

            lblChartTitle.setText("Profit Analysis");
            lblTableTitle.setText("Profit Details by Date");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate report: " + e.getMessage());
        }
    }

    private void generateInventoryUsageReport() {
        reportData.clear();
        tableReport.getColumns().clear();

        DateRange dateRange = getDateRange();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT i.id, i.name, i.unit, i.cost_per_unit, i.quantity as current_stock, " +
                    "SUM(pi.quantity * oi.quantity) as total_used, " +
                    "SUM(pi.quantity * oi.quantity * i.cost_per_unit) as total_cost " +
                    "FROM inventory i " +
                    "JOIN product_ingredients pi ON i.id = pi.inventory_id " +
                    "JOIN order_items oi ON pi.product_id = oi.product_id " +
                    "JOIN orders o ON oi.order_id = o.id " +
                    "WHERE o.created_at BETWEEN ? AND ? " +
                    "AND o.status = 'paid' " +
                    "GROUP BY i.id, i.name, i.unit, i.cost_per_unit, i.quantity " +
                    "ORDER BY total_cost DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(dateRange.fromDate.getTime()));
            ps.setTimestamp(2, new Timestamp(dateRange.toDate.getTime()));

            ResultSet rs = ps.executeQuery();

            double totalCost = 0;

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("inventory_id", rs.getInt("id"));
                row.put("item_name", rs.getString("name"));
                row.put("unit", rs.getString("unit"));
                row.put("cost_per_unit", rs.getDouble("cost_per_unit"));
                row.put("current_stock", rs.getDouble("current_stock"));
                row.put("total_used", rs.getDouble("total_used"));
                row.put("total_cost", rs.getDouble("total_cost"));

                reportData.add(row);
                totalCost += rs.getDouble("total_cost");
            }

            lblTotalRevenue.setText("N/A");
            lblTotalOrders.setText(String.valueOf(reportData.size()));
            lblNetProfit.setText(currencyFormat.format(-totalCost));
            lblProfitMargin.setText("Total Cost");
            lblAvgOrderValue.setText(currencyFormat.format(reportData.size() > 0 ? totalCost / reportData.size() : 0));

            // Ẩn các change labels cho inventory report
            lblRevenueChange.setVisible(false);
            lblOrdersChange.setVisible(false);
            lblAvgChange.setVisible(false);

            setupInventoryUsageTable();
            updateInventoryChart();
            updateInventoryPieChart();

            filteredData.setAll(reportData);
            tableReport.setItems(filteredData);
            lblTableSummary.setText("Showing " + filteredData.size() + " inventory items");

            lblChartTitle.setText("Inventory Usage");
            lblTableTitle.setText("Inventory Consumption Details");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to generate report: " + e.getMessage());
        }
    }

    // Tính toán dữ liệu kỳ trước
    private void calculatePreviousPeriodData(Connection conn, DateRange previousDateRange) {
        try {
            String sql = "SELECT " +
                    "SUM(p.total_price) as revenue, " +
                    "COUNT(DISTINCT o.id) as orders " +
                    "FROM orders o " +
                    "JOIN payments p ON o.id = p.order_id " +
                    "WHERE o.created_at BETWEEN ? AND ? " +
                    "AND o.status = 'paid'";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(previousDateRange.fromDate.getTime()));
            ps.setTimestamp(2, new Timestamp(previousDateRange.toDate.getTime()));

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                previousRevenue = rs.getDouble("revenue");
                previousOrders = rs.getInt("orders");
                previousAvgOrder = previousOrders > 0 ? previousRevenue / previousOrders : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Table Setup Methods
    private void setupRevenueByDateTable() {
        TableColumn<Map<String, Object>, Date> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Date) data.getValue().get("date")));
        colDate.setCellFactory(col -> new TableCell<Map<String, Object>, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : new SimpleDateFormat("MMM dd, yyyy").format(item));
            }
        });

        TableColumn<Map<String, Object>, Integer> colOrders = new TableColumn<>("Orders");
        colOrders.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Integer) data.getValue().get("orders")));

        TableColumn<Map<String, Object>, Double> colRevenue = new TableColumn<>("Revenue");
        colRevenue.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("revenue")));
        colRevenue.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        tableReport.getColumns().addAll(colDate, colOrders, colRevenue);
    }

    private void setupRevenueByStaffTable() {
        TableColumn<Map<String, Object>, String> colUsername = new TableColumn<>("Staff");
        colUsername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("username")));

        TableColumn<Map<String, Object>, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("role")));

        TableColumn<Map<String, Object>, Integer> colOrders = new TableColumn<>("Orders");
        colOrders.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Integer) data.getValue().get("orders")));

        TableColumn<Map<String, Object>, Double> colRevenue = new TableColumn<>("Revenue");
        colRevenue.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("revenue")));
        colRevenue.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        TableColumn<Map<String, Object>, Double> colAvg = new TableColumn<>("Avg Order");
        colAvg.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("avg_order")));
        colAvg.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        tableReport.getColumns().addAll(colUsername, colRole, colOrders, colRevenue, colAvg);
    }

    private void setupProductPerformanceTable() {
        TableColumn<Map<String, Object>, String> colProduct = new TableColumn<>("Product");
        colProduct.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("product_name")));

        TableColumn<Map<String, Object>, Double> colPrice = new TableColumn<>("Unit Price");
        colPrice.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("unit_price")));
        colPrice.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        TableColumn<Map<String, Object>, Integer> colSold = new TableColumn<>("Qty Sold");
        colSold.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Integer) data.getValue().get("quantity_sold")));

        TableColumn<Map<String, Object>, Double> colRevenue = new TableColumn<>("Revenue");
        colRevenue.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("revenue")));
        colRevenue.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        TableColumn<Map<String, Object>, Integer> colOrders = new TableColumn<>("Orders");
        colOrders.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Integer) data.getValue().get("num_orders")));

        tableReport.getColumns().addAll(colProduct, colPrice, colSold, colRevenue, colOrders);
    }

    private void setupProfitAnalysisTable() {
        TableColumn<Map<String, Object>, Date> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Date) data.getValue().get("date")));
        colDate.setCellFactory(col -> new TableCell<Map<String, Object>, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : new SimpleDateFormat("MMM dd, yyyy").format(item));
            }
        });

        TableColumn<Map<String, Object>, Double> colRevenue = new TableColumn<>("Revenue");
        colRevenue.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("revenue")));
        colRevenue.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        TableColumn<Map<String, Object>, Double> colCost = new TableColumn<>("Cost");
        colCost.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("cost")));
        colCost.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        TableColumn<Map<String, Object>, Double> colProfit = new TableColumn<>("Profit");
        colProfit.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("profit")));
        colProfit.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        TableColumn<Map<String, Object>, Double> colMargin = new TableColumn<>("Margin %");
        colMargin.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("profit_margin")));
        colMargin.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : percentFormat.format(item));
            }
        });

        tableReport.getColumns().addAll(colDate, colRevenue, colCost, colProfit, colMargin);
    }

    private void setupInventoryUsageTable() {
        TableColumn<Map<String, Object>, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("item_name")));

        TableColumn<Map<String, Object>, String> colUnit = new TableColumn<>("Unit");
        colUnit.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("unit")));

        TableColumn<Map<String, Object>, Double> colUsed = new TableColumn<>("Used");
        colUsed.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("total_used")));
        colUsed.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });

        TableColumn<Map<String, Object>, Double> colStock = new TableColumn<>("Current Stock");
        colStock.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("current_stock")));
        colStock.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });

        TableColumn<Map<String, Object>, Double> colCost = new TableColumn<>("Total Cost");
        colCost.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>((Double) data.getValue().get("total_cost")));
        colCost.setCellFactory(col -> new TableCell<Map<String, Object>, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
            }
        });

        tableReport.getColumns().addAll(colItem, colUnit, colUsed, colStock, colCost);
    }

    // Chart Update Methods - SỬA ĐỂ TRÁNH LABEL BỊ TRÙNG
    private void updateRevenueChart() {
        mainChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Revenue");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");

        // Lấy tối đa 15 ngày gần nhất để tránh chart quá đông
        int maxItems = Math.min(15, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String date = sdf.format((Date) row.get("date"));
            double revenue = (Double) row.get("revenue");
            series.getData().add(new XYChart.Data<>(date, revenue));
        }

        mainChart.getData().add(series);
        yAxis.setLabel("Revenue ($)");

        // Rotate labels để tránh bị chồng lên nhau
        xAxis.setTickLabelRotation(45);
    }

    private void updateRevenuePieChart() {
        pieChart.getData().clear();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
        // Giới hạn 8 items cho pie chart
        int maxItems = Math.min(8, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String date = sdf.format((Date) row.get("date"));
            double revenue = (Double) row.get("revenue");
            pieChart.getData().add(new PieChart.Data(date, revenue));
        }
    }

    private void updateStaffRevenueChart() {
        mainChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Staff Revenue");

        int maxItems = Math.min(10, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String staff = (String) row.get("username");
            double revenue = (Double) row.get("revenue");
            series.getData().add(new XYChart.Data<>(staff, revenue));
        }

        mainChart.getData().add(series);
        yAxis.setLabel("Revenue ($)");
        xAxis.setTickLabelRotation(45);
    }

    private void updateStaffPieChart() {
        pieChart.getData().clear();

        int maxItems = Math.min(8, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String staff = (String) row.get("username");
            double revenue = (Double) row.get("revenue");
            pieChart.getData().add(new PieChart.Data(staff, revenue));
        }
    }

    private void updateProductChart() {
        mainChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Product Sales");

        int maxItems = Math.min(10, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String product = (String) row.get("product_name");
            // Rút ngắn tên sản phẩm nếu quá dài
            if (product.length() > 15) {
                product = product.substring(0, 12) + "...";
            }
            double revenue = (Double) row.get("revenue");
            series.getData().add(new XYChart.Data<>(product, revenue));
        }

        mainChart.getData().add(series);
        yAxis.setLabel("Revenue ($)");
        xAxis.setTickLabelRotation(45);
    }

    private void updateProductPieChart() {
        pieChart.getData().clear();

        int maxItems = Math.min(8, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String product = (String) row.get("product_name");
            double revenue = (Double) row.get("revenue");
            pieChart.getData().add(new PieChart.Data(product, revenue));
        }
    }

    private void updateProfitChart() {
        mainChart.getData().clear();
        XYChart.Series<String, Number> revenueSeriesSeries = new XYChart.Series<>();
        revenueSeriesSeries.setName("Revenue");

        XYChart.Series<String, Number> profitSeries = new XYChart.Series<>();
        profitSeries.setName("Profit");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
        int maxItems = Math.min(15, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String date = sdf.format((Date) row.get("date"));
            double revenue = (Double) row.get("revenue");
            double profit = (Double) row.get("profit");

            revenueSeriesSeries.getData().add(new XYChart.Data<>(date, revenue));
            profitSeries.getData().add(new XYChart.Data<>(date, profit));
        }

        mainChart.getData().addAll(revenueSeriesSeries, profitSeries);
        yAxis.setLabel("Amount ($)");
        xAxis.setTickLabelRotation(45);
    }

    private void updateProfitPieChart() {
        pieChart.getData().clear();

        double totalRevenue = 0;
        double totalCost = 0;

        for (Map<String, Object> row : reportData) {
            totalRevenue += (Double) row.get("revenue");
            totalCost += (Double) row.get("cost");
        }

        double profit = totalRevenue - totalCost;

        pieChart.getData().add(new PieChart.Data("Revenue", totalRevenue));
        pieChart.getData().add(new PieChart.Data("Cost", totalCost));
        pieChart.getData().add(new PieChart.Data("Profit", profit));
    }

    private void updateInventoryChart() {
        mainChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Inventory Usage");

        int maxItems = Math.min(10, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String item = (String) row.get("item_name");
            if (item.length() > 15) {
                item = item.substring(0, 12) + "...";
            }
            double used = (Double) row.get("total_used");
            series.getData().add(new XYChart.Data<>(item, used));
        }

        mainChart.getData().add(series);
        yAxis.setLabel("Quantity Used");
        xAxis.setTickLabelRotation(45);
    }

    private void updateInventoryPieChart() {
        pieChart.getData().clear();

        int maxItems = Math.min(8, reportData.size());
        for (int i = 0; i < maxItems; i++) {
            Map<String, Object> row = reportData.get(i);
            String item = (String) row.get("item_name");
            double cost = (Double) row.get("total_cost");
            pieChart.getData().add(new PieChart.Data(item, cost));
        }
    }

    // Helper Methods - CẢI THIỆN SUMMARY CARDS
    private void updateSummaryCards(double totalRevenue, int totalOrders) {
        lblTotalRevenue.setText(currencyFormat.format(totalRevenue));
        lblTotalOrders.setText(String.valueOf(totalOrders));
        lblAvgOrderValue.setText(currencyFormat.format(totalOrders > 0 ? totalRevenue / totalOrders : 0));

        double estimatedCosts = totalRevenue * 0.35;
        double profit = totalRevenue - estimatedCosts;
        lblNetProfit.setText(currencyFormat.format(profit));
        lblProfitMargin.setText("Margin: " + percentFormat.format(totalRevenue > 0 ? profit / totalRevenue : 0));

        updateSummaryCardChanges(totalRevenue, totalOrders);
    }

    private void updateSummaryCardChanges(double totalRevenue, int totalOrders) {
        // Tính % thay đổi so với kỳ trước
        double revenueChange = previousRevenue > 0 ? ((totalRevenue - previousRevenue) / previousRevenue) * 100 : 0;
        double ordersChange = previousOrders > 0 ? ((totalOrders - previousOrders) / (double)previousOrders) * 100 : 0;

        double currentAvgOrder = totalOrders > 0 ? totalRevenue / totalOrders : 0;
        double avgChange = previousAvgOrder > 0 ? ((currentAvgOrder - previousAvgOrder) / previousAvgOrder) * 100 : 0;

        // Hiển thị thay đổi
        lblRevenueChange.setVisible(true);
        lblOrdersChange.setVisible(true);
        lblAvgChange.setVisible(true);

        // Revenue change
        lblRevenueChange.setGraphic(loadArrowIcon(revenueChange >= 0));
        lblRevenueChange.setText(String.format("%+.1f%% from last period", revenueChange));
        lblRevenueChange.setStyle(revenueChange >= 0 ?
                "-fx-text-fill: #10b981; -fx-font-weight: 600;" :
                "-fx-text-fill: #ef4444; -fx-font-weight: 600;");

        // Orders change
        lblOrdersChange.setGraphic(loadArrowIcon(ordersChange >= 0));
        lblOrdersChange.setText(String.format("%+.1f%% from last period", ordersChange));
        lblOrdersChange.setStyle(ordersChange >= 0 ?
                "-fx-text-fill: #10b981; -fx-font-weight: 600;" :
                "-fx-text-fill: #ef4444; -fx-font-weight: 600;");

        // Avg order change
        lblAvgChange.setGraphic(loadArrowIcon(avgChange >= 0));
        lblAvgChange.setText(String.format("%+.1f%% from last period", avgChange));
        lblAvgChange.setStyle(avgChange >= 0 ?
                "-fx-text-fill: #10b981; -fx-font-weight: 600;" :
                "-fx-text-fill: #ef4444; -fx-font-weight: 600;");
    }

    private ImageView loadArrowIcon(boolean isUp) {
        ImageView arrow = new ImageView();
        try {
            String path = isUp ? "/resources/img/arrow-trend-up-solid-full.png" : "/resources/img/arrow-trend-down-solid-full.png";
            Image icon = new Image(getClass().getResourceAsStream(path));
            arrow.setImage(icon);
            arrow.setFitWidth(14);
            arrow.setFitHeight(14);
            arrow.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load arrow icon");
        }
        return arrow;
    }

    private DateRange getDateRange() {
        DateRange range = new DateRange();
        String period = cmbTimePeriod.getValue();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        range.toDate = cal.getTime();

        switch (period) {
            case "Today":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                range.fromDate = cal.getTime();
                break;

            case "This Week":
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                range.fromDate = cal.getTime();
                break;

            case "This Month":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                range.fromDate = cal.getTime();
                break;

            case "This Year":
                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                range.fromDate = cal.getTime();
                break;

            case "Custom Range":
                if (dpFromDate.getValue() != null && dpToDate.getValue() != null) {
                    range.fromDate = Date.from(dpFromDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    range.toDate = Date.from(dpToDate.getValue().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());
                } else {
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    range.fromDate = cal.getTime();

                    cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    range.toDate = cal.getTime();
                }
                break;

            default:
                cal.add(Calendar.DAY_OF_MONTH, -30);
                range.fromDate = cal.getTime();
        }

        return range;
    }

    // Lấy khoảng thời gian của kỳ trước
    private DateRange getPreviousDateRange() {
        DateRange current = getDateRange();
        DateRange previous = new DateRange();

        long duration = current.toDate.getTime() - current.fromDate.getTime();

        previous.toDate = new Date(current.fromDate.getTime() - 1000); // 1 giây trước kỳ hiện tại
        previous.fromDate = new Date(previous.toDate.getTime() - duration);

        return previous;
    }

    @FXML
    private void handleTableSearch() {
        String searchText = txtTableSearch.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            filteredData.setAll(reportData);
        } else {
            filteredData.clear();
            for (Map<String, Object> row : reportData) {
                boolean matches = false;
                for (Object value : row.values()) {
                    if (value != null && value.toString().toLowerCase().contains(searchText)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    filteredData.add(row);
                }
            }
        }

        tableReport.setItems(filteredData);
        lblTableSummary.setText("Showing " + filteredData.size() + " of " + reportData.size() + " records");
    }

    @FXML
    private void handleExportChart() {
        showAlert(Alert.AlertType.INFORMATION, "Export Chart", "Chart export feature coming soon!");
    }

    @FXML
    private void handleExportCSV() {
        if (reportData.isEmpty()) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "No Data", "No data to export!");
            return;
        }

        try {
            // Mở file chooser để người dùng chọn vị trí lưu
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save CSV File");
            fileChooser.setInitialFileName("report_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );

            java.io.File file = fileChooser.showSaveDialog(tableReport.getScene().getWindow());

            if (file != null) {
                FileWriter writer = new FileWriter(file);

                // Write headers
                Map<String, Object> firstRow = reportData.get(0);
                List<String> headers = new ArrayList<>(firstRow.keySet());
                writer.append(String.join(",", headers));
                writer.append("\n");

                // Write data với proper CSV escaping
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                for (Map<String, Object> row : reportData) {
                    List<String> values = new ArrayList<>();
                    for (String key : headers) {
                        Object value = row.get(key);
                        String stringValue = "";

                        if (value != null) {
                            if (value instanceof Date) {
                                stringValue = dateFormat.format((Date) value);
                            } else if (value instanceof Double) {
                                stringValue = String.format("%.2f", (Double) value);
                            } else {
                                stringValue = value.toString();
                            }

                            // Escape commas and quotes
                            if (stringValue.contains(",") || stringValue.contains("\"")) {
                                stringValue = "\"" + stringValue.replace("\"", "\"\"") + "\"";
                            }
                        }
                        values.add(stringValue);
                    }
                    writer.append(String.join(",", values));
                    writer.append("\n");
                }

                writer.flush();
                writer.close();

                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Export Successful",
                        "Data exported successfully to:\n" + file.getAbsolutePath());
            }

        } catch (IOException e) {
            e.printStackTrace();
            showSweetAlert(SweetAlert.AlertType.ERROR, "Export Failed",
                    "Failed to export CSV: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportPDF() {
        if (reportData.isEmpty()) {
            showSweetAlert(SweetAlert.AlertType.WARNING, "No Data", "No data to export!");
            return;
        }

        // Tạo nội dung report để in
        VBox reportContent = createPrintableReport();

        // Sử dụng PrinterJob để in hoặc save as PDF
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob == null) {
            showSweetAlert(SweetAlert.AlertType.ERROR, "Print Failed",
                    "Failed to print the report. Please try again.");
            return;
        }

        // Hiển thị print dialog
        boolean proceed = printerJob.showPrintDialog(tableReport.getScene().getWindow());

        if (proceed) {
            // Set page layout
            PageLayout pageLayout = printerJob.getPrinter().createPageLayout(
                    Paper.A4,
                    PageOrientation.LANDSCAPE,
                    0, 0, 0, 0
            );

            // Scale content to fit page
            double scaleX = pageLayout.getPrintableWidth() / reportContent.getBoundsInParent().getWidth();
            double scaleY = pageLayout.getPrintableHeight() / reportContent.getBoundsInParent().getHeight();
            double scale = Math.min(scaleX, scaleY);

            if (scale < 1.0) {
                reportContent.getTransforms().add(new javafx.scene.transform.Scale(scale, scale));
            }

            // Print the report
            boolean success = printerJob.printPage(pageLayout, reportContent);

            if (success) {
                printerJob.endJob();

                showSweetAlert(SweetAlert.AlertType.SUCCESS, "Print Successful",
                        "Your report has been sent to the printer.\n\n" +
                                "Tip: To save as PDF:\n" +
                                "   Select 'Microsoft Print to PDF' or\n" +
                                "   Select 'Save as PDF' in the print dialog");
            } else {
                showSweetAlert(SweetAlert.AlertType.ERROR, "Print Failed",
                        "Failed to print the report. Please try again.");
            }
        }
    }

    // Tạo nội dung report có thể in
    private VBox createPrintableReport() {
        VBox report = new VBox(15);
        report.setPadding(new Insets(30));
        report.setStyle("-fx-background-color: white;");
        report.setPrefWidth(1000);

        // === HEADER SECTION ===
        VBox headerSection = new VBox(10);
        headerSection.setAlignment(Pos.CENTER);

        // Logo & Title
        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER);

        ImageView logoIcon = new ImageView();
        try {
            Image icon = new Image(getClass().getResourceAsStream("/resources/img/chart-simple-solid-full.png"));
            logoIcon.setImage(icon);
            logoIcon.setFitWidth(40);
            logoIcon.setFitHeight(40);
            logoIcon.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load logo");
        }

        Label titleLabel = new Label("COFFEE SHOP MANAGEMENT");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        titleBox.getChildren().addAll(logoIcon, titleLabel);

        Label reportTitle = new Label(lblTableTitle.getText().toUpperCase());
        reportTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #667eea;");

        Label dateLabel = new Label("Generated: " + new SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm:ss").format(new Date()));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Separator headerSep = new Separator();
        headerSep.setMaxWidth(900);
        headerSep.setStyle("-fx-background-color: #bdc3c7;");

        headerSection.getChildren().addAll(titleBox, reportTitle, dateLabel, headerSep);

        // === SUMMARY SECTION ===
        VBox summarySection = new VBox(12);
        summarySection.setPadding(new Insets(15));
        summarySection.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");

        Label summaryTitle = new Label("SUMMARY");
        summaryTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        HBox summaryBox = new HBox(40);
        summaryBox.setAlignment(Pos.CENTER);

        // Total Revenue
        VBox revenueBox = new VBox(5);
        revenueBox.setAlignment(Pos.CENTER);
        Label revenueLabel = new Label("Total Revenue");
        revenueLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        Label revenueValue = new Label(lblTotalRevenue.getText());
        revenueValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
        revenueBox.getChildren().addAll(revenueLabel, revenueValue);

        // Total Orders
        VBox ordersBox = new VBox(5);
        ordersBox.setAlignment(Pos.CENTER);
        Label ordersLabel = new Label("Total Orders");
        ordersLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        Label ordersValue = new Label(lblTotalOrders.getText());
        ordersValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        ordersBox.getChildren().addAll(ordersLabel, ordersValue);

        // Net Profit
        if (!lblNetProfit.getText().equals("N/A")) {
            VBox profitBox = new VBox(5);
            profitBox.setAlignment(Pos.CENTER);
            Label profitLabel = new Label("Net Profit");
            profitLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
            Label profitValue = new Label(lblNetProfit.getText());
            profitValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            profitBox.getChildren().addAll(profitLabel, profitValue);
            summaryBox.getChildren().addAll(revenueBox, ordersBox, profitBox);
        } else {
            summaryBox.getChildren().addAll(revenueBox, ordersBox);
        }

        // Average Order Value
        VBox avgBox = new VBox(5);
        avgBox.setAlignment(Pos.CENTER);
        Label avgLabel = new Label("Avg Order Value");
        avgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        Label avgValue = new Label(lblAvgOrderValue.getText());
        avgValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
        avgBox.getChildren().addAll(avgLabel, avgValue);
        summaryBox.getChildren().add(avgBox);

        summarySection.getChildren().addAll(summaryTitle, summaryBox);

        // === DATA TABLE SECTION ===
        VBox tableSection = new VBox(10);
        tableSection.setPadding(new Insets(10, 0, 0, 0));

        Label tableTitle = new Label("DETAILED DATA");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        // Create table
        GridPane dataTable = new GridPane();
        dataTable.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1;");
        dataTable.setHgap(0);
        dataTable.setVgap(0);

        // Get headers
        Map<String, Object> firstRow = reportData.get(0);
        List<String> headers = new ArrayList<>(firstRow.keySet());

        // Column widths
        int columnWidth = 900 / headers.size();

        // Add header row
        int col = 0;
        for (String header : headers) {
            Label headerLabel = new Label(formatHeader(header));
            headerLabel.setStyle(
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; " +
                            "-fx-background-color: #34495e; -fx-padding: 10; " +
                            "-fx-border-color: #2c3e50; -fx-border-width: 0 1 1 0; -fx-alignment: center;"
            );
            headerLabel.setPrefWidth(columnWidth);
            headerLabel.setMaxWidth(columnWidth);
            dataTable.add(headerLabel, col, 0);
            col++;
        }

        // Add data rows
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        int maxRows = Math.min(20, reportData.size()); // Limit to 20 rows for printing

        for (int row = 0; row < maxRows; row++) {
            Map<String, Object> dataRow = reportData.get(row);
            col = 0;

            for (String key : headers) {
                Object value = dataRow.get(key);
                String cellValue = "";

                if (value != null) {
                    if (value instanceof Date) {
                        cellValue = dateFormat.format((Date) value);
                    } else if (value instanceof Double) {
                        if (key.toLowerCase().contains("margin")) {
                            cellValue = percentFormat.format((Double) value);
                        } else {
                            cellValue = currencyFormat.format((Double) value);
                        }
                    } else {
                        cellValue = value.toString();
                    }
                }

                Label cellLabel = new Label(cellValue);
                cellLabel.setStyle(
                        "-fx-font-size: 10px; -fx-padding: 8; " +
                                "-fx-border-color: #bdc3c7; -fx-border-width: 0 1 1 0; " +
                                "-fx-background-color: " + (row % 2 == 0 ? "white" : "#f9f9f9") + ";"
                );
                cellLabel.setPrefWidth(columnWidth);
                cellLabel.setMaxWidth(columnWidth);
                cellLabel.setWrapText(true);
                dataTable.add(cellLabel, col, row + 1);
                col++;
            }
        }

        // Show info if more rows exist
        if (reportData.size() > maxRows) {
            Label moreRowsLabel = new Label("... and " + (reportData.size() - maxRows) + " more rows");
            moreRowsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 10 0 0 0;");
            tableSection.getChildren().addAll(tableTitle, dataTable, moreRowsLabel);
        } else {
            tableSection.getChildren().addAll(tableTitle, dataTable);
        }

        // === FOOTER SECTION ===
        Separator footerSep = new Separator();
        footerSep.setMaxWidth(900);
        footerSep.setStyle("-fx-background-color: #bdc3c7;");

        VBox footerSection = new VBox(8);
        footerSection.setAlignment(Pos.CENTER);
        footerSection.setPadding(new Insets(15, 0, 0, 0));

        Label totalRecords = new Label("Total Records: " + reportData.size());
        totalRecords.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        Label systemLabel = new Label("Coffee Shop Management System - Report");
        systemLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6; -fx-font-style: italic;");

        footerSection.getChildren().addAll(totalRecords, systemLabel);

        // Add all sections to report
        report.getChildren().addAll(
                headerSection,
                summarySection,
                tableSection,
                footerSep,
                footerSection
        );

        return report;
    }

    // Helper method để format header names
    private String formatHeader(String header) {
        // Chuyển từ snake_case/camelCase sang Title Case
        String[] words = header.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            if (word.length() > 0) {
                formatted.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    formatted.append(word.substring(1).toLowerCase());
                }
            }
        }
        return formatted.toString();
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisible(show);
        loadingOverlay.setManaged(show);
    }

//    private void showAlert(Alert.AlertType type, String title, String message) {
//        Alert alert = new Alert(type);
//        alert.setTitle(title);
//        alert.setHeaderText(null);
//        alert.setContentText(message);
//        alert.showAndWait();
//    }

    // Inner class for date range
    private static class DateRange {
        Date fromDate;
        Date toDate;
    }
}
