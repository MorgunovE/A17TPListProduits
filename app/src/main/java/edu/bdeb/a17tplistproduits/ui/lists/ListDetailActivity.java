package edu.bdeb.a17tplistproduits.ui.lists;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import edu.bdeb.a17tplistproduits.ui.products.AddProductToListActivity;
import edu.bdeb.a17tplistproduits.ui.products.ProductDetailActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ListDetailActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private String listId;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> products;
    private ProgressBar progressBar;
    private TextView textViewNoProducts;
    private ApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Récupérer l'ID de la liste et le nom
        listId = getIntent().getStringExtra("LIST_ID");
        String listName = getIntent().getStringExtra("LIST_NAME");

        if (listId == null) {
            Toast.makeText(this, R.string.error_loading_list, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Définir le titre
        setTitle(listName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialiser les vues
        recyclerView = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progressBar);
        textViewNoProducts = findViewById(R.id.textViewNoProducts);
        FloatingActionButton fabAddProduct = findViewById(R.id.fabAddProduct);

        // Configurer RecyclerView
        products = new ArrayList<>();
        adapter = new ProductAdapter(products, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Configurer le bouton d'ajout
        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(ListDetailActivity.this, AddProductToListActivity.class);
            intent.putExtra("LIST_ID", listId);
            startActivity(intent);
        });

        // Charger les détails de la liste
        loadListDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadListDetails();
    }

    private void loadListDetails() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        textViewNoProducts.setVisibility(View.GONE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.getList(listId).get();
            if (response.isSuccess()) {
                ProductList productList = response.getData();
                if (productList != null && productList.getProduits() != null && !productList.getProduits().isEmpty()) {
                    products.clear();
                    products.addAll(productList.getProduits());
                    adapter.notifyDataSetChanged();
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    textViewNoProducts.setVisibility(View.VISIBLE);
                }
            } else {
                textViewNoProducts.setVisibility(View.VISIBLE);
                Toast.makeText(this, R.string.error_loading_list, Toast.LENGTH_SHORT).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            textViewNoProducts.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.error_loading_list, Toast.LENGTH_SHORT).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getId());
        startActivity(intent);
    }
}