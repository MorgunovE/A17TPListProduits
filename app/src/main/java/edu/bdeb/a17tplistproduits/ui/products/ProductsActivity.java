package edu.bdeb.a17tplistproduits.ui.products;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
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
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ProductsActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private static final int REQUEST_ADD_PRODUCT = 1;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    private SearchView searchView;
    private RecyclerView recyclerViewProducts;
    private TextView textViewNoProducts;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddProduct;

    private ProductAdapter adapter;
    private List<Product> productList;
    private String listId;

    private List<Product> products;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.search_products);
        }

        searchView = findViewById(R.id.searchView);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        textViewNoProducts = findViewById(R.id.textViewNoProducts);
        progressBar = findViewById(R.id.progressBar);
        fabAddProduct = findViewById(R.id.fabAddProduct);

        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList, this);
        recyclerViewProducts.setAdapter(adapter);

        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                rechercherProduits(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 3) {
                    rechercherProduits(newText);
                } else if (newText.isEmpty()) {
                    chargerProduits("");
                }
                return true;
            }
        });

        fabAddProduct.setOnClickListener(v -> ouvrirEcranAjoutProduit());

        chargerProduits("");
    }

    private void rechercherProduits(String query) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<List<Product>> response = apiClient.searchProducts(query).get();

            if (response.isSuccess() && response.getData() != null) {
                mettreAJourListeProduits(response.getData());
            } else {
                Toast.makeText(ProductsActivity.this,
                        "Erreur lors de la recherche: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(ProductsActivity.this,
                    "Erreur réseau: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }


    private void chargerProduits(String query) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<List<Product>> response;
            if (query.isEmpty()) {
                response = apiClient.getProducts().get();
            } else {
                response = apiClient.searchProducts(query).get();
            }

            if (response.isSuccess() && response.getData() != null) {
                mettreAJourListeProduits(response.getData());
            } else {
                Toast.makeText(ProductsActivity.this,
                        "Erreur lors du chargement des produits: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(ProductsActivity.this,
                    "Erreur réseau: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void mettreAJourListeProduits(List<Product> products) {
        productList.clear();
        productList.addAll(products);
        adapter.notifyDataSetChanged();

        if (products.isEmpty()) {
            textViewNoProducts.setVisibility(View.VISIBLE);
            recyclerViewProducts.setVisibility(View.GONE);
        } else {
            textViewNoProducts.setVisibility(View.GONE);
            recyclerViewProducts.setVisibility(View.VISIBLE);
        }
    }

    private void ouvrirEcranAjoutProduit() {
        Intent intent = new Intent(this, AddProductActivity.class);
        if (listId != null) {
            intent.putExtra("list_id", listId);
        }
        startActivityForResult(intent, REQUEST_ADD_PRODUCT);
    }

    @Override
    public void onProductClick(Product product) {
        if (listId != null) {
            showQuantityDialog(product);
        } else {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.getId());
            startActivity(intent);
        }
    }

    private void showQuantityDialog(Product product) {
        View view = getLayoutInflater().inflate(R.layout.dialog_product_quantity, null);
        EditText editTextQuantity = view.findViewById(R.id.editTextQuantity);

        editTextQuantity.setText(String.valueOf(product.getQuantite()));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.quantity_for, product.getNom()))
                .setView(view)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String quantityStr = editTextQuantity.getText().toString();
                    if (!quantityStr.isEmpty()) {
                        try {
                            double quantity = Double.parseDouble(quantityStr);
                            addProductToList(product.getId(), quantity);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.invalid_quantity, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, R.string.invalid_quantity, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addProductToList(String productId, double quantity) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.addProductToList(listId, productId, quantity).get();

            if (response.isSuccess()) {
                Toast.makeText(this, R.string.product_added_to_list, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this,
                        getString(R.string.add_product_error) + ": " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDeleteProductClick(Product product) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(getString(R.string.confirm_delete_product, product.getNom()))
            .setPositiveButton(R.string.delete, (dialog, which) -> deleteProduct(product))
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void deleteProduct(Product product) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<Boolean> response = apiClient.deleteProduct(product.getId()).get();

            if (response.isSuccess() && Boolean.TRUE.equals(response.getData())) {
                Toast.makeText(this, R.string.product_deleted, Toast.LENGTH_SHORT).show();
                loadProducts();
            } else {
                Toast.makeText(this,
                    getString(R.string.delete_failed) + ": " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                getString(R.string.network_error) + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoProducts.setVisibility(View.GONE);
        recyclerViewProducts.setVisibility(View.GONE);

        try {
            ApiClient.ApiResponse<List<Product>> response = apiClient.getProducts().get();

            if (response.isSuccess() && response.getData() != null) {
                products.clear();
                products.addAll(response.getData());
                adapter.notifyDataSetChanged();

                if (products.isEmpty()) {
                    textViewNoProducts.setVisibility(View.VISIBLE);
                    recyclerViewProducts.setVisibility(View.GONE);
                } else {
                    textViewNoProducts.setVisibility(View.GONE);
                    recyclerViewProducts.setVisibility(View.VISIBLE);
                }
            } else {
                Toast.makeText(this,
                        getString(R.string.error_loading_products) + ": " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
                textViewNoProducts.setVisibility(View.VISIBLE);
                recyclerViewProducts.setVisibility(View.GONE);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            textViewNoProducts.setVisibility(View.VISIBLE);
            recyclerViewProducts.setVisibility(View.GONE);
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            chargerProduits("");
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