package edu.bdeb.a17tplistproduits.ui.lists;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import edu.bdeb.a17tplistproduits.ui.products.ProductSearchActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ListDetailActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private static final int REQUEST_PRODUCT_SEARCH = 100;
    private static final String EXTRA_LIST_ID = "list_id";
    private static final String EXTRA_PRODUCT_ID = "product_id";

    private ApiClient apiClient;
    private String listId;
    private ProductList currentList;

    private TextView textViewListName;
    private TextView textViewListDescription;
    private RecyclerView recyclerViewProducts;
    private FloatingActionButton fabAddProduct;
    private ProgressBar progressBar;

    private ProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        // Récupérer l'ID de la liste depuis l'intent
        listId = getIntent().getStringExtra(EXTRA_LIST_ID);
        if (listId == null) {
            Toast.makeText(this, R.string.error_loading_list, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialisation
        apiClient = new ApiClient(new SessionManager(this));
        initViews();
        setupRecyclerView();

        // Charger les détails de la liste
        loadListDetails();

        // Configurer les listeners
        fabAddProduct.setOnClickListener(v -> openProductSearch());
    }

    private void initViews() {
        textViewListName = findViewById(R.id.textViewListName);
        textViewListDescription = findViewById(R.id.textViewListDescription);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        fabAddProduct = findViewById(R.id.fabAddProduct);
        progressBar = findViewById(R.id.progressBar);

        // Configuration de la toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.list_detail);
        }
    }

    private void setupRecyclerView() {
        adapter = new ProductAdapter(productList, this);
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewProducts.setAdapter(adapter);
    }

    private void loadListDetails() {
        showLoading(true);
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.getList(listId).get();

            if (response.isSuccess()) {
                currentList = response.getData();
                updateUI();
            } else {
                Toast.makeText(this, getString(R.string.error_loading_list) + ": " +
                        response.getErrorMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, getString(R.string.network_error) + ": " +
                    e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        } finally {
            showLoading(false);
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

            // Mettre à jour la liste des produits
            productList.clear();
            if (currentList.getProduits() != null) {
                productList.addAll(currentList.getProduits());
            }
            adapter.notifyDataSetChanged();

            // Afficher un message si la liste est vide
            findViewById(R.id.textViewEmptyList).setVisibility(
                    productList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void openProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        startActivityForResult(intent, REQUEST_PRODUCT_SEARCH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PRODUCT_SEARCH && resultCode == RESULT_OK && data != null) {
            String productId = data.getStringExtra(EXTRA_PRODUCT_ID);
            if (productId != null) {
                showQuantityDialog(productId);
            }
        }
    }

    private void showQuantityDialog(String productId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_product_quantity, null);
        EditText editTextQuantity = view.findViewById(R.id.editTextQuantity);

        builder.setTitle(R.string.enter_quantity)
               .setView(view)
               .setPositiveButton(R.string.add, (dialog, which) -> {
                   String quantityStr = editTextQuantity.getText().toString();
                   if (!quantityStr.isEmpty()) {
                       double quantity = Double.parseDouble(quantityStr);
                       addProductToList(productId, quantity);
                   }
               })
               .setNegativeButton(R.string.cancel, null)
               .show();
    }

    private void addProductToList(String productId, double quantity) {
        showLoading(true);
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.addProductToList(listId, productId, quantity).get();

            if (response.isSuccess()) {
                currentList = response.getData();
                updateUI();
                Toast.makeText(this, R.string.product_added_to_list, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.error_adding_product) + ": " +
                        response.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, getString(R.string.network_error) + ": " +
                    e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            showLoading(false);
        }
    }

    public void copyList(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_copy_list, null);
        EditText editTextListName = dialogView.findViewById(R.id.editTextListName);

        // Suggérer un nom par défaut
        editTextListName.setText(getString(R.string.list_copy, currentList.getNom()));

        builder.setTitle(R.string.copy_list)
               .setView(dialogView)
               .setPositiveButton(R.string.copy, (dialog, which) -> {
                   String newName = editTextListName.getText().toString();
                   if (!newName.isEmpty()) {
                       performCopyList(newName);
                   }
               })
               .setNegativeButton(R.string.cancel, null)
               .show();
    }

    private void performCopyList(String newName) {
        showLoading(true);
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(listId, newName).get();

            if (response.isSuccess()) {
                Toast.makeText(this, R.string.list_copied, Toast.LENGTH_SHORT).show();

                // Ouvrir la nouvelle liste copiée
                Intent intent = new Intent(this, ListDetailActivity.class);
                intent.putExtra(EXTRA_LIST_ID, response.getData().getId());
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.error_copying_list) + ": " +
                        response.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, getString(R.string.network_error) + ": " +
                    e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            showLoading(false);
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(EXTRA_PRODUCT_ID, product.getId());
        startActivity(intent);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}