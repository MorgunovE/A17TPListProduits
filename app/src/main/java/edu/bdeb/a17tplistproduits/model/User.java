package edu.bdeb.a17tplistproduits.model;

public class User {
    private String id;
    private String username;
    private String email;
    private String token;
    private String password;

    public User(String id, String username, String email, String token, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.token = token;
        this.password = password;
    }

    public User() {

    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public void setName(String name) {
        this.username = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}