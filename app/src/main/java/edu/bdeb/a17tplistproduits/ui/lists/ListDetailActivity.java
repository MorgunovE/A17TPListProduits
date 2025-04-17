package edu.bdeb.a17tplistproduits.ui.lists;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

    private static final int REQUEST_ADD_PRODUCT = 100;

    private String listId;
    private ProductList currentList;

    private TextView textViewListName;
    private TextView textViewListDescription;
    private RecyclerView recyclerViewProducts;
    private TextView textViewEmptyList;
    private ProgressBar progressBar;
    private Button buttonCopyList;
    private FloatingActionButton fabAddProduct;

    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();

    private ApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        // Initialiser les vues
        initializeViews();

        // Initialiser les données
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Récupérer l'ID de la liste depuis l'intent
        listId = getIntent().getStringExtra("list_id");
        if (listId == null) {
            Toast.makeText(this, R.string.error_loading_list, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurer le RecyclerView
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(productList, this);
        recyclerViewProducts.setAdapter(productAdapter);

        // Charger les détails de la liste
        loadListDetails();

        // Configurer le bouton d'ajout de produit
        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(ListDetailActivity.this, ProductsActivity.class);
            intent.putExtra("list_id", listId);
            startActivityForResult(intent, REQUEST_ADD_PRODUCT);
        });

        // Configurer le bouton de copie de liste
        buttonCopyList.setOnClickListener(v -> showCopyListDialog());
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.list_details);

        textViewListName = findViewById(R.id.textViewListName);
        textViewListDescription = findViewById(R.id.textViewListDescription);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        textViewEmptyList = findViewById(R.id.textViewEmptyList);
        progressBar = findViewById(R.id.progressBar);
        buttonCopyList = findViewById(R.id.buttonCopyList);
        fabAddProduct = findViewById(R.id.fabAddProduct);
    }

    private void loadListDetails() {
        progressBar.setVisibility(View.VISIBLE);
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.getList(listId).get();
            if (response.isSuccess()) {
                currentList = response.getData();
                updateUI();
            } else {
                Toast.makeText(this, response.getErrorMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void updateUI() {
        if (currentList != null) {
            textViewListName.setText(currentList.getNom());

            if (currentList.getDescription() != null && !currentList.getDescription().isEmpty()) {
                textViewListDescription.setText(currentList.getDescription());
                textViewListDescription.setVisibility(View.VISIBLE);
            } else {
                textViewListDescription.setVisibility(View.GONE);
            }

            productList.clear();
            if (currentList.getProduits() != null && !currentList.getProduits().isEmpty()) {
                productList.addAll(currentList.getProduits());
                recyclerViewProducts.setVisibility(View.VISIBLE);
                textViewEmptyList.setVisibility(View.GONE);
            } else {
                recyclerViewProducts.setVisibility(View.GONE);
                textViewEmptyList.setVisibility(View.VISIBLE);
            }
            productAdapter.notifyDataSetChanged();
        }
    }

    private void showCopyListDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_copy_list, null);
        EditText editTextListName = dialogView.findViewById(R.id.editTextListName);

        // Proposer un nom par défaut
        String newName = getString(R.string.list_copy, currentList.getNom());
        editTextListName.setText(newName);

        new AlertDialog.Builder(this)
            .setTitle(R.string.copy_list)
            .setView(dialogView)
            .setPositiveButton(R.string.copy, (dialog, which) -> {
                String name = editTextListName.getText().toString().trim();
                if (!name.isEmpty()) {
                    copyList(name);
                } else {
                    Toast.makeText(ListDetailActivity.this,
                        R.string.error_empty_name, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void copyList(String newName) {
        progressBar.setVisibility(View.VISIBLE);
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(listId, newName).get();
            if (response.isSuccess()) {
                Toast.makeText(this, R.string.list_copied, Toast.LENGTH_SHORT).show();

                // Ouvrir la nouvelle liste
                Intent intent = new Intent(ListDetailActivity.this, ListDetailActivity.class);
                intent.putExtra("list_id", response.getData().getId());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, response.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            // Recharger la liste après l'ajout d'un produit
            loadListDetails();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}