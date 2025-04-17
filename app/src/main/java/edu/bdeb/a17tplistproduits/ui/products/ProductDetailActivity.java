package edu.bdeb.a17tplistproduits.ui.products;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ProductDetailActivity extends AppCompatActivity {

    private ApiClient apiClient;
    private SessionManager sessionManager;

    private TextView textViewProductName;
    private TextView textViewProductQuantity;
    private TextView textViewProductUnit;
    private TextView textViewProductPrice;
    private TextView textViewProductDescription;
    private Button buttonAddToList;
    private ProgressBar progressBar;

    private String productId;
    private Product product;
    private NumberFormat currencyFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Initialiser les services
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);
        currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.product_details);
        }

        // Initialiser les vues
        textViewProductName = findViewById(R.id.textViewProductName);
        textViewProductQuantity = findViewById(R.id.textViewProductQuantity);
        textViewProductUnit = findViewById(R.id.textViewProductUnit);
        textViewProductPrice = findViewById(R.id.textViewProductPrice);
        textViewProductDescription = findViewById(R.id.textViewProductDescription);
        buttonAddToList = findViewById(R.id.buttonAddToList);
        progressBar = findViewById(R.id.progressBar);

        // Récupérer l'ID du produit depuis l'intent
        if (getIntent().hasExtra("product_id")) {
            productId = getIntent().getStringExtra("product_id");
            chargerDetailsProduit();
        } else {
            Toast.makeText(this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Configurer les écouteurs
        buttonAddToList.setOnClickListener(v -> recupererListes());
    }

    private void chargerDetailsProduit() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<Product> response = apiClient.getProduct(productId).get();

            if (response.isSuccess() && response.getData() != null) {
                product = response.getData();
                mettreAJourInterface();
            } else {
                Toast.makeText(this,
                    "Erreur lors du chargement du produit: " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                "Erreur réseau: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            finish();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void mettreAJourInterface() {
        textViewProductName.setText(product.getNom());
        textViewProductQuantity.setText(String.valueOf(product.getQuantite()));
        textViewProductUnit.setText(product.getUnite());
        textViewProductPrice.setText(currencyFormat.format(product.getPrix()));

        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            textViewProductDescription.setText(product.getDescription());
            textViewProductDescription.setVisibility(View.VISIBLE);
        } else {
            textViewProductDescription.setVisibility(View.GONE);
        }
    }

    private void recupererListes() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<List<ProductList>> response = apiClient.getLists().get();

            if (response.isSuccess() && response.getData() != null) {
                List<ProductList> listes = response.getData();
                if (listes.isEmpty()) {
                    Toast.makeText(this, R.string.no_lists, Toast.LENGTH_SHORT).show();
                } else {
                    afficherDialogueChoixListe(listes);
                }
            } else {
                Toast.makeText(this,
                    "Erreur lors du chargement des listes: " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                "Erreur réseau: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void afficherDialogueChoixListe(List<ProductList> listes) {
        // Créer les tableaux pour le dialogue
        String[] nomListes = new String[listes.size()];
        String[] idListes = new String[listes.size()];

        for (int i = 0; i < listes.size(); i++) {
            nomListes[i] = listes.get(i).getNom();
            idListes[i] = listes.get(i).getId();
        }

        // Créer et afficher le dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_list)
               .setItems(nomListes, (dialog, which) -> {
                   afficherDialogueQuantite(idListes[which]);
               })
               .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void afficherDialogueQuantite(String listeId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_product_quantity, null);
        EditText editTextQuantity = view.findViewById(R.id.editTextQuantity);
        editTextQuantity.setText(String.valueOf(product.getQuantite()));

        builder.setView(view)
               .setTitle(getString(R.string.quantity_for, product.getNom()))
               .setPositiveButton(R.string.add, (dialog, which) -> {
                   try {
                       double quantite = Double.parseDouble(editTextQuantity.getText().toString());
                       ajouterProduitAListe(listeId, quantite);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, R.string.invalid_quantity, Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void ajouterProduitAListe(String listeId, double quantite) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.addProductToList(listeId, productId, quantite).get();

            if (response.isSuccess()) {
                Toast.makeText(this, R.string.product_added_to_list, Toast.LENGTH_SHORT).show();
                // Retourner un résultat positif
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this,
                    "Erreur lors de l'ajout du produit: " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                "Erreur réseau: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
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