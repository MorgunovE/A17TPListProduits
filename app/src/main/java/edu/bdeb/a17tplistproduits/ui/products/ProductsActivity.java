package edu.bdeb.a17tplistproduits.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ProductsActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private static final String TAG = "ProductsActivity";

    private String listId;
    private ApiClient apiClient;
    private SessionManager sessionManager;

    private SearchView searchView;
    private RecyclerView recyclerViewProducts;
    private ProgressBar progressBar;
    private TextView textViewNoProducts;
    private FloatingActionButton fabAddProduct;

    private ProductAdapter adapter;
    private List<Product> products = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        // Initialiser les services
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.search_products);
        }

        // Récupérer l'ID de la liste des extras (si disponible)
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
        }

        // Initialiser les vues
        searchView = findViewById(R.id.searchView);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progressBar);
        textViewNoProducts = findViewById(R.id.textViewNoProducts);
        fabAddProduct = findViewById(R.id.fabAddProduct);

        // Configurer le RecyclerView
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(products, this);
        recyclerViewProducts.setAdapter(adapter);

        // Configurer le SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchProducts(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2) {
                    searchProducts(newText);
                } else if (newText.isEmpty()) {
                    loadProducts(null);
                }
                return true;
            }
        });

        // Configurer le bouton d'ajout de produit
        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(ProductsActivity.this, AddProductActivity.class);
            if (listId != null) {
                intent.putExtra("list_id", listId);
            }
            startActivityForResult(intent, 1);
        });

        // Charger les produits initiaux
        loadProducts(null);
    }

    private void loadProducts(String searchTerm) {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewProducts.setVisibility(View.GONE);
        textViewNoProducts.setVisibility(View.GONE);

        try {
            ApiClient.ApiResponse<List<Product>> response = apiClient.getProducts(searchTerm).get();

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
                        "Erreur lors du chargement des produits: " + response.getErrorMessage(),
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

    private void searchProducts(String query) {
        loadProducts(query);
    }

    @Override
    public void onProductClick(Product product) {
        if (listId != null) {
            // Si nous avons un ID de liste, ajouter le produit à la liste
            Intent resultIntent = new Intent();
            resultIntent.putExtra("product_id", product.getId());
            resultIntent.putExtra("product_name", product.getNom());
            resultIntent.putExtra("product_quantity", product.getQuantite());
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            // Sinon, afficher les détails du produit
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.getId());
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Recharger les produits après l'ajout d'un nouveau produit
            loadProducts(null);
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