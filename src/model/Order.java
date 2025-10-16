package model;

public class Order {
    private int id;
    private Integer tableId; // Nullable
    private int staffId;
    private String orderType;
    private String status;
    private String createdAt;
    private double totalPrice;

    // Default constructor
    public Order() {
    }

    // Constructor with 7 parameters
    public Order(int id, Integer tableId, int staffId, String orderType, String status, String createdAt, double totalPrice) {
        this.id = id;
        this.tableId = tableId;
        this.staffId = staffId;
        this.orderType = orderType;
        this.status = status;
        this.createdAt = createdAt;
        this.totalPrice = totalPrice;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getTableId() {
        return tableId;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}