package edu.bdeb.a17tplistproduits.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        progressBar = findViewById(R.id.progressBar);

        buttonLogin.setOnClickListener(v -> loginUser());
        textViewRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String name = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString();

        if (name.isEmpty()) {
            editTextUsername.setError(getString(R.string.email_required));
            editTextUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError(getString(R.string.password_required));
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        new Thread(() -> {
            try {
                ApiClient.ApiResponse<String> response = apiClient.login(name, password).get();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    buttonLogin.setEnabled(true);

                    if (response.isSuccess() && response.getData() != null) {
                        String token = response.getData();
                        sessionManager.saveAuthToken(token);
                        Log.d("LoginActivity", "Token: " + token.substring(0, 15) + "...");

                        Intent intent = new Intent(LoginActivity.this, ListsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                getString(R.string.login_failed) + ": " + response.getErrorMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e("LoginActivity", "Login failed: " + response.getErrorMessage());
                    }
                });
            } catch (ExecutionException | InterruptedException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    buttonLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.network_error) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("LoginActivity", "Network error: " + e.getMessage(), e);
                });
            }
        }).start();
    }
}