package model;

public class ProductSales {
    private int productId;
    private String name;
    private int quantitySold;
    private double totalRevenue;

    // Default constructor (required if using FXML or certain JavaFX features)
    public ProductSales() {
    }

    // Parameterized constructor
    public ProductSales(int productId, String name, int quantitySold, double totalRevenue) {
        this.productId = productId;
        this.name = name;
        this.quantitySold = quantitySold;
        this.totalRevenue = totalRevenue;
    }

    public int getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    // Optional: Setters if needed
    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQuantitySold(int quantitySold) {
        this.quantitySold = quantitySold;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}