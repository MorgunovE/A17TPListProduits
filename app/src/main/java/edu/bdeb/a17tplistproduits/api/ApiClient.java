package edu.bdeb.a17tplistproduits.api;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://10.0.2.2:5000"; // For emulator connecting to localhost
    private final SessionManager sessionManager;

    public ApiClient(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    // Generic API response class
    public static class ApiResponse<T> {
        private final T data;
        private final String errorMessage;
        private final boolean success;

        public ApiResponse(T data) {
            this.data = data;
            this.errorMessage = null;
            this.success = true;
        }

        public ApiResponse(String errorMessage) {
            this.data = null;
            this.errorMessage = errorMessage;
            this.success = false;
        }

        public T getData() {
            return data;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    // Authentication methods
    public CompletableFuture<ApiResponse<String>> login(String username, String password) {
        CompletableFuture<ApiResponse<String>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<String>>() {
            @Override
            protected ApiResponse<String> doInBackground(Void... voids) {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("username", username);
                    jsonBody.put("password", password);

                    HttpURLConnection connection = createConnection("/connexion", "POST", false);
                    writeRequestBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        String token = response.getString("access_token");
                        return new ApiResponse<>(token);
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<String> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<String>> register(String username, String email, String password) {
        CompletableFuture<ApiResponse<String>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<String>>() {
            @Override
            protected ApiResponse<String> doInBackground(Void... voids) {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("username", username);
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);

                    HttpURLConnection connection = createConnection("/inscription", "POST", false);
                    writeRequestBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                        return new ApiResponse<>("Registration successful");
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<String> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    // Product methods
    public CompletableFuture<ApiResponse<List<Product>>> getProducts() {
        CompletableFuture<ApiResponse<List<Product>>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<List<Product>>>() {
            @Override
            protected ApiResponse<List<Product>> doInBackground(Void... voids) {
                try {
                    HttpURLConnection connection = createConnection("/produits", "GET", true);

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        JSONArray jsonArray = new JSONArray(readResponse(connection));
                        List<Product> products = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonProduct = jsonArray.getJSONObject(i);
                            products.add(parseProduct(jsonProduct));
                        }

                        return new ApiResponse<>(products);
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<List<Product>> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<List<Product>>> searchProducts(String query) {
        CompletableFuture<ApiResponse<List<Product>>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<List<Product>>>() {
            @Override
            protected ApiResponse<List<Product>> doInBackground(Void... voids) {
                try {
                    HttpURLConnection connection = createConnection("/produits?nom=" + query, "GET", true);

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        JSONArray jsonArray = new JSONArray(readResponse(connection));
                        List<Product> products = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonProduct = jsonArray.getJSONObject(i);
                            products.add(parseProduct(jsonProduct));
                        }

                        return new ApiResponse<>(products);
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<List<Product>> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<Product>> getProduct(String productId) {
        CompletableFuture<ApiResponse<Product>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<Product>>() {
            @Override
            protected ApiResponse<Product> doInBackground(Void... voids) {
                try {
                    HttpURLConnection connection = createConnection("/produits/" + productId, "GET", true);

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        JSONObject jsonProduct = new JSONObject(readResponse(connection));
                        return new ApiResponse<>(parseProduct(jsonProduct));
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<Product> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<String>> createProduct(Product product) {
        CompletableFuture<ApiResponse<String>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<String>>() {
            @Override
            protected ApiResponse<String> doInBackground(Void... voids) {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("nom", product.getNom());
                    jsonBody.put("quantite", product.getQuantite());
                    jsonBody.put("unite", product.getUnite());
                    jsonBody.put("prix", product.getPrix());

                    if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                        jsonBody.put("description", product.getDescription());
                    }

                    HttpURLConnection connection = createConnection("/produits", "POST", true);
                    writeRequestBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        return new ApiResponse<>(response.getString("_id"));
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<String> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    // List methods
    public CompletableFuture<ApiResponse<List<ProductList>>> getLists() {
        CompletableFuture<ApiResponse<List<ProductList>>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<List<ProductList>>>() {
            @Override
            protected ApiResponse<List<ProductList>> doInBackground(Void... voids) {
                try {
                    HttpURLConnection connection = createConnection("/listes", "GET", true);

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        JSONArray jsonArray = new JSONArray(readResponse(connection));
                        List<ProductList> lists = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonList = jsonArray.getJSONObject(i);
                            lists.add(parseProductList(jsonList));
                        }

                        return new ApiResponse<>(lists);
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<List<ProductList>> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<ProductList>> getList(String listId) {
        CompletableFuture<ApiResponse<ProductList>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<ProductList>>() {
            @Override
            protected ApiResponse<ProductList> doInBackground(Void... voids) {
                try {
                    HttpURLConnection connection = createConnection("/listes/" + listId, "GET", true);

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        JSONObject jsonList = new JSONObject(readResponse(connection));
                        return new ApiResponse<>(parseProductList(jsonList));
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<ProductList> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<ProductList>> createList(ProductList list) {
        CompletableFuture<ApiResponse<ProductList>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<ProductList>>() {
            @Override
            protected ApiResponse<ProductList> doInBackground(Void... voids) {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("nom", list.getNom());

                    if (list.getDescription() != null && !list.getDescription().isEmpty()) {
                        jsonBody.put("description", list.getDescription());
                    }

                    HttpURLConnection connection = createConnection("/listes", "POST", true);
                    writeRequestBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        return new ApiResponse<>(parseProductList(response));
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<ProductList> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<ProductList>> addProductToList(String listId, String productId, double quantity) {
        CompletableFuture<ApiResponse<ProductList>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<ProductList>>() {
            @Override
            protected ApiResponse<ProductList> doInBackground(Void... voids) {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("produit_id", productId);
                    jsonBody.put("quantite", quantity);

                    HttpURLConnection connection = createConnection("/listes/" + listId + "/produits", "POST", true);
                    writeRequestBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        return new ApiResponse<>(parseProductList(response));
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<ProductList> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<ProductList>> copyList(String listId, String newName) {
        CompletableFuture<ApiResponse<ProductList>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<ProductList>>() {
            @Override
            protected ApiResponse<ProductList> doInBackground(Void... voids) {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("nom", newName);

                    HttpURLConnection connection = createConnection("/listes/" + listId + "/copier", "POST", true);
                    writeRequestBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        return new ApiResponse<>(parseProductList(response));
                    } else {
                        String error = readErrorResponse(connection);
                        return new ApiResponse<>(error);
                    }
                } catch (Exception e) {
                    return new ApiResponse<>("Network error: " + e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<ProductList> response) {
                future.complete(response);
            }
        }.execute();

        return future;
    }

    // Helper methods
    private HttpURLConnection createConnection(String endpoint, String method, boolean requiresAuth) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        if (requiresAuth) {
            String token = sessionManager.getToken();
            if (token != null && !token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
        }

        return connection;
    }

    private void writeRequestBody(HttpURLConnection connection, String body) throws IOException {
        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    private String readErrorResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } catch (Exception e) {
            return "Unknown error: " + connection.getResponseCode();
        }
    }

    private Product parseProduct(JSONObject json) throws JSONException {
        Product product = new Product();
        product.setId(json.getString("_id"));
        product.setNom(json.getString("nom"));
        product.setQuantite(json.getDouble("quantite"));
        product.setUnite(json.getString("unite"));
        product.setPrix(json.getDouble("prix"));

        if (json.has("description") && !json.isNull("description")) {
            product.setDescription(json.getString("description"));
        }

        return product;
    }

    private ProductList parseProductList(JSONObject json) throws JSONException {
        ProductList productList = new ProductList();
        productList.setId(json.getString("_id"));
        productList.setNom(json.getString("nom"));

        if (json.has("description") && !json.isNull("description")) {
            productList.setDescription(json.getString("description"));
        }

        if (json.has("produits") && !json.isNull("produits")) {
            JSONArray produitsArray = json.getJSONArray("produits");
            List<Product> products = new ArrayList<>();

            for (int i = 0; i < produitsArray.length(); i++) {
                JSONObject produitObject = produitsArray.getJSONObject(i);
                Product product = new Product();

                product.setId(produitObject.getString("produit_id"));
                product.setNom(produitObject.getString("nom"));
                product.setQuantite(produitObject.getDouble("quantite"));
                product.setUnite(produitObject.getString("unite"));
                product.setPrix(produitObject.getDouble("prix"));

                products.add(product);
            }

            productList.setProduits(products);
        }

        return productList;
    }
}