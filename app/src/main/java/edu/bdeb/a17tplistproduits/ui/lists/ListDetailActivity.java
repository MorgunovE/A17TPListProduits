package edu.bdeb.a17tplistproduits.ui.lists;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.adapters.ProductAdapter;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.ui.products.ProductsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ListDetailActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private static final String TAG = "ListDetailActivity";
    private static final int REQUEST_ADD_PRODUCT = 1;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    private TextView textViewListName;
    private TextView textViewListDescription;
    private TextView textViewProductsTitle;
    private RecyclerView recyclerViewProducts;
    private TextView textViewEmptyList;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddProduct;
    private Button buttonCopyList;

    private ProductList productList;
    private String listId;
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        // Initialiser les services
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.list_details);
        }

        // Initialiser les vues
        textViewListName = findViewById(R.id.textViewListName);
        textViewListDescription = findViewById(R.id.textViewListDescription);
        textViewProductsTitle = findViewById(R.id.textViewProductsTitle);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        textViewEmptyList = findViewById(R.id.textViewEmptyList);
        progressBar = findViewById(R.id.progressBar);
        fabAddProduct = findViewById(R.id.fabAddProduct);
        buttonCopyList = findViewById(R.id.buttonCopyList);

        // Configurer le RecyclerView
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(new ArrayList<>(), this);
        recyclerViewProducts.setAdapter(adapter);

        // Obtenir l'ID de la liste depuis l'intent
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
            chargerDetailsDeLaListe();
        } else {
            Toast.makeText(this, R.string.list_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Configurer les écouteurs d'événements
        fabAddProduct.setOnClickListener(v -> ouvrirEcranAjoutProduit());
        buttonCopyList.setOnClickListener(v -> afficherDialogueCopie());
    }

    private void chargerDetailsDeLaListe() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.getList(listId).get();

            if (response.isSuccess() && response.getData() != null) {
                productList = response.getData();
                mettreAJourInterface();
            } else {
                Toast.makeText(this,
                    "Erreur lors du chargement de la liste: " + response.getErrorMessage(),
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
        textViewListName.setText(productList.getNom());

        if (productList.getDescription() != null && !productList.getDescription().isEmpty()) {
            textViewListDescription.setText(productList.getDescription());
            textViewListDescription.setVisibility(View.VISIBLE);
        } else {
            textViewListDescription.setVisibility(View.GONE);
        }

        if (productList.getProduits() != null && !productList.getProduits().isEmpty()) {
            adapter = new ProductAdapter(productList.getProduits(), this);
            recyclerViewProducts.setAdapter(adapter);
            textViewEmptyList.setVisibility(View.GONE);
            recyclerViewProducts.setVisibility(View.VISIBLE);
        } else {
            textViewEmptyList.setVisibility(View.VISIBLE);
            recyclerViewProducts.setVisibility(View.GONE);
        }
    }

    private void ouvrirEcranAjoutProduit() {
        Intent intent = new Intent(this, ProductsActivity.class);
        intent.putExtra("list_id", listId);
        startActivityForResult(intent, REQUEST_ADD_PRODUCT);
    }

    public void copyList(View view) {
        afficherDialogueCopie();
    }

    private void afficherDialogueCopie() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_copy_list, null);
        EditText editTextListName = view.findViewById(R.id.editTextListName);

        // Proposer un nom par défaut
        String defaultName = "Copie de " + productList.getNom();
        editTextListName.setText(defaultName);

        builder.setView(view)
               .setTitle(R.string.copy_list)
               .setPositiveButton(R.string.copy, (dialog, which) -> {
                   String newName = editTextListName.getText().toString().trim();
                   if (!newName.isEmpty()) {
                       effectuerCopieDeLaListe(newName);
                   } else {
                       Toast.makeText(this, "Nom de liste requis", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void effectuerCopieDeLaListe(String newName) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(listId, newName).get();

            if (response.isSuccess() && response.getData() != null) {
                Toast.makeText(this, "Liste copiée avec succès", Toast.LENGTH_SHORT).show();
                // Ouvrir la nouvelle liste
                Intent intent = new Intent(this, ListDetailActivity.class);
                intent.putExtra("list_id", response.getData().getId());
                startActivity(intent);
            } else {
                Toast.makeText(this,
                    "Erreur lors de la copie de la liste: " + response.getErrorMessage(),
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
    public void onProductClick(Product product) {
        // Afficher la boîte de dialogue pour ajouter le produit à la liste
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_product_quantity, null);
        EditText editTextQuantity = view.findViewById(R.id.editTextQuantity);
        editTextQuantity.setText(String.valueOf(product.getQuantite()));

        builder.setView(view)
               .setTitle("Quantité de " + product.getNom())
               .setPositiveButton("Ajouter", (dialog, which) -> {
                   try {
                       double quantity = Double.parseDouble(editTextQuantity.getText().toString());
                       ajouterProduitALaListe(product.getId(), quantity);
                   } catch (NumberFormatException e) {
                       Toast.makeText(this, "Quantité invalide", Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void ajouterProduitALaListe(String productId, double quantity) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.addProductToList(listId, productId, quantity).get();

            if (response.isSuccess() && response.getData() != null) {
                productList = response.getData();
                mettreAJourInterface();
                Toast.makeText(this, "Produit ajouté à la liste", Toast.LENGTH_SHORT).show();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            // Recharger la liste pour afficher le nouveau produit
            chargerDetailsDeLaListe();
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