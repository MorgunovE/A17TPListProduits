package edu.bdeb.a17tplistproduits;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerViewLists;
    private FloatingActionButton fabAddList;
    private ProgressBar progressBarLists;
    private ProductListAdapter adapter;
    private ApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        initializeViews();
        setupRecyclerView();
        loadProductLists();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        recyclerViewLists = findViewById(R.id.recyclerViewLists);
        fabAddList = findViewById(R.id.fabAddList);
        progressBarLists = findViewById(R.id.progressBarLists);

        fabAddList.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddListActivity.class));
        });
    }

    private void setupRecyclerView() {
        adapter = new ProductListAdapter(new ArrayList<>(), this);
        recyclerViewLists.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLists.setAdapter(adapter);
    }

    private void loadProductLists() {
        progressBarLists.setVisibility(View.VISIBLE);

        try {
            apiClient.getLists().thenAccept(response -> runOnUiThread(() -> {
                progressBarLists.setVisibility(View.GONE);

                if (response.isSuccess() && response.getData() != null) {
                    adapter.updateLists(response.getData());
                } else {
                    Toast.makeText(MainActivity.this,
                            "Aucune liste trouvée", Toast.LENGTH_SHORT).show();
                }
            }));
        } catch (Exception e) {
            progressBarLists.setVisibility(View.GONE);
            Toast.makeText(this, "Erreur lors du chargement des listes: " +
                    e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProductLists(); // Recharger à chaque retour
    }
}