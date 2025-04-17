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
import java.util.List;
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
    private ProductList currentList;
    private ProductAdapter adapter;
    private List<Product> productList;

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
            getSupportActionBar().setTitle("");
        }

        // Initialiser les vues
        textViewListName = findViewById(R.id.textViewListName);
        textViewListDescription = findViewById(R.id.textViewListDescription);
        textViewEmptyList = findViewById(R.id.textViewEmptyList);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progressBar);
        fabAddProduct = findViewById(R.id.fabAddProduct);

        // Configurer RecyclerView
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList, this);
        recyclerViewProducts.setAdapter(adapter);

        // Récupérer l'ID de la liste depuis l'intent
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
            chargerDetailsDeLaListe();
        } else {
            Toast.makeText(this, R.string.list_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Configurer le FAB
        fabAddProduct.setOnClickListener(v -> ouvrirEcranAjoutProduit());
    }

    private void chargerDetailsDeLaListe() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.getList(listId).get();

            if (response.isSuccess() && response.getData() != null) {
                currentList = response.getData();
                mettreAJourInterface();
                chargerProduitsDeLaListe();
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
        textViewListName.setText(currentList.getNom());

        if (currentList.getDescription() != null && !currentList.getDescription().isEmpty()) {
            textViewListDescription.setText(currentList.getDescription());
            textViewListDescription.setVisibility(View.VISIBLE);
        } else {
            textViewListDescription.setVisibility(View.GONE);
        }
    }

    private void chargerProduitsDeLaListe() {
        if (currentList.getProduits() != null && !currentList.getProduits().isEmpty()) {
            productList.clear();
            productList.addAll(currentList.getProduits());
            adapter.notifyDataSetChanged();

            recyclerViewProducts.setVisibility(View.VISIBLE);
            textViewEmptyList.setVisibility(View.GONE);
        } else {
            recyclerViewProducts.setVisibility(View.GONE);
            textViewEmptyList.setVisibility(View.VISIBLE);
        }
    }

    private void ouvrirEcranAjoutProduit() {
        Intent intent = new Intent(this, ProductsActivity.class);
        intent.putExtra("list_id", listId);
        startActivityForResult(intent, REQUEST_ADD_PRODUCT);
    }

    public void copyList(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_copy_list, null);
        EditText editTextListName = dialogView.findViewById(R.id.editTextListName);

        // Suggérer un nom pour la copie
        String suggestedName = getString(R.string.list_copy, currentList.getNom());
        editTextListName.setText(suggestedName);
        editTextListName.selectAll();

        builder.setView(dialogView)
               .setTitle(R.string.copy_list)
               .setPositiveButton(R.string.copy, (dialog, which) -> {
                   String newListName = editTextListName.getText().toString().trim();
                   if (!newListName.isEmpty()) {
                       copierListe(newListName);
                   } else {
                       Toast.makeText(this, R.string.list_name_required, Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void copierListe(String newListName) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(listId, newListName).get();

            if (response.isSuccess() && response.getData() != null) {
                Toast.makeText(this, R.string.list_copied_success, Toast.LENGTH_SHORT).show();

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
        // Ouvrir le détail du produit
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            // Rafraîchir la liste après l'ajout d'un produit
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
}