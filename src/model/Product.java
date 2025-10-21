package model;

import java.util.Set;

public class Product {
    private int id;
    private String name;
    private double price;
    private Set<String> drinkTypes;
    private String image;

    public Product() {}

    public Product(int id, String name, double price, Set<String> drinkTypes, String image) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.drinkTypes = drinkTypes;
        this.image = image;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public Set<String> getDrinkTypes() { return drinkTypes; }
    public void setDrinkTypes(Set<String> drinkTypes) { this.drinkTypes = drinkTypes; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public double getPriceByType(String type) {
        return drinkTypes.contains(type.toLowerCase()) ? price : 0.0;
    }
}