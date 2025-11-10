package dao;

import model.Order;
import model.OrderItem;
import model.Payment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // Create new order (Takeaway or Dine-in)
    public int createOrder(Integer tableId, int staffId, String orderType) {
        String sql = "INSERT INTO orders (table_id, staff_id, order_type, status) VALUES (?, ?, ?, 'unpaid')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (tableId != null) {
                stmt.setInt(1, tableId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setInt(2, staffId);
            stmt.setString(3, orderType);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Add item to order
    public boolean addOrderItem(int orderId, int productId, String drinkType, int quantity, double price, String note) {
        String sql = "INSERT INTO order_items (order_id, product_id, drink_type, quantity, price, note) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setString(3, drinkType);
            stmt.setInt(4, quantity);
            stmt.setDouble(5, price);
            stmt.setString(6, note != null ? note : "");

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Get order items
    public List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.*, p.name as product_name FROM order_items oi " +
                "JOIN products p ON oi.product_id = p.id WHERE oi.order_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getInt("id"));
                item.setOrderId(rs.getInt("order_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setDrinkType(rs.getString("drink_type"));
                item.setQuantity(rs.getInt("quantity"));
                item.setPrice(rs.getDouble("price"));
                item.setNote(rs.getString("note"));
                item.setProductName(rs.getString("product_name"));
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    // Update order item quantity
    public boolean updateOrderItemQuantity(int itemId, int quantity) {
        String sql = "UPDATE order_items SET quantity = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, itemId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Delete order item
    public boolean deleteOrderItem(int itemId) {
        String sql = "DELETE FROM order_items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Get unpaid order by table
    public Order getUnpaidOrderByTable(int tableId) {
        String sql = "SELECT * FROM orders WHERE table_id = ? AND status = 'unpaid' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tableId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setTableId(rs.getObject("table_id") != null ? rs.getInt("table_id") : null);
                order.setStaffId(rs.getInt("staff_id"));
                order.setOrderType(rs.getString("order_type"));
                order.setStatus(rs.getString("status"));
                order.setCreatedAt(rs.getString("created_at"));
                return order;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Get order by ID
    public Order getOrderById(int orderId) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setTableId(rs.getObject("table_id") != null ? rs.getInt("table_id") : null);
                order.setStaffId(rs.getInt("staff_id"));
                order.setOrderType(rs.getString("order_type"));
                order.setStatus(rs.getString("status"));
                order.setCreatedAt(rs.getString("created_at"));
                return order;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Calculate order total
    public double calculateOrderTotal(int orderId) {
        String sql = "SELECT SUM(price * quantity) as total FROM order_items WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // Process payment
    public boolean processPayment(int orderId, double totalPrice, double vat, double amountReceived, double changeReturned) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Insert payment
            String paymentSql = "INSERT INTO payments (order_id, total_price, vat, amount_received, change_returned) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement paymentStmt = conn.prepareStatement(paymentSql);
            paymentStmt.setInt(1, orderId);
            paymentStmt.setDouble(2, totalPrice);
            paymentStmt.setDouble(3, vat);
            paymentStmt.setDouble(4, amountReceived);
            paymentStmt.setDouble(5, changeReturned);
            paymentStmt.executeUpdate();

            // Update order status
            String orderSql = "UPDATE orders SET status = 'paid' WHERE id = ?";
            PreparedStatement orderStmt = conn.prepareStatement(orderSql);
            orderStmt.setInt(1, orderId);
            orderStmt.executeUpdate();

            // Add income transaction WITH order_id ← CẬP NHẬT PHẦN NÀY
            String transSql = "INSERT INTO transactions (type, amount, reason, created_by, order_id) VALUES ('income', ?, ?, ?, ?)";
            PreparedStatement transStmt = conn.prepareStatement(transSql);
            transStmt.setDouble(1, totalPrice + vat);
            transStmt.setString(2, "Payment for order #" + orderId);
            transStmt.setInt(3, utils.SessionManager.getCurrentUserId());
            transStmt.setInt(4, orderId); // ← THÊM ORDER_ID
            transStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    // Get all orders (for history/reports)
    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setTableId(rs.getObject("table_id") != null ? rs.getInt("table_id") : null);
                order.setStaffId(rs.getInt("staff_id"));
                order.setOrderType(rs.getString("order_type"));
                order.setStatus(rs.getString("status"));
                order.setCreatedAt(rs.getString("created_at"));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    // Get payment by order ID
    public Payment getPaymentByOrderId(int orderId) {
        String sql = "SELECT * FROM payments WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Payment payment = new Payment();
                payment.setId(rs.getInt("id"));
                payment.setOrderId(rs.getInt("order_id"));
                payment.setTotalPrice(rs.getDouble("total_price"));
                payment.setVat(rs.getDouble("vat"));
                payment.setAmountReceived(rs.getDouble("amount_received"));
                payment.setChangeReturned(rs.getDouble("change_returned"));
                payment.setPaidAt(rs.getString("paid_at"));
                return payment;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}