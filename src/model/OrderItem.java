package model;

public class OrderItem {
    private int id;
    private int orderId;
    private int productId;
    private String drinkType; // hot, iced, frappe
    private int quantity;
    private double price;
    private String note; // Thêm trường note
    private String productName; // For display purposes

    public OrderItem() {}

    public OrderItem(int id, int orderId, int productId, String drinkType, int quantity, double price, String note) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.drinkType = drinkType;
        this.quantity = quantity;
        this.price = price;
        this.note = note != null ? note : "";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getDrinkType() { return drinkType; }
    public void setDrinkType(String drinkType) { this.drinkType = drinkType; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note != null ? note : ""; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getTotalPrice() { return price * quantity; }
}