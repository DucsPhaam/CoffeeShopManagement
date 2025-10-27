package dao;

import model.Transaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public List<Transaction> getAllTransactions() {
        List<Transaction> transactionList = new ArrayList<>();
        String sql = "SELECT id, type, amount, reason, created_by, created_at FROM transactions";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getString("reason"),
                        rs.getInt("created_by"),
                        rs.getTimestamp("created_at")
                );
                transactionList.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactionList;
    }

    public List<Transaction> getTransactionsPaginated(int offset, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, type, amount, reason, created_by, created_at, display_order FROM transactions " +
                "ORDER BY display_order DESC LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getDouble("amount"),
                            rs.getString("reason"),
                            rs.getInt("created_by"),
                            rs.getTimestamp("created_at")
                    );
                    transaction.setDisplayOrder(rs.getInt("display_order"));
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }
    public boolean addTransaction(Transaction transaction) {
        String sqlUpdate = "UPDATE transactions SET display_order = display_order + 1";
        String sqlInsert = "INSERT INTO transactions (type, amount, reason, created_by, created_at, display_order) VALUES (?, ?, ?, ?, NOW(), ?)";
        String sqlMaxDisplayOrder = "SELECT COALESCE(MAX(display_order), 0) AS max_order FROM transactions";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Lấy giá trị max của display_order
            int newDisplayOrder = 1;
            try (PreparedStatement stmtMax = conn.prepareStatement(sqlMaxDisplayOrder);
                 ResultSet rs = stmtMax.executeQuery()) {
                if (rs.next()) {
                    newDisplayOrder = rs.getInt("max_order") + 1;
                }
            }

            // Tăng display_order của tất cả giao dịch hiện có
            try (PreparedStatement stmtUpdateAll = conn.prepareStatement(sqlUpdate)) {
                stmtUpdateAll.executeUpdate();
            }

            // Thêm giao dịch mới với display_order mới
            try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                stmtInsert.setString(1, transaction.getType());
                stmtInsert.setDouble(2, transaction.getAmount());
                stmtInsert.setString(3, transaction.getReason());
                stmtInsert.setInt(4, transaction.getCreatedBy());
                stmtInsert.setInt(5, newDisplayOrder); // Gán display_order mới
                int rows = stmtInsert.executeUpdate();

                if (rows > 0) {
                    ResultSet generatedKeys = stmtInsert.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        transaction.setId(generatedKeys.getInt(1));
                    }
                    conn.commit();
                    return true;
                }
            }
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error in addTransaction: " + e.getMessage());
            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }
    public boolean updateTransaction(Transaction transaction) {
        String sql = "UPDATE transactions SET type = ?, amount = ?, reason = ?, created_by = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transaction.getType());
            stmt.setDouble(2, transaction.getAmount());
            stmt.setString(3, transaction.getReason());
            stmt.setInt(4, transaction.getCreatedBy());
            stmt.setInt(5, transaction.getId());
            int rows = stmt.executeUpdate();

            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteTransaction(int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";

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

    public double calculateBalance() {
        String sql = "SELECT SUM(CASE WHEN type = 'income' THEN amount ELSE -amount END) as balance FROM transactions";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}