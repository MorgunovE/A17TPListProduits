package edu.bdeb.a17tplistproduits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.ui.auth.LoginActivity;
import edu.bdeb.a17tplistproduits.ui.lists.ListsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIMEOUT = 1500; // 1.5 seconds
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Delay for splash screen display, then check login state
        new Handler().postDelayed(this::checkLoginState, SPLASH_TIMEOUT);
    }

    private void checkLoginState() {
        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            // User is logged in, navigate to lists activity
            startActivity(new Intent(SplashActivity.this, ListsActivity.class));
        } else {
            // User is not logged in, navigate to login activity
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }

        // Close this activity
        finish();
    }
}