package edu.bdeb.a17tplistproduits.ui.products;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
    private String listId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        editTextProductName = findViewById(R.id.editTextProductName);
        editTextProductQuantity = findViewById(R.id.editTextProductQuantity);
        editTextProductUnit = findViewById(R.id.editTextProductUnit);
        editTextProductPrice = findViewById(R.id.editTextProductPrice);
        editTextProductDescription = findViewById(R.id.editTextProductDescription);
        buttonAddProduct = findViewById(R.id.buttonAddProduct);
        progressBar = findViewById(R.id.progressBar);

        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
        }

        buttonAddProduct.setOnClickListener(v -> ajouterProduit());
    }

    private void ajouterProduit() {
        String nom = editTextProductName.getText().toString().trim();
        String quantiteStr = editTextProductQuantity.getText().toString().trim();
        String unite = editTextProductUnit.getText().toString().trim();
        String prixStr = editTextProductPrice.getText().toString().trim();
        String description = editTextProductDescription.getText().toString().trim();

        if (nom.isEmpty()) {
            editTextProductName.setError("Le nom du produit est requis");
            editTextProductName.requestFocus();
            return;
        }

        if (quantiteStr.isEmpty()) {
            editTextProductQuantity.setError("La quantité est requise");
            editTextProductQuantity.requestFocus();
            return;
        }

        if (unite.isEmpty()) {
            editTextProductUnit.setError("L'unité est requise");
            editTextProductUnit.requestFocus();
            return;
        }

        if (prixStr.isEmpty()) {
            editTextProductPrice.setError("Le prix est requis");
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

        Product nouveauProduit = new Product();
        nouveauProduit.setNom(nom);
        nouveauProduit.setQuantite(quantite);
        nouveauProduit.setUnite(unite);
        nouveauProduit.setPrix(prix);
        nouveauProduit.setDescription(description);

        progressBar.setVisibility(View.VISIBLE);
        buttonAddProduct.setEnabled(false);

        try {
            ApiClient.ApiResponse<String> response = apiClient.createProduct(nouveauProduit).get();

            if (response.isSuccess() && response.getData() != null) {
                String produitCree = response.getData();

                if (listId != null) {
                    // Si un ID de liste est fourni, ajouter le produit à cette liste
                    ajouterProduitAListe(produitCree, quantite);
                } else {
                    // Sinon, informer l'utilisateur que le produit a été créé
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}