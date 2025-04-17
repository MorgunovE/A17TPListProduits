package edu.bdeb.a17tplistproduits.model;

public class Product {
    private String id;
    private String nom;
    private double quantite;
    private String unite;
    private double prix;
    private String description;

    public Product(String id, String nom, double quantite, String unite, double prix, String description) {
        this.id = id;
        this.nom = nom;
        this.quantite = quantite;
        this.unite = unite;
        this.prix = prix;
        this.description = description;
    }

    // Version sans id pour création de produit
    public Product(String nom, double quantite, String unite, double prix, String description) {
        this.nom = nom;
        this.quantite = quantite;
        this.unite = unite;
        this.prix = prix;
        this.description = description;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public double getQuantite() { return quantite; }
    public void setQuantite(double quantite) { this.quantite = quantite; }
    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}