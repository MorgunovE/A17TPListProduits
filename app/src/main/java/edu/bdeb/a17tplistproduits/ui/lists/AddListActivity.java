package edu.bdeb.a17tplistproduits.ui.lists;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class AddListActivity extends AppCompatActivity {
    private static final String TAG = "AddListActivity";

    private EditText editTextListName;
    private EditText editTextListDescription;
    private Button buttonCreateList;
    private ProgressBar progressBar;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_list);

        // Initialiser les services
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.add_list);
            }
        }

        // Initialiser les vues
        editTextListName = findViewById(R.id.editTextListName);
        editTextListDescription = findViewById(R.id.editTextListDescription);
        buttonCreateList = findViewById(R.id.buttonCreateList);
        progressBar = findViewById(R.id.progressBar);

        // Configurer les écouteurs d'événements
        buttonCreateList.setOnClickListener(v -> creerNouvelleListe());
    }

    private void creerNouvelleListe() {
        String nom = editTextListName.getText().toString().trim();
        String description = editTextListDescription.getText().toString().trim();

        // Validation des entrées
        if (nom.isEmpty()) {
            editTextListName.setError(getString(R.string.list_name_required));
            editTextListName.requestFocus();
            return;
        }

        // Afficher la barre de progression
        progressBar.setVisibility(View.VISIBLE);
        buttonCreateList.setEnabled(false);

        // Création de l'objet ProductList
        ProductList newList = new ProductList(nom, description);

        // Appel à l'API pour créer la liste
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.createList(newList).get();

            if (response.isSuccess() && response.getData() != null) {
                // Succès, retourner à l'activité précédente avec le résultat
                Toast.makeText(this, getString(R.string.list_created_success), Toast.LENGTH_SHORT).show();

                // Ouvrir la liste nouvellement créée
                Intent resultIntent = new Intent();
                resultIntent.putExtra("list_id", response.getData().getId());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this,
                    getString(R.string.list_creation_error) + ": " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                getString(R.string.network_error) + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonCreateList.setEnabled(true);
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