package edu.bdeb.a17tplistproduits.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.ui.lists.ListsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText editTextName;
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

        // Initialize UI elements
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);

        // Set up click listeners
        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void registerUser() {
        // Get input values and trim whitespace
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        // Input validation
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
            editTextPassword.setError(getString(R.string.password_min_length));
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError(getString(R.string.passwords_dont_match));
            editTextConfirmPassword.requestFocus();
            return;
        }

        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        try {
            // Create JSON object for registration request
            JSONObject userJson = new JSONObject();
            userJson.put("nom", name);
            userJson.put("courriel", email);
            userJson.put("motDePasse", password);

            // Use Future<ApiResponse<>> pattern directly as in LoginActivity
            apiClient.register(name, password, email)
                    .thenAccept(response -> {
                        if (response.isSuccess() && response.getData() != null) {
                            // Registration successful, now attempt login
                            loginAfterRegistration(name, password);
                        } else {
                            // Registration failed
                            runOnUiThread(() -> {
                                Toast.makeText(
                                        RegisterActivity.this,
                                        getString(R.string.registration_failed) + ": " + response.getErrorMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                                progressBar.setVisibility(View.GONE);
                                buttonRegister.setEnabled(true);
                            });
                        }
                    })
                    .exceptionally(e -> {
                        Log.e(TAG, "Registration error", e);
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    RegisterActivity.this,
                                    getString(R.string.network_error) + ": " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                            progressBar.setVisibility(View.GONE);
                            buttonRegister.setEnabled(true);
                        });
                        return null;
                    });
        } catch (Exception e) {
            Log.e(TAG, "Registration error", e);
            Toast.makeText(
                    RegisterActivity.this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
            progressBar.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
        }
    }

    private void loginAfterRegistration(String name, String password) {
        // Create a handler to post results back to the UI thread
        final Handler handler = new Handler(Looper.getMainLooper());

        // Create a new thread for the login operation
        new Thread(() -> {
            try {
                // Get the response from the API client
                ApiClient.ApiResponse<String> response = apiClient.login(name, password).get();

                // Post the result back to the UI thread
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    buttonRegister.setEnabled(true);

                    if (response.isSuccess() && response.getData() != null) {
                        // The token is in response.getData() - this is a successful login
                        String token = response.getData();
                        Toast.makeText(
                                RegisterActivity.this,
                                R.string.registration_successful,
                                Toast.LENGTH_SHORT
                        ).show();

                        sessionManager.saveAuthToken(token);
                        Log.d(TAG, "Token saved: " + token.substring(0, 15) + "...");

                        // Navigate to main screen
                        Intent intent = new Intent(RegisterActivity.this, ListsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Actual login failure
                        Toast.makeText(
                                RegisterActivity.this,
                                getString(R.string.login_after_registration_failed) + ": " + response.getErrorMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        Log.e(TAG, "Login after registration failed: " + response.getErrorMessage());
                        navigateToLogin(); // Redirect to login page
                    }
                });
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Login after registration error", e);

                // Post the error back to the UI thread
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    buttonRegister.setEnabled(true);

                    Toast.makeText(
                            RegisterActivity.this,
                            getString(R.string.network_error) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    navigateToLogin(); // Redirect to login page
                });
            }
        }).start();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}