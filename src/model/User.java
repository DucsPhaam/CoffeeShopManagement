package model;

public class User {
    private int id;
    private String username;
    private String role;
    private String password;
    private String status;

    public User() {}

    public User(int id, String username, String role, String password, String status) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.password = password;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}