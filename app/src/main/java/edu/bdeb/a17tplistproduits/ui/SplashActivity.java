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

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        sessionManager = new SessionManager(this);

        new Handler().postDelayed(this::checkLoginState, SPLASH_TIMEOUT);
    }

    private void checkLoginState() {
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(SplashActivity.this, ListsActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }

        finish();
    }
}