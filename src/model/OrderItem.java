package model;

import java.util.Set;
import java.util.HashSet;

public class OrderItem {
    private int id;
    private int orderId;
    private int productId;
    private String drinkType; // hot, iced, frappe
    private int quantity;
    private double price;
    private String note;
    private String productName; // For display purposes
    private String image; // Product image path
    private Set<String> availableDrinkTypes; // Available drink types for this product

    public OrderItem() {
        this.availableDrinkTypes = new HashSet<>();
    }

    public OrderItem(int id, int orderId, int productId, String drinkType, int quantity, double price, String note) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.drinkType = drinkType;
        this.quantity = quantity;
        this.price = price;
        this.note = note != null ? note : "";
        this.availableDrinkTypes = new HashSet<>();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getDrinkType() {
        return drinkType;
    }

    public void setDrinkType(String drinkType) {
        this.drinkType = drinkType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note != null ? note : "";
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Set<String> getAvailableDrinkTypes() {
        return availableDrinkTypes;
    }

    public void setAvailableDrinkTypes(Set<String> availableDrinkTypes) {
        this.availableDrinkTypes = availableDrinkTypes != null ? availableDrinkTypes : new HashSet<>();
    }

    public double getTotalPrice() {
        return price * quantity;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", drinkType='" + drinkType + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", note='" + note + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}