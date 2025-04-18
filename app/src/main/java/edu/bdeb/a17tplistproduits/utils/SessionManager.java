package edu.bdeb.a17tplistproduits.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import edu.bdeb.a17tplistproduits.ui.auth.LoginActivity;

public class SessionManager {
    private static final String PREF_NAME = "ProductListApp";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final int PRIVATE_MODE = Context.MODE_PRIVATE;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void createLoginSession(String token, String userId, String username, String email) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }


    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }


    public String getUsername() {
        return pref.getString(KEY_USERNAME, null);
    }


    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, null);
    }


    public void checkLogin() {
        if (!isLoggedIn()) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }


    public void logout() {
        editor.clear();
        editor.commit();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    public void saveAuthToken(String token) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public void saveUserId(String userId) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public void saveUsername(String username) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public void saveToken(String token) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public void setLoggedIn(boolean b) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, b);
        editor.apply();
    }

    public String getToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }

    public void saveEmail(String email) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }
}