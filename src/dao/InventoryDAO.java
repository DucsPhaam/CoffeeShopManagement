package dao;

import model.Inventory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    public List<Inventory> getAllInventory() {
        List<Inventory> inventoryList = new ArrayList<>();
        String sql = "SELECT id, name, quantity, unit, min_stock, cost_per_unit FROM inventory";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Inventory inv = new Inventory(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("quantity"),
                        rs.getString("unit"),
                        rs.getDouble("min_stock"),
                        rs.getDouble("cost_per_unit")
                );
                inventoryList.add(inv);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return inventoryList;
    }

    public boolean addInventory(Inventory inv) {
        String sql = "INSERT INTO inventory (name, quantity, unit, min_stock, cost_per_unit) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, inv.getName());
            stmt.setDouble(2, inv.getQuantity());
            stmt.setString(3, inv.getUnit());
            stmt.setDouble(4, inv.getMinStock());
            stmt.setDouble(5, inv.getCostPerUnit());
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    inv.setId(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateInventory(Inventory inv) {
        String sql = "UPDATE inventory SET name = ?, quantity = ?, unit = ?, min_stock = ?, cost_per_unit = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inv.getName());
            stmt.setDouble(2, inv.getQuantity());
            stmt.setString(3, inv.getUnit());
            stmt.setDouble(4, inv.getMinStock());
            stmt.setDouble(5, inv.getCostPerUnit());
            stmt.setInt(6, inv.getId());
            int rows = stmt.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteInventory(int id) {
        String sql = "DELETE FROM inventory WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public Inventory getInventoryById(int id) {
        String sql = "SELECT id, name, quantity, unit, min_stock, cost_per_unit FROM inventory WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Inventory(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("quantity"),
                        rs.getString("unit"),
                        rs.getDouble("min_stock"),
                        rs.getDouble("cost_per_unit")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}