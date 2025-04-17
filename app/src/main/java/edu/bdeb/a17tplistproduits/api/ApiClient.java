package edu.bdeb.a17tplistproduits.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://127.0.0.1:5000"; // localhost
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Gson gson = new Gson();
    private final SessionManager sessionManager;

    public ApiClient(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    // Méthodes génériques pour les appels HTTP
    private <T> Future<ApiResponse<T>> executeRequest(Callable<ApiResponse<T>> callable) {
        return executor.submit(callable);
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String readErrorResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    // Authentification
    public Future<ApiResponse<String>> login(String username, String password) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/connexion");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonInputString = "{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(connection);
                    return new ApiResponse<>(true, gson.fromJson(response, LoginResponse.class).getAccessToken(), null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Login failed: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public Future<ApiResponse<Boolean>> register(String username, String password, String email) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/inscription");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonInputString = "{\"username\": \"" + username +
                                        "\", \"password\": \"" + password +
                                        "\", \"email\": \"" + email + "\"}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    return new ApiResponse<>(true, true, null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, false, "Registration failed: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Registration error", e);
                return new ApiResponse<>(false, false, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    // Produits
    public Future<ApiResponse<List<Product>>> getProducts(String searchTerm) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = BASE_URL + "/produits";
                if (searchTerm != null && !searchTerm.isEmpty()) {
                    endpoint += "?nom=" + searchTerm;
                }

                URL url = new URL(endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(connection);
                    Type listType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> products = gson.fromJson(response, listType);
                    return new ApiResponse<>(true, products, null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Failed to get products: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Get products error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public Future<ApiResponse<Product>> getProduct(String productId) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/produits/" + productId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(connection);
                    Product product = gson.fromJson(response, Product.class);
                    return new ApiResponse<>(true, product, null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Failed to get product: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Get product error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public Future<ApiResponse<Product>> addProduct(Product product) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/produits");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
                connection.setDoOutput(true);

                String jsonInputString = gson.toJson(product);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    String response = readResponse(connection);
                    Product createdProduct = gson.fromJson(response, Product.class);
                    return new ApiResponse<>(true, createdProduct, null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Failed to add product: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Add product error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    // Listes de produits
    public Future<ApiResponse<List<ProductList>>> getLists() {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/listes");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(connection);
                    Type listType = new TypeToken<List<ProductList>>(){}.getType();
                    List<ProductList> lists = gson.fromJson(response, listType);
                    return new ApiResponse<>(true, lists, null);
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // Si pas de liste trouvée, on renvoie une liste vide
                    return new ApiResponse<>(true, null, "Aucune liste trouvée");
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Failed to get lists: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Get lists error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public Future<ApiResponse<ProductList>> getList(String listId) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/listes/" + listId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(connection);
                    ProductList productList = gson.fromJson(response, ProductList.class);
                    return new ApiResponse<>(true, productList, null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Failed to get list: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Get list error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public Future<ApiResponse<ProductList>> createList(ProductList list) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/listes");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
                connection.setDoOutput(true);

                String jsonInputString = gson.toJson(list);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    String response = readResponse(connection);
                    ProductList createdList = gson.fromJson(response, ProductList.class);
                    return new ApiResponse<>(true, createdList, null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Failed to create list: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Create list error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public Future<ApiResponse<ProductList>> addProductToList(String listId, String productId, double quantity) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/listes/" + listId + "/produits");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
                connection.setDoOutput(true);

                String jsonInputString = "{\"produit_id\": \"" + productId + "\", \"quantite\": " + quantity + "}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String response = readResponse(connection);
                    ProductList updatedList = gson.fromJson(response, ProductList.class);
                    return new ApiResponse<>(true, updatedList, null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Failed to add product to list: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Add product to list error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public Future<ApiResponse<ProductList>> copyList(String listId, String newName) {
        return executeRequest(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/listes/" + listId + "/copier");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
                connection.setDoOutput(true);

                String jsonInputString = "{\"nom\": \"" + newName + "\"}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    String response = readResponse(connection);
                    ProductList copiedList = gson.fromJson(response, ProductList.class);
                    return new ApiResponse<>(true, copiedList, null);
                } else {
                    String errorResponse = readErrorResponse(connection);
                    return new ApiResponse<>(false, null, "Failed to copy list: " + errorResponse);
                }
            } catch (Exception e) {
                Log.e(TAG, "Copy list error", e);
                return new ApiResponse<>(false, null, "Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    public List<Object> searchProducts(String query) {
    }

    // Classe pour la réponse d'authentification
    private static class LoginResponse {
        private String access_token;

        public String getAccessToken() {
            return access_token;
        }
    }

    // Classe pour encapsuler les réponses API
    public static class ApiResponse<T> {
        private final boolean success;
        private final T data;
        private final String errorMessage;

        public ApiResponse(boolean success, T data, String errorMessage) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public T getData() {
            return data;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}