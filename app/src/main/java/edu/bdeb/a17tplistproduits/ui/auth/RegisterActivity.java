package edu.bdeb.a17tplistproduits.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword, editTextConfirmPassword, editTextEmail;
    private Button buttonRegister;
    private TextView textViewLogin;
    private ProgressBar progressBar;
    private ApiClient apiClient;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize SessionManager and ApiClient
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Initialize UI elements
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        editTextEmail = findViewById(R.id.editTextEmail);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);

        // Set click listener for register button
        buttonRegister.setOnClickListener(v -> {
            registerUser();
        });

        // Set click listener for login text
        textViewLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        // Get input values
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("Le nom d'utilisateur est requis");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Le mot de passe est requis");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.setError("Veuillez confirmer le mot de passe");
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Les mots de passe ne correspondent pas");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("L'email est requis");
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Call API for registration
        apiClient.register(username, password, email).thenAccept(response -> {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccess()) {
                    Toast.makeText(RegisterActivity.this, "Inscription réussie!", Toast.LENGTH_SHORT).show();

                    // Redirect to login
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Échec de l'inscription: " + response.getErrorMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}