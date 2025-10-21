package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.ProductIngredient;

public class ProductIngredientDAO {

    // Lấy danh sách nguyên liệu cần cho 1 sản phẩm (productId -> quantity)
    public Map<Integer, Double> getIngredientsByProduct(int productId) {
        Map<Integer, Double> ingredients = new HashMap<>();
        String sql = "SELECT inventory_id, quantity FROM product_ingredients WHERE product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int inventoryId = rs.getInt("inventory_id");
                double quantity = rs.getDouble("quantity");
                ingredients.put(inventoryId, quantity);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ingredients;
    }

    public boolean consumeIngredients(Connection conn, Map<Integer, Double> usedIngredients) throws SQLException {
        for (Map.Entry<Integer, Double> entry : usedIngredients.entrySet()) {
            int invId = entry.getKey();
            double qtyNeeded = entry.getValue();

            String checkSql = "SELECT quantity FROM inventory WHERE id = ? FOR UPDATE";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, invId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    double available = rs.getDouble("quantity");
                    if (available < qtyNeeded) {
                        return false; // Không đủ nguyên liệu
                    }
                } else {
                    return false; // Nguyên liệu không tồn tại
                }
            }

            String updateSql = "UPDATE inventory SET quantity = quantity - ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, qtyNeeded);
                updateStmt.setInt(2, invId);
                updateStmt.executeUpdate();
            }
        }
        return true;
    }

    public List<ProductIngredient> getIngredientsByProductId(int productId) {
        List<ProductIngredient> list = new ArrayList<>();
        String sql = "SELECT id, product_id, inventory_id, quantity FROM product_ingredients WHERE product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProductIngredient pi = new ProductIngredient(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getInt("inventory_id"),
                        rs.getDouble("quantity")
                );
                list.add(pi);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean deleteByProductId(int productId) {
        String sql = "DELETE FROM product_ingredients WHERE product_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean addProductIngredient(ProductIngredient pi) {
        String sql = "INSERT INTO product_ingredients (product_id, inventory_id, quantity) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, pi.getProductId());
            stmt.setInt(2, pi.getInventoryId());
            stmt.setDouble(3, pi.getQuantity());
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    pi.setId(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}