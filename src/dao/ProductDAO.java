package dao;

import model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductDAO {

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, name, price, drink_types, image FROM products ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt("id"));
                product.setName(rs.getString("name"));
                product.setPrice(rs.getDouble("price"));
                // Chuyển chuỗi drink_types (ví dụ: "hot,iced,frappe") thành Set
                String[] drinkTypesArray = rs.getString("drink_types").split(",");
                Set<String> drinkTypes = new HashSet<>();
                for (String type : drinkTypesArray) {
                    drinkTypes.add(type.trim().toLowerCase());
                }
                product.setDrinkTypes(drinkTypes);
                product.setImage(rs.getString("image"));
                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public Product getProductById(int productId) {
        String sql = "SELECT id, name, price, drink_types, image FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt("id"));
                product.setName(rs.getString("name"));
                product.setPrice(rs.getDouble("price"));
                String[] drinkTypesArray = rs.getString("drink_types").split(",");
                Set<String> drinkTypes = new HashSet<>();
                for (String type : drinkTypesArray) {
                    drinkTypes.add(type.trim().toLowerCase());
                }
                product.setDrinkTypes(drinkTypes);
                product.setImage(rs.getString("image"));
                return product;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addProduct(Product product) {
        String sql = "INSERT INTO products (name, price, drink_types, image) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getName());
            stmt.setDouble(2, product.getPrice());
            // Chuyển Set<String> thành chuỗi (ví dụ: "hot,iced,frappe")
            String drinkTypesStr = String.join(",", product.getDrinkTypes());
            stmt.setString(3, drinkTypesStr);
            stmt.setString(4, product.getImage());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, price = ?, drink_types = ?, image = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getName());
            stmt.setDouble(2, product.getPrice());
            String drinkTypesStr = String.join(",", product.getDrinkTypes());
            stmt.setString(3, drinkTypesStr);
            stmt.setString(4, product.getImage());
            stmt.setInt(5, product.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Product> getProductsPaginated(int offset, int limit) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, name, price, image, drink_types FROM products ORDER BY name ASC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product();
                    p.setId(rs.getInt("id"));
                    p.setName(rs.getString("name"));
                    p.setPrice(rs.getDouble("price"));
                    p.setImage(rs.getString("image"));

                    String typesStr = rs.getString("drink_types");
                    Set<String> types = new HashSet<>();
                    if (typesStr != null && !typesStr.isEmpty()) {
                        for (String t : typesStr.split(",")) {
                            types.add(t.trim().toLowerCase());
                        }
                    }
                    p.setDrinkTypes(types);

                    products.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
}