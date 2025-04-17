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

import edu.bdeb.a17tplistproduits.MainActivity;
import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextUsername;
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

        // Si déjà connecté, aller directement au MainActivity
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Initialisation des vues
        editTextUsername = findViewById(R.id.editTextUsername);
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
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validation des entrées
        if (username.isEmpty()) {
            editTextUsername.setError("Nom d'utilisateur requis");
            editTextUsername.requestFocus();
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
            ApiClient.ApiResponse<Boolean> response = apiClient.login(username, password).get();

            if (response.isSuccess()) {
                // Enregistrer les données de session
                sessionManager.saveToken(response.getData());
                sessionManager.saveUsername(username);
                sessionManager.setLoggedIn(true);

                // Naviguer vers l'écran principal
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this,
                    "Échec de connexion: " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(LoginActivity.this,
                "Erreur de connexion: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonLogin.setEnabled(true);
        }
    }
}