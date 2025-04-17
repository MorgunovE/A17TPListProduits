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
import edu.bdeb.a17tplistproduits.model.User;
import edu.bdeb.a17tplistproduits.ui.lists.ListsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonRegister;
    private TextView textViewLogin;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize session manager and API client
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Initialize views
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void registerUser() {
        // Get input values
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty()) {
            editTextName.setError(getString(R.string.name_required));
            editTextName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            editTextEmail.setError(getString(R.string.email_required));
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError(getString(R.string.password_required));
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError(getString(R.string.password_length));
            editTextPassword.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        // Create user object
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);

        try {
            // Call API to register user String username, String password, String email
            ApiClient.ApiResponse<Boolean> response = apiClient.register(name, password, email).get();

            if (response.isSuccess() && response.getData() != null) {
                // Registration successful, now login the user directly
                loginAfterRegistration(email, password);
            } else {
                // Show error
                Toast.makeText(RegisterActivity.this,
                    "Registration failed: " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();

                progressBar.setVisibility(View.GONE);
                buttonRegister.setEnabled(true);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(RegisterActivity.this,
                "Network error: " + e.getMessage(),
                Toast.LENGTH_LONG).show();

            progressBar.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
        }
    }

    private void loginAfterRegistration(String email, String password) {
        try {
            // Call API to login
            ApiClient.ApiResponse<String> response = apiClient.login(email, password).get();

            if (response.isSuccess() && response.getData() != null) {
                // Save token and navigate to lists activity
                sessionManager.saveToken(response.getData());
                sessionManager.saveEmail(email);

                Intent intent = new Intent(RegisterActivity.this, ListsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // Login failed after registration
                Toast.makeText(RegisterActivity.this,
                    "Registration successful but login failed. Please try logging in manually.",
                    Toast.LENGTH_LONG).show();

                // Navigate to login page
                navigateToLogin();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(RegisterActivity.this,
                "Registration successful but login failed: " + e.getMessage(),
                Toast.LENGTH_LONG).show();

            // Navigate to login page
            navigateToLogin();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}