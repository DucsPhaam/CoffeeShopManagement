package model;

public class ProductIngredient {
    private int id;
    private int productId;
    private int inventoryId;
    private double quantity;

    public ProductIngredient() {}

    public ProductIngredient(int id, int productId, int inventoryId, double quantity) {
        this.id = id;
        this.productId = productId;
        this.inventoryId = inventoryId;
        this.quantity = quantity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getInventoryId() { return inventoryId; }
    public void setInventoryId(int inventoryId) { this.inventoryId = inventoryId; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
}