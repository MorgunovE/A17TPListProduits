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
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextUsername;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewLogin;
    private ProgressBar progressBar;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Initialisation des vues
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);

        // Configuration des écouteurs d'événements
        buttonRegister.setOnClickListener(v -> effectuerInscription());
        textViewLogin.setOnClickListener(v -> {
            finish(); // Retour à l'écran de connexion
        });
    }

    private void effectuerInscription() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validation des entrées
        if (username.isEmpty()) {
            editTextUsername.setError("Nom d'utilisateur requis");
            editTextUsername.requestFocus();
            return;
        }

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

        if (confirmPassword.isEmpty() || !confirmPassword.equals(password)) {
            editTextConfirmPassword.setError("Les mots de passe ne correspondent pas");
            editTextConfirmPassword.requestFocus();
            return;
        }

        // Afficher la barre de progression
        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        // Tenter l'inscription
        try {
            ApiClient.ApiResponse<Boolean> response = apiClient.register(username, password, email).get();

            if (response.isSuccess()) {
                Toast.makeText(RegisterActivity.this,
                        "Inscription réussie. Veuillez vous connecter.",
                        Toast.LENGTH_LONG).show();
                finish(); // Retour à l'écran de connexion
            } else {
                Toast.makeText(RegisterActivity.this,
                        "Échec de l'inscription: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(RegisterActivity.this,
                    "Erreur lors de l'inscription: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
        }
    }
}