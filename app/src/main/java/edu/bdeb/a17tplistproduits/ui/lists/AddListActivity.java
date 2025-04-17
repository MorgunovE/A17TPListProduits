package edu.bdeb.a17tplistproduits.ui.lists;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class AddListActivity extends AppCompatActivity {

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

        // Initialiser les vues
        editTextListName = findViewById(R.id.editTextListName);
        editTextListDescription = findViewById(R.id.editTextListDescription);
        buttonCreateList = findViewById(R.id.buttonCreateList);
        progressBar = findViewById(R.id.progressBar);

        // Configurer l'écouteur du bouton de création
        buttonCreateList.setOnClickListener(v -> creerNouvelleListe());
    }

    private void creerNouvelleListe() {
        // Récupérer les données saisies
        String nomListe = editTextListName.getText().toString().trim();
        String descriptionListe = editTextListDescription.getText().toString().trim();

        // Valider le nom de la liste
        if (nomListe.isEmpty()) {
            editTextListName.setError(getString(R.string.list_name_required));
            editTextListName.requestFocus();
            return;
        }

        // Afficher la barre de progression et désactiver le bouton
        progressBar.setVisibility(View.VISIBLE);
        buttonCreateList.setEnabled(false);

        // Créer un nouvel objet ProductList
        ProductList nouvelleListe = new ProductList(nomListe, descriptionListe);

        // Envoyer la requête à l'API
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.createList(nouvelleListe).get();

            if (response.isSuccess() && response.getData() != null) {
                // Liste créée avec succès
                Toast.makeText(this, R.string.list_created_success, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                // Erreur lors de la création de la liste
                Toast.makeText(this,
                        getString(R.string.list_creation_error) + ": " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            // Erreur réseau
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            // Masquer la barre de progression et réactiver le bouton
            progressBar.setVisibility(View.GONE);
            buttonCreateList.setEnabled(true);
        }
    }
}