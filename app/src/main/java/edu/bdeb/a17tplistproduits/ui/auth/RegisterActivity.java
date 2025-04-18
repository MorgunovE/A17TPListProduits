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

        // Initialize services
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Initialize views
        editTextUsername = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerUser() {
        // Get input values
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        // Validate input
        if (username.isEmpty()) {
            editTextUsername.setError(getString(R.string.username_required));
            editTextUsername.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            editTextEmail.setError(getString(R.string.email_required));
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError(getString(R.string.valid_email_required));
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            editTextPassword.setError(getString(R.string.password_too_short));
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError(getString(R.string.password_mismatch));
            editTextConfirmPassword.requestFocus();
            return;
        }

        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        // Register user
        try {
            ApiClient.ApiResponse<Boolean> response = apiClient.register(username, email, password).get();

            if (response.isSuccess()) {
                // Registration successful, now login
                Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                loginAfterRegistration(username, password);
            } else {
                // Registration failed
                Toast.makeText(this,
                    getString(R.string.registration_failed) + ": " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                buttonRegister.setEnabled(true);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                getString(R.string.network_error) + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
        }
    }

    private void loginAfterRegistration(String username, String password) {
        try {
            ApiClient.ApiResponse<String> response = apiClient.login(username, password).get();

            if (response.isSuccess() && response.getData() != null) {
                // Login successful
                String token = response.getData();
                sessionManager.saveToken(token);
                sessionManager.saveUsername(username);

                // Navigate to lists activity
                Intent intent = new Intent(this, ListsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                // Login failed
                Toast.makeText(this,
                    getString(R.string.auto_login_failed) + ": " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();

                // Go back to login screen
                finish();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                getString(R.string.network_error) + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            finish();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
        }
    }
}