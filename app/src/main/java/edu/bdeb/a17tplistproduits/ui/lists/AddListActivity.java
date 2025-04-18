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

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        editTextListName = findViewById(R.id.editTextListName);
        editTextListDescription = findViewById(R.id.editTextListDescription);
        buttonCreateList = findViewById(R.id.buttonCreateList);
        progressBar = findViewById(R.id.progressBar);

        buttonCreateList.setOnClickListener(v -> creerNouvelleListe());
    }

    private void creerNouvelleListe() {
        String nomListe = editTextListName.getText().toString().trim();
        String descriptionListe = editTextListDescription.getText().toString().trim();

        if (nomListe.isEmpty()) {
            editTextListName.setError(getString(R.string.list_name_required));
            editTextListName.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonCreateList.setEnabled(false);

        ProductList nouvelleListe = new ProductList(nomListe, descriptionListe);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.createList(nouvelleListe).get();

            if (response.isSuccess() && response.getData() != null) {
                Toast.makeText(this, R.string.list_created_success, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
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
}