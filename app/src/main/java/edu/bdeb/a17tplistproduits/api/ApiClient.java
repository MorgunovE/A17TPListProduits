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
    private static final String BASE_URL = "http://10.0.2.2:5000"; // Emulator localhost

    private final SessionManager sessionManager;

    public ApiClient(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static class ApiResponse<T> {
        private final T data;
        private final boolean success;
        private final String errorMessage;

        public ApiResponse(T data) {
            this.data = data;
            this.success = true;
            this.errorMessage = null;
        }

        public ApiResponse(String errorMessage) {
            this.data = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public T getData() {
            return data;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
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

                    String jsonResponse = performRequest(BASE_URL + "/connexion", "POST",
                            jsonBody.toString(), null);
                    JSONObject response = new JSONObject(jsonResponse);

                    if (response.has("access_token")) {
                        return new ApiResponse<>(response.getString("access_token"));
                    } else if (response.has("erreur")) {
                        return new ApiResponse<>(response.getString("erreur"));
                    } else {
                        return new ApiResponse<>("Unknown error occurred");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Login error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<String> result) {
                future.complete(result);
            }
        }.execute();

        return future;
    }

    public CompletableFuture<ApiResponse<Boolean>> register(String username, String password, String email) {
        CompletableFuture<ApiResponse<Boolean>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<Boolean>>() {
            @Override
            protected ApiResponse<Boolean> doInBackground(Void... voids) {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("username", username);
                    jsonBody.put("password", password);
                    jsonBody.put("email", email);

                    String jsonResponse = performRequest(BASE_URL + "/inscription", "POST",
                            jsonBody.toString(), null);
                    JSONObject response = new JSONObject(jsonResponse);

                    if (response.has("message") && response.getString("message").contains("réussie")) {
                        return new ApiResponse<>(true);
                    } else if (response.has("erreur")) {
                        return new ApiResponse<>(response.getString("erreur"));
                    } else {
                        return new ApiResponse<>("Unknown error occurred");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Registration error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<Boolean> result) {
                future.complete(result);
            }
        }.execute();

        return future;
    }

    // Product methods
    public CompletableFuture<ApiResponse<List<Product>>> getProducts() {
        return searchProducts("");
    }

    public CompletableFuture<ApiResponse<List<Product>>> searchProducts(String query) {
        CompletableFuture<ApiResponse<List<Product>>> future = new CompletableFuture<>();

        new AsyncTask<Void, Void, ApiResponse<List<Product>>>() {
            @Override
            protected ApiResponse<List<Product>> doInBackground(Void... voids) {
                try {
                    String endpoint = BASE_URL + "/produits";
                    if (query != null && !query.isEmpty()) {
                        endpoint += "?nom=" + query;
                    }

                    String token = sessionManager.getAuthToken();
                    String jsonResponse = performRequest(endpoint, "GET", null, token);

                    JSONArray jsonArray = new JSONArray(jsonResponse);
                    List<Product> products = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonProduct = jsonArray.getJSONObject(i);
                        Product product = new Product();
                        product.setId(jsonProduct.getString("_id"));
                        product.setNom(jsonProduct.getString("nom"));
                        product.setQuantite(jsonProduct.getDouble("quantite"));
                        product.setUnite(jsonProduct.getString("unite"));
                        product.setPrix(jsonProduct.getDouble("prix"));

                        if (jsonProduct.has("description") && !jsonProduct.isNull("description")) {
                            product.setDescription(jsonProduct.getString("description"));
                        }

                        products.add(product);
                    }

                    return new ApiResponse<>(products);
                } catch (Exception e) {
                    Log.e(TAG, "Search products error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<List<Product>> result) {
                future.complete(result);
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
                    String token = sessionManager.getAuthToken();
                    String jsonResponse = performRequest(BASE_URL + "/produits/" + productId, "GET", null, token);

                    JSONObject jsonProduct = new JSONObject(jsonResponse);
                    Product product = new Product();
                    product.setId(jsonProduct.getString("_id"));
                    product.setNom(jsonProduct.getString("nom"));
                    product.setQuantite(jsonProduct.getDouble("quantite"));
                    product.setUnite(jsonProduct.getString("unite"));
                    product.setPrix(jsonProduct.getDouble("prix"));

                    if (jsonProduct.has("description") && !jsonProduct.isNull("description")) {
                        product.setDescription(jsonProduct.getString("description"));
                    }

                    return new ApiResponse<>(product);
                } catch (Exception e) {
                    Log.e(TAG, "Get product error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<Product> result) {
                future.complete(result);
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

                    String token = sessionManager.getAuthToken();
                    String jsonResponse = performRequest(BASE_URL + "/produits", "POST", jsonBody.toString(), token);

                    JSONObject response = new JSONObject(jsonResponse);
                    if (response.has("_id")) {
                        return new ApiResponse<>(response.getString("_id"));
                    } else if (response.has("erreur")) {
                        return new ApiResponse<>(response.getString("erreur"));
                    } else {
                        return new ApiResponse<>("Unknown error occurred");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Create product error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<String> result) {
                future.complete(result);
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
                    String token = sessionManager.getAuthToken();
                    String jsonResponse = performRequest(BASE_URL + "/listes", "GET", null, token);

                    JSONArray jsonArray = new JSONArray(jsonResponse);
                    List<ProductList> lists = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonList = jsonArray.getJSONObject(i);
                        ProductList list = new ProductList();
                        list.setId(jsonList.getString("_id"));
                        list.setNom(jsonList.getString("nom"));

                        if (jsonList.has("description") && !jsonList.isNull("description")) {
                            list.setDescription(jsonList.getString("description"));
                        }

                        lists.add(list);
                    }

                    return new ApiResponse<>(lists);
                } catch (Exception e) {
                    Log.e(TAG, "Get lists error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<List<ProductList>> result) {
                future.complete(result);
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

                    String token = sessionManager.getAuthToken();
                    String jsonResponse = performRequest(BASE_URL + "/listes", "POST", jsonBody.toString(), token);

                    JSONObject jsonList = new JSONObject(jsonResponse);
                    ProductList createdList = new ProductList();
                    createdList.setId(jsonList.getString("_id"));
                    createdList.setNom(jsonList.getString("nom"));

                    if (jsonList.has("description") && !jsonList.isNull("description")) {
                        createdList.setDescription(jsonList.getString("description"));
                    }

                    return new ApiResponse<>(createdList);
                } catch (Exception e) {
                    Log.e(TAG, "Create list error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<ProductList> result) {
                future.complete(result);
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
                    String token = sessionManager.getAuthToken();
                    String jsonResponse = performRequest(BASE_URL + "/listes/" + listId, "GET", null, token);

                    JSONObject jsonList = new JSONObject(jsonResponse);
                    ProductList list = new ProductList();
                    list.setId(jsonList.getString("_id"));
                    list.setNom(jsonList.getString("nom"));

                    if (jsonList.has("description") && !jsonList.isNull("description")) {
                        list.setDescription(jsonList.getString("description"));
                    }

                    if (jsonList.has("produits") && !jsonList.isNull("produits")) {
                        JSONArray jsonProducts = jsonList.getJSONArray("produits");
                        List<Product> products = new ArrayList<>();

                        for (int i = 0; i < jsonProducts.length(); i++) {
                            JSONObject jsonProduct = jsonProducts.getJSONObject(i);
                            Product product = new Product();
                            product.setId(jsonProduct.getString("produit_id"));
                            product.setNom(jsonProduct.getString("nom"));
                            product.setQuantite(jsonProduct.getDouble("quantite"));
                            product.setUnite(jsonProduct.getString("unite"));
                            product.setPrix(jsonProduct.getDouble("prix"));
                            products.add(product);
                        }

                        list.setProduits(products);
                    }

                    return new ApiResponse<>(list);
                } catch (Exception e) {
                    Log.e(TAG, "Get list error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<ProductList> result) {
                future.complete(result);
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

                    String token = sessionManager.getAuthToken();
                    String jsonResponse = performRequest(BASE_URL + "/listes/" + listId + "/produits",
                            "POST", jsonBody.toString(), token);

                    JSONObject jsonList = new JSONObject(jsonResponse);
                    ProductList updatedList = new ProductList();
                    updatedList.setId(jsonList.getString("_id"));
                    updatedList.setNom(jsonList.getString("nom"));

                    if (jsonList.has("description") && !jsonList.isNull("description")) {
                        updatedList.setDescription(jsonList.getString("description"));
                    }

                    return new ApiResponse<>(updatedList);
                } catch (Exception e) {
                    Log.e(TAG, "Add product to list error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<ProductList> result) {
                future.complete(result);
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

                    String token = sessionManager.getAuthToken();
                    String jsonResponse = performRequest(BASE_URL + "/listes/" + listId + "/copier",
                            "POST", jsonBody.toString(), token);

                    JSONObject jsonList = new JSONObject(jsonResponse);
                    ProductList copiedList = new ProductList();
                    copiedList.setId(jsonList.getString("_id"));
                    copiedList.setNom(jsonList.getString("nom"));

                    if (jsonList.has("description") && !jsonList.isNull("description")) {
                        copiedList.setDescription(jsonList.getString("description"));
                    }

                    return new ApiResponse<>(copiedList);
                } catch (Exception e) {
                    Log.e(TAG, "Copy list error", e);
                    return new ApiResponse<>(e.getMessage());
                }
            }

            @Override
            protected void onPostExecute(ApiResponse<ProductList> result) {
                future.complete(result);
            }
        }.execute();

        return future;
    }

    // HTTP request helper
    private String performRequest(String urlStr, String method, String jsonBody, String token) throws IOException, JSONException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }

        // For POST/PUT requests
        if (jsonBody != null) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
        }

        // Read the response
        StringBuilder response = new StringBuilder();
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // If it's JSON, try to extract error message
            if (response.length() > 0) {
                try {
                    JSONObject errorResponse = new JSONObject(response.toString());
                    if (errorResponse.has("erreur")) {
                        throw new IOException(errorResponse.getString("erreur"));
                    }
                } catch (JSONException ignored) {
                    // If not JSON, just use the response as error message
                }
            }

            throw new IOException("HTTP error code: " + responseCode);
        }

        return response.toString();
    }
}