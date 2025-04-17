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
import edu.bdeb.a17tplistproduits.ui.auth.LoginActivity;
import edu.bdeb.a17tplistproduits.ui.lists.AddListActivity;
import edu.bdeb.a17tplistproduits.ui.lists.ListDetailActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class MainActivity extends AppCompatActivity implements ProductListAdapter.OnListClickListener {

    private RecyclerView recyclerView;
    private ProductListAdapter adapter;
    private ProgressBar progressBar;
    private TextView textViewNoLists;
    private FloatingActionButton fabAddList;

    private ApiClient apiClient;
    private SessionManager sessionManager;
    private List<ProductList> productLists = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        // Vérifier si l'utilisateur est connecté
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        apiClient = new ApiClient(sessionManager);

        // Initialiser les vues
        recyclerView = findViewById(R.id.recyclerViewLists);
        progressBar = findViewById(R.id.progressBar);
        textViewNoLists = findViewById(R.id.textViewNoLists);
        fabAddList = findViewById(R.id.fabAddList);

        // Configurer le RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductListAdapter(productLists, this);
        recyclerView.setAdapter(adapter);

        // Configurer le bouton d'ajout
        fabAddList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddListActivity.class);
            startActivity(intent);
        });

        // Définir le titre avec le nom d'utilisateur
        String username = sessionManager.getUsername();
        if (username != null) {
            setTitle(getString(R.string.product_lists) + " - " + username);
        }

        // Charger les listes de l'utilisateur
        loadProductLists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les listes à chaque reprise de l'activité
        loadProductLists();
    }

    private void loadProductLists() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        textViewNoLists.setVisibility(View.GONE);

        try {
            apiClient.getLists().thenAccept(response -> {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (response.isSuccess()) {
                        productLists.clear();
                        List<ProductList> lists = response.getData();

                        if (lists != null && !lists.isEmpty()) {
                            productLists.addAll(lists);
                            adapter.notifyDataSetChanged();
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            textViewNoLists.setVisibility(View.VISIBLE);
                        }
                    } else {
                        textViewNoLists.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Erreur lors du chargement des listes: " +
                                response.getErrorMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }).exceptionally(e -> {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    textViewNoLists.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Erreur réseau: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
                return null;
            });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            textViewNoLists.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onListClick(ProductList productList) {
        Intent intent = new Intent(this, ListDetailActivity.class);
        intent.putExtra("LIST_ID", productList.getId());
        startActivity(intent);
    }

    @Override
    public void onCopyListClick(ProductList productList) {
        progressBar.setVisibility(View.VISIBLE);

        String newName = getString(R.string.list_copy, productList.getNom());

        try {
            apiClient.copyList(productList.getId(), newName).thenAccept(response -> {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (response.isSuccess()) {
                        Toast.makeText(this, "Liste copiée avec succès", Toast.LENGTH_SHORT).show();
                        loadProductLists();
                    } else {
                        Toast.makeText(this, "Erreur lors de la copie: " +
                                response.getErrorMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }).exceptionally(e -> {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Erreur réseau: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
                return null;
            });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            sessionManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}