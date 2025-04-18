package edu.bdeb.a17tplistproduits.ui.lists;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import edu.bdeb.a17tplistproduits.adapters.ProductListAdapter;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.ui.auth.LoginActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ListsActivity extends AppCompatActivity implements ProductListAdapter.OnListClickListener {

    private static final int REQUEST_ADD_LIST = 1;
    private static final int REQUEST_LIST_DETAIL = 2;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    private RecyclerView recyclerViewLists;
    private TextView textViewNoLists;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddList;

    private List<ProductList> productLists;
    private ProductListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lists);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.product_lists);
        }

        recyclerViewLists = findViewById(R.id.recyclerViewLists);
        textViewNoLists = findViewById(R.id.textViewNoLists);
        progressBar = findViewById(R.id.progressBar);
        fabAddList = findViewById(R.id.fabAddList);

        recyclerViewLists.setLayoutManager(new LinearLayoutManager(this));
        productLists = new ArrayList<>();
        adapter = new ProductListAdapter(productLists, this);
        recyclerViewLists.setAdapter(adapter);

        fabAddList.setOnClickListener(v -> ouvrirEcranAjoutListe());

        chargerListes();
    }

    private void chargerListes() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoLists.setVisibility(View.GONE);

        try {
            ApiClient.ApiResponse<List<ProductList>> response = apiClient.getLists().get();

            if (response.isSuccess() && response.getData() != null) {
                productLists.clear();
                productLists.addAll(response.getData());
                adapter.notifyDataSetChanged();

                if (productLists.isEmpty()) {
                    textViewNoLists.setVisibility(View.VISIBLE);
                    recyclerViewLists.setVisibility(View.GONE);
                } else {
                    textViewNoLists.setVisibility(View.GONE);
                    recyclerViewLists.setVisibility(View.VISIBLE);
                }
            } else {
                Toast.makeText(this,
                        getString(R.string.error_loading_lists) + ": " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
                textViewNoLists.setVisibility(View.VISIBLE);
                recyclerViewLists.setVisibility(View.GONE);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            textViewNoLists.setVisibility(View.VISIBLE);
            recyclerViewLists.setVisibility(View.GONE);
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void ouvrirEcranAjoutListe() {
        Intent intent = new Intent(this, AddListActivity.class);
        startActivityForResult(intent, REQUEST_ADD_LIST);
    }

    @Override
    public void onListClick(ProductList productList) {
        Intent intent = new Intent(this, ListDetailActivity.class);
        intent.putExtra("list_id", productList.getId());
        startActivityForResult(intent, REQUEST_LIST_DETAIL);
    }

    @Override
    public void onCopyListClick(ProductList productList) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            String newName = getString(R.string.list_copy, productList.getNom());
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(productList.getId(), newName).get();

            if (response.isSuccess() && response.getData() != null) {
                Toast.makeText(this, R.string.list_copied_success, Toast.LENGTH_SHORT).show();
                chargerListes();
            } else {
                Toast.makeText(this,
                        "Error copying list: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_ADD_LIST || requestCode == REQUEST_LIST_DETAIL) &&
                resultCode == RESULT_OK) {
            chargerListes();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            sessionManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}