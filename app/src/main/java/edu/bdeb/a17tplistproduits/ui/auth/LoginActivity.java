package edu.bdeb.a17tplistproduits.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.ui.lists.ListsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private ProgressBar progressBar;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Initialisation des vues
        editTextEmail = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        progressBar = findViewById(R.id.progressBar);

        // Configuration des écouteurs d'événements
        buttonLogin.setOnClickListener(v -> effectuerConnexion());
        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void effectuerConnexion() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validation des entrées
        if (email.isEmpty()) {
            editTextEmail.setError("Email requis");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Mot de passe requis");
            editTextPassword.requestFocus();
            return;
        }

        // Afficher la barre de progression
        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        // Tenter la connexion
        try {
            ApiClient.ApiResponse<String> response = apiClient.login(email, password).get();

            if (response.isSuccess()) {
                // Connexion réussie, navigation vers l'écran principal
                Intent intent = new Intent(LoginActivity.this, ListsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this,
                        "Échec de la connexion: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(LoginActivity.this,
                    "Erreur lors de la connexion: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonLogin.setEnabled(true);
        }
    }
}