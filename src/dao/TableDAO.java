package dao;

import model.Table;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {

    public List<Table> getAllTables() {
        List<Table> tableList = new ArrayList<>();
        String sql = "SELECT id, name, floor, seats, status FROM tables";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Table table = new Table(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("floor"),
                        rs.getInt("seats"),
                        rs.getString("status")
                );
                tableList.add(table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tableList;
    }

    public boolean addTable(Table table) {
        String sql = "INSERT INTO tables (name, floor, seats, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, table.getName());
            stmt.setInt(2, table.getFloor());
            stmt.setInt(3, table.getSeats());
            stmt.setString(4, table.getStatus());
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    table.setId(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateTable(Table table) {
        String sql = "UPDATE tables SET name = ?, floor = ?, seats = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, table.getName());
            stmt.setInt(2, table.getFloor());
            stmt.setInt(3, table.getSeats());
            stmt.setString(4, table.getStatus());
            stmt.setInt(5, table.getId());
            int rows = stmt.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteTable(int id) {
        String sql = "DELETE FROM tables WHERE id = ?";

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
}