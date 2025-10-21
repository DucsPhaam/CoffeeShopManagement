package model;

public class Inventory {
    private int id;
    private String name;
    private double quantity;
    private String unit;
    private double minStock;
    private double costPerUnit;

    public Inventory() {}

    public Inventory(int id, String name, double quantity, String unit, double minStock, double costPerUnit) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.minStock = minStock;
        this.costPerUnit = costPerUnit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getMinStock() { return minStock; }
    public void setMinStock(double minStock) { this.minStock = minStock; }

    public double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(double costPerUnit) { this.costPerUnit = costPerUnit; }
}