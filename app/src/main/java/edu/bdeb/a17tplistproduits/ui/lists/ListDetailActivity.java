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
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.adapters.ProductAdapter;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.ui.products.ProductsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ListDetailActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    public static final String EXTRA_LIST_ID = "list_id";
    private static final int REQUEST_ADD_PRODUCT = 1;

    private ApiClient apiClient;
    private SessionManager sessionManager;
    private String listId;
    private ProductList currentList;

    // UI components
    private TextView textViewListName;
    private TextView textViewListDescription;
    private RecyclerView recyclerViewProducts;
    private ProgressBar progressBar;
    private TextView textViewEmptyList;
    private FloatingActionButton fabAddProduct;
    private Button buttonCopyList;

    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.list_details);

        // Initialize API client and session manager
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Get list ID from intent
        listId = getIntent().getStringExtra(EXTRA_LIST_ID);
        if (listId == null) {
            Toast.makeText(this, R.string.error_loading_list, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();
        setupRecyclerView();
        setupListeners();

        // Load list data
        loadListDetails();
    }

    private void initializeViews() {
        textViewListName = findViewById(R.id.textViewListName);
        textViewListDescription = findViewById(R.id.textViewListDescription);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progressBar);
        textViewEmptyList = findViewById(R.id.textViewEmptyList);
        fabAddProduct = findViewById(R.id.fabAddProduct);
        buttonCopyList = findViewById(R.id.buttonCopyList);
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(productList, this);
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewProducts.setAdapter(productAdapter);
    }

    private void setupListeners() {
        fabAddProduct.setOnClickListener(v -> navigateToProductSelection());
        buttonCopyList.setOnClickListener(v -> showCopyListDialog());
    }

    private void loadListDetails() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewProducts.setVisibility(View.GONE);
        textViewEmptyList.setVisibility(View.GONE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.getList(listId).get();
            if (response.isSuccess()) {
                currentList = response.getData();
                updateUI();
            } else {
                Toast.makeText(this, getString(R.string.error_loading_list) + ": " + response.getErrorMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, getString(R.string.error_loading_list) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void navigateToProductSelection() {
        Intent intent = new Intent(this, ProductsActivity.class);
        intent.putExtra(ProductsActivity.EXTRA_LIST_ID, listId);
        startActivityForResult(intent, REQUEST_ADD_PRODUCT);
    }

    private void showCopyListDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_copy_list, null);
        EditText editTextListName = view.findViewById(R.id.editTextListName);
        editTextListName.setText(getString(R.string.list_copy, currentList.getNom()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.copy_list)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String newName = editTextListName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        copyList(newName);
                    } else {
                        Toast.makeText(this, R.string.list_name_required, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void copyList(String newName) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(listId, newName).get();
            if (response.isSuccess()) {
                Toast.makeText(this, R.string.list_copied, Toast.LENGTH_SHORT).show();
                // Optionally navigate to the newly created list
                Intent intent = new Intent(this, ListDetailActivity.class);
                intent.putExtra(EXTRA_LIST_ID, response.getData().getId());
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.error_copying_list) + ": " + response.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, getString(R.string.error_copying_list) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProductClick(Product product) {
        // On peut ajouter une action lorsque l'utilisateur clique sur un produit
        // Par exemple, afficher les détails du produit ou permettre de modifier la quantité
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            // Recharger les détails de la liste après l'ajout d'un produit
            loadListDetails();
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