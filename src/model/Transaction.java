package model;

import java.sql.Timestamp;

public class Transaction {
    private int id;
    private String type; // "income" or "expense"
    private double amount;
    private String reason;
    private int createdBy;
    private Integer orderId;
    private Timestamp createdAt;

    public Transaction() {}

    public Transaction(int id, String type, double amount, String reason, int createdBy, Timestamp createdAt) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

}