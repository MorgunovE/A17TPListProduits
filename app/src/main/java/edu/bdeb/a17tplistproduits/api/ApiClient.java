package edu.bdeb.a17tplistproduits.api;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private static final String BASE_URL = "http://10.0.2.2:5000";
    private final ExecutorService executorService;
    private final SessionManager sessionManager;

    public ApiClient(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.executorService = Executors.newCachedThreadPool();
    }

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

        public String getData() {
            return (String) data;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    // ========== Authentication methods ==========

    public Future<ApiResponse<Boolean>> login(String username, String password) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/auth/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("username", username);
                jsonRequest.put("password", password);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());

                    if (response.has("token")) {
                        String token = response.getString("token");
                        String userId = response.getString("user_id");

                        // Save authentication token
                        sessionManager.saveUserSession(userId, token);
                        return new ApiResponse<>(true);
                    } else {
                        return new ApiResponse<>("Identifiants invalides");
                    }
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error during login: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<Boolean>> register(String username, String password, String email) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/auth/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("username", username);
                jsonRequest.put("password", password);
                jsonRequest.put("email", email);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    return new ApiResponse<>(true);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error during registration: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<Boolean>> logout() {
        return executorService.submit(() -> {
            sessionManager.clearSession();
            return new ApiResponse<>(true);
        });
    }

    // ========== Product List methods ==========

    public Future<ApiResponse<List<ProductList>>> getLists() {
        return executorService.submit(new Callable<ApiResponse<List<ProductList>>>() {
            @Override
            public ApiResponse<List<ProductList>> call() {
                try {
                    URL url = new URL(BASE_URL + "/lists");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        JSONObject response = new JSONObject(reader.readLine());
                        JSONArray listsArray = response.getJSONArray("lists");

                        ArrayList<ProductList> lists = new ArrayList<>();
                        for (int i = 0; i < listsArray.length(); i++) {
                            JSONObject listObject = listsArray.getJSONObject(i);
                            ProductList list = parseProductList(listObject);
                            lists.add(list);
                        }

                        return new ApiResponse<>(lists);
                    } else {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        JSONObject error = new JSONObject(reader.readLine());
                        return new ApiResponse<>(error.getString("message"));
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error fetching lists: " + e.getMessage());
                    return new ApiResponse<>("Erreur réseau: " + e.getMessage());
                }
            }
        });
    }

    public Future<ApiResponse<ProductList>> getList(String listId) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/lists/" + listId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());
                    JSONObject listObject = response.getJSONObject("list");

                    ProductList list = parseProductList(listObject);
                    return new ApiResponse<>(list);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching list details: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<ProductList>> createList(ProductList list) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/lists");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
                conn.setDoOutput(true);

                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("nom", list.getNom());
                if (list.getDescription() != null && !list.getDescription().isEmpty()) {
                    jsonRequest.put("description", list.getDescription());
                }

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());
                    JSONObject listObject = response.getJSONObject("list");

                    ProductList createdList = parseProductList(listObject);
                    return new ApiResponse<>(createdList);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error creating list: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<ProductList>> copyList(String listId, String newName) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/lists/" + listId + "/copy");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
                conn.setDoOutput(true);

                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("nom", newName);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());
                    JSONObject listObject = response.getJSONObject("list");

                    ProductList copiedList = parseProductList(listObject);
                    return new ApiResponse<>(copiedList);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error copying list: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    // ========== Product methods ==========

    public Future<ApiResponse<List<Product>>> getProducts() {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/products");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());
                    JSONArray productsArray = response.getJSONArray("products");

                    ArrayList<Product> products = new ArrayList<>();
                    for (int i = 0; i < productsArray.length(); i++) {
                        JSONObject productObject = productsArray.getJSONObject(i);
                        Product product = parseProduct(productObject);
                        products.add(product);
                    }

                    return new ApiResponse<>(products);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching products: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<List<Product>>> searchProducts(String query) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/products/search?q=" + query);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());
                    JSONArray productsArray = response.getJSONArray("products");

                    ArrayList<Product> products = new ArrayList<>();
                    for (int i = 0; i < productsArray.length(); i++) {
                        JSONObject productObject = productsArray.getJSONObject(i);
                        Product product = parseProduct(productObject);
                        products.add(product);
                    }

                    return new ApiResponse<>(products);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error searching products: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<Product>> getProduct(String productId) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/products/" + productId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());
                    JSONObject productObject = response.getJSONObject("product");

                    Product product = parseProduct(productObject);
                    return new ApiResponse<>(product);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error fetching product details: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<Product>> createProduct(Product product) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/products");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
                conn.setDoOutput(true);

                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("nom", product.getNom());
                jsonRequest.put("quantite", product.getQuantite());
                jsonRequest.put("unite", product.getUnite());
                jsonRequest.put("prix", product.getPrix());
                if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                    jsonRequest.put("description", product.getDescription());
                }

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());
                    JSONObject productObject = response.getJSONObject("product");

                    Product createdProduct = parseProduct(productObject);
                    return new ApiResponse<>(createdProduct);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error creating product: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<ProductList>> addProductToList(String listId, String productId, double quantity) {
        return executorService.submit(() -> {
            try {
                URL url = new URL(BASE_URL + "/lists/" + listId + "/products");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
                conn.setDoOutput(true);

                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("product_id", productId);
                jsonRequest.put("quantite", quantity);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject response = new JSONObject(reader.readLine());
                    JSONObject listObject = response.getJSONObject("list");

                    ProductList updatedList = parseProductList(listObject);
                    return new ApiResponse<>(updatedList);
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    JSONObject error = new JSONObject(reader.readLine());
                    return new ApiResponse<>(error.getString("message"));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error adding product to list: " + e.getMessage());
                return new ApiResponse<>("Erreur réseau: " + e.getMessage());
            }
        });
    }

    // ========== Helper methods ==========

    private ProductList parseProductList(JSONObject listObject) throws JSONException {
        ProductList list = new ProductList();
        list.setId(listObject.getString("_id"));
        list.setNom(listObject.getString("nom"));

        if (listObject.has("description") && !listObject.isNull("description")) {
            list.setDescription(listObject.getString("description"));
        }

        if (listObject.has("produits") && !listObject.isNull("produits")) {
            JSONArray produitsArray = listObject.getJSONArray("produits");
            ArrayList<Product> produits = new ArrayList<>();

            for (int i = 0; i < produitsArray.length(); i++) {
                JSONObject produitObject = produitsArray.getJSONObject(i);
                Product produit = parseProduct(produitObject);
                produits.add(produit);
            }

            list.setProduits(produits);
        }

        return list;
    }

    private Product parseProduct(JSONObject productObject) throws JSONException {
        Product product = new Product();
        product.setId(productObject.getString("_id"));
        product.setNom(productObject.getString("nom"));
        product.setQuantite(productObject.getDouble("quantite"));
        product.setUnite(productObject.getString("unite"));
        product.setPrix(productObject.getDouble("prix"));

        if (productObject.has("description") && !productObject.isNull("description")) {
            product.setDescription(productObject.getString("description"));
        }

        return product;
    }
}