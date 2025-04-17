package edu.bdeb.a17tplistproduits.ui.products;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class AddProductActivity extends AppCompatActivity {

    private EditText editTextProductName;
    private EditText editTextProductQuantity;
    private EditText editTextProductUnit;
    private EditText editTextProductPrice;
    private EditText editTextProductDescription;
    private Button buttonAddProduct;
    private ProgressBar progressBar;

    private ApiClient apiClient;
    private SessionManager sessionManager;
    private String listId; // ID de la liste où ajouter directement le produit (optionnel)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Initialiser les services
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Initialiser les vues
        editTextProductName = findViewById(R.id.editTextProductName);
        editTextProductQuantity = findViewById(R.id.editTextProductQuantity);
        editTextProductUnit = findViewById(R.id.editTextProductUnit);
        editTextProductPrice = findViewById(R.id.editTextProductPrice);
        editTextProductDescription = findViewById(R.id.editTextProductDescription);
        buttonAddProduct = findViewById(R.id.buttonAddProduct);
        progressBar = findViewById(R.id.progressBar);

        // Récupérer l'ID de la liste si passé
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
        }

        // Configurer l'écouteur du bouton d'ajout
        buttonAddProduct.setOnClickListener(v -> ajouterProduit());
    }

    private void ajouterProduit() {
        // Récupérer les valeurs saisies
        String nom = editTextProductName.getText().toString().trim();
        String quantiteStr = editTextProductQuantity.getText().toString().trim();
        String unite = editTextProductUnit.getText().toString().trim();
        String prixStr = editTextProductPrice.getText().toString().trim();
        String description = editTextProductDescription.getText().toString().trim();

        // Valider les entrées
        if (nom.isEmpty()) {
            editTextProductName.setError("Nom du produit requis");
            editTextProductName.requestFocus();
            return;
        }

        if (quantiteStr.isEmpty()) {
            editTextProductQuantity.setError("Quantité requise");
            editTextProductQuantity.requestFocus();
            return;
        }

        if (unite.isEmpty()) {
            editTextProductUnit.setError("Unité requise");
            editTextProductUnit.requestFocus();
            return;
        }

        if (prixStr.isEmpty()) {
            editTextProductPrice.setError("Prix requis");
            editTextProductPrice.requestFocus();
            return;
        }

        double quantite;
        double prix;

        try {
            quantite = Double.parseDouble(quantiteStr);
        } catch (NumberFormatException e) {
            editTextProductQuantity.setError("Quantité invalide");
            editTextProductQuantity.requestFocus();
            return;
        }

        try {
            prix = Double.parseDouble(prixStr);
        } catch (NumberFormatException e) {
            editTextProductPrice.setError("Prix invalide");
            editTextProductPrice.requestFocus();
            return;
        }

        // Créer l'objet Product
        Product nouveauProduit = new Product();
        nouveauProduit.setNom(nom);
        nouveauProduit.setQuantite(quantite);
        nouveauProduit.setUnite(unite);
        nouveauProduit.setPrix(prix);
        nouveauProduit.setDescription(description);

        // Afficher la progression et désactiver le bouton
        progressBar.setVisibility(View.VISIBLE);
        buttonAddProduct.setEnabled(false);

        // Envoyer la requête à l'API
        try {
            ApiClient.ApiResponse<Product> response = apiClient.createProduct(nouveauProduit).get();

            if (response.isSuccess() && response.getData() != null) {
                Product produitCree = response.getData();

                if (listId != null) {
                    // Si un ID de liste est fourni, ajouter le produit à cette liste
                    ajouterProduitAListe(produitCree.getId(), quantite);
                } else {
                    // Sinon, simplement informer l'utilisateur que le produit a été créé
                    Toast.makeText(this, "Produit créé avec succès", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            } else {
                Toast.makeText(this,
                        "Erreur lors de la création du produit: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                buttonAddProduct.setEnabled(true);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    "Erreur réseau: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            buttonAddProduct.setEnabled(true);
        }
    }

    private void ajouterProduitAListe(String productId, double quantite) {
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.addProductToList(listId, productId, quantite).get();

            if (response.isSuccess()) {
                Toast.makeText(this, "Produit ajouté à la liste avec succès", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this,
                        "Erreur lors de l'ajout du produit à la liste: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    "Erreur réseau: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonAddProduct.setEnabled(true);
        }
    }
}