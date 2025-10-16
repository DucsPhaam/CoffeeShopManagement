package model;

public class Payment {
    private int id;
    private int orderId;
    private double totalPrice;
    private double vat;
    private double amountReceived;
    private double changeReturned;
    private String paidAt;

    public Payment() {}

    public Payment(int id, int orderId, double totalPrice, double vat, double amountReceived, double changeReturned, String paidAt) {
        this.id = id;
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.vat = vat;
        this.amountReceived = amountReceived;
        this.changeReturned = changeReturned;
        this.paidAt = paidAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public double getVat() { return vat; }
    public void setVat(double vat) { this.vat = vat; }
    public double getAmountReceived() { return amountReceived; }
    public void setAmountReceived(double amountReceived) { this.amountReceived = amountReceived; }
    public double getChangeReturned() { return changeReturned; }
    public void setChangeReturned(double changeReturned) { this.changeReturned = changeReturned; }
    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }

    public double getGrandTotal() { return totalPrice + vat; }
}