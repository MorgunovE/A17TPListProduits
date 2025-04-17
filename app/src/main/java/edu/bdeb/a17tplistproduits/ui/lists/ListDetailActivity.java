package edu.bdeb.a17tplistproduits.ui.lists;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import edu.bdeb.a17tplistproduits.ui.products.ProductDetailActivity;
import edu.bdeb.a17tplistproduits.ui.products.ProductsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ListDetailActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private static final int REQUEST_ADD_PRODUCT = 1;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    private TextView textViewListName;
    private TextView textViewListDescription;
    private TextView textViewEmptyList;
    private RecyclerView recyclerViewProducts;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddProduct;

    private String listId;
    private ProductList productList;
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
        }

        // Initialiser les vues
        textViewListName = findViewById(R.id.textViewListName);
        textViewListDescription = findViewById(R.id.textViewListDescription);
        textViewEmptyList = findViewById(R.id.textViewEmptyList);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progressBar);
        fabAddProduct = findViewById(R.id.fabAddProduct);

        // Configurer le RecyclerView
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(new ArrayList<>(), this);
        recyclerViewProducts.setAdapter(adapter);

        // Récupérer l'ID de la liste
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
            chargerListeDetails();
        } else {
            Toast.makeText(this, R.string.list_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Configurer le FAB
        fabAddProduct.setOnClickListener(v -> ouvrirRechercheOuAjoutProduit());

        // Configurer le bouton de copie
        Button buttonCopyList = findViewById(R.id.buttonCopyList);
        buttonCopyList.setOnClickListener(v -> afficherDialogueCopie());
    }

    private void chargerListeDetails() {
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
            recyclerViewProducts.setVisibility(View.VISIBLE);
            textViewEmptyList.setVisibility(View.GONE);
        } else {
            recyclerViewProducts.setVisibility(View.GONE);
            textViewEmptyList.setVisibility(View.VISIBLE);
        }
    }

    private void ouvrirRechercheOuAjoutProduit() {
        Intent intent = new Intent(this, ProductsActivity.class);
        intent.putExtra("list_id", listId);
        startActivityForResult(intent, REQUEST_ADD_PRODUCT);
    }

    private void afficherDialogueCopie() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_copy_list, null);
        EditText editTextListName = view.findViewById(R.id.editTextListName);

        // Proposer un nom pour la copie
        String suggestedName = getString(R.string.list_copy, productList.getNom());
        editTextListName.setText(suggestedName);

        builder.setView(view)
               .setTitle(R.string.copy_list)
               .setPositiveButton(R.string.copy, (dialog, which) -> {
                   String nomCopie = editTextListName.getText().toString().trim();
                   if (!nomCopie.isEmpty()) {
                       copierListe(nomCopie);
                   } else {
                       Toast.makeText(ListDetailActivity.this,
                                      R.string.list_name_required,
                                      Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void copierListe(String nomCopie) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(listId, nomCopie).get();

            if (response.isSuccess() && response.getData() != null) {
                Toast.makeText(this, R.string.list_copied_success, Toast.LENGTH_SHORT).show();

                // Actualiser la liste des listes à l'activité précédente
                setResult(RESULT_OK);

                // Ouvrir la liste copiée
                Intent intent = new Intent(this, ListDetailActivity.class);
                intent.putExtra("list_id", response.getData().getId());
                startActivity(intent);
            } else {
                Toast.makeText(this,
                    "Erreur lors de la copie: " + response.getErrorMessage(),
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
        // Ouvrir les détails du produit
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            // Recharger les détails de la liste
            chargerListeDetails();
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