package edu.bdeb.a17tplistproduits.model;

import java.util.ArrayList;
import java.util.List;

public class ProductList {
    private String id;
    private String nom;
    private String description;
    private String utilisateurId;
    private List<Product> produits;

    public ProductList(String id, String nom, String description, String utilisateurId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.utilisateurId = utilisateurId;
        this.produits = new ArrayList<>();
    }

    // Version sans id pour création
    public ProductList(String nom, String description) {
        this.nom = nom;
        this.description = description;
        this.produits = new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(String utilisateurId) { this.utilisateurId = utilisateurId; }
    public List<Product> getProduits() { return produits; }
    public void setProduits(List<Product> produits) { this.produits = produits; }

    public void addProduct(Product product) {
        if (produits == null) {
            produits = new ArrayList<>();
        }
        produits.add(product);
    }
}