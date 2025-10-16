package model;

public class Table {
    private int id;
    private String name;
    private int floor;
    private int seats;
    private String status;

    public Table() {}

    public Table(int id, String name, int floor, int seats, String status) {
        this.id = id;
        this.name = name;
        this.floor = floor;
        this.seats = seats;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}