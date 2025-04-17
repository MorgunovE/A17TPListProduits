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

    /**
     * Enregistre les détails de la session utilisateur après connexion réussie
     *
     * @param token Token d'authentification
     * @param userId ID de l'utilisateur
     * @param username Nom d'utilisateur
     * @param email Email de l'utilisateur
     */
    public void createLoginSession(String token, String userId, String username, String email) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit();
    }

    /**
     * Vérifie si l'utilisateur est connecté
     *
     * @return true si l'utilisateur est connecté, false sinon
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Récupère le token d'authentification
     *
     * @return Le token d'authentification
     */
    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }

    /**
     * Récupère l'ID de l'utilisateur
     *
     * @return L'ID de l'utilisateur
     */
    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    /**
     * Récupère le nom d'utilisateur
     *
     * @return Le nom d'utilisateur
     */
    public String getUsername() {
        return pref.getString(KEY_USERNAME, null);
    }

    /**
     * Récupère l'email de l'utilisateur
     *
     * @return L'email de l'utilisateur
     */
    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    /**
     * Vérifie l'état de connexion et redirige vers LoginActivity si non connecté
     */
    public void checkLogin() {
        if (!isLoggedIn()) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * Déconnexion de l'utilisateur
     */
    public void logout() {
        editor.clear();
        editor.commit();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Save authentication token to shared preferences
     * @param token The JWT token to save
     */
    public void saveAuthToken(String token) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    /**
     * Save user ID to shared preferences
     * @param userId The user ID to save
     */
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

    public void saveToken(Boolean data) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_AUTH_TOKEN, data);
        editor.apply();
    }

    public void setLoggedIn(boolean b) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, b);
        editor.apply();
    }
}