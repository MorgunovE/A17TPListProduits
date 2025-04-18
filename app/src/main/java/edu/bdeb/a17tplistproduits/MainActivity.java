package edu.bdeb.a17tplistproduits;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
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

import edu.bdeb.a17tplistproduits.adapters.ProductListAdapter;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.ui.lists.AddListActivity;
import edu.bdeb.a17tplistproduits.ui.lists.ListDetailActivity;
import edu.bdeb.a17tplistproduits.ui.auth.LoginActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class MainActivity extends AppCompatActivity implements ProductListAdapter.OnListClickListener {

    private RecyclerView recyclerView;
    private ProductListAdapter adapter;
    private List<ProductList> productLists;
    private ProgressBar progressBar;
    private TextView textViewNoLists;
    private ApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setTitle(R.string.product_lists);

        recyclerView = findViewById(R.id.recyclerViewLists);
        progressBar = findViewById(R.id.progressBar);
        textViewNoLists = findViewById(R.id.textViewNoLists);
        FloatingActionButton fabAddList = findViewById(R.id.fabAddList);

        productLists = new ArrayList<>();
        adapter = new ProductListAdapter(productLists, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAddList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddListActivity.class);
            startActivity(intent);
        });

        loadLists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            loadLists();
        }
    }

    private void loadLists() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        textViewNoLists.setVisibility(View.GONE);

        try {
            ApiClient.ApiResponse<List<ProductList>> response = apiClient.getLists().get();
            if (response.isSuccess()) {
                List<ProductList> lists = response.getData();
                if (lists != null && !lists.isEmpty()) {
                    productLists.clear();
                    productLists.addAll(lists);
                    adapter.notifyDataSetChanged();
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    textViewNoLists.setVisibility(View.VISIBLE);
                }
            } else {
                textViewNoLists.setVisibility(View.VISIBLE);
                Toast.makeText(this, getString(R.string.error_loading_lists), Toast.LENGTH_SHORT).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            textViewNoLists.setVisibility(View.VISIBLE);
            Toast.makeText(this, getString(R.string.error_loading_lists), Toast.LENGTH_SHORT).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListClick(ProductList productList) {
        Intent intent = new Intent(this, ListDetailActivity.class);
        intent.putExtra("LIST_ID", productList.getId());
        intent.putExtra("LIST_NAME", productList.getNom());
        startActivity(intent);
    }

    @Override
    public void onCopyListClick(ProductList productList) {
        progressBar.setVisibility(View.VISIBLE);
        String newName = getString(R.string.list_copy, productList.getNom());

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(productList.getId(), newName).get();
            if (response.isSuccess()) {
                Toast.makeText(this, getString(R.string.list_copied_success), Toast.LENGTH_SHORT).show();
                loadLists();
            } else {
                Toast.makeText(this, getString(R.string.error_copying_list), Toast.LENGTH_SHORT).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, getString(R.string.error_copying_list), Toast.LENGTH_SHORT).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }
}