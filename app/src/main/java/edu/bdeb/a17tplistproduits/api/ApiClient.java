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
    private static final String BASE_URL = "http://10.0.2.2:5000"; // Android emulator localhost
    private final SessionManager sessionManager;
    private final ExecutorService executorService;

    public ApiClient(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.executorService = Executors.newCachedThreadPool();
    }

    public static class ApiResponse<T> {
        private final T data;
        private final String errorMessage;
        private final boolean success;

        public ApiResponse(T data, String errorMessage, boolean success) {
            this.data = data;
            this.errorMessage = errorMessage;
            this.success = success;
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

    public Future<ApiResponse<String>> login(String username, String password) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", username);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return executorService.submit(new Callable<ApiResponse<String>>() {
            @Override
            public ApiResponse<String> call() {
                try {
                    HttpURLConnection connection = createConnection("/connexion", "POST", false);
                    writeBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == 200) {
                        JSONObject response = readJsonResponse(connection);
                        String token = response.getString("access_token");
                        return new ApiResponse<>(token, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Login error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<Boolean>> register(String username, String email, String password) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", username);
            jsonBody.put("email", email);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return executorService.submit(new Callable<ApiResponse<Boolean>>() {
            @Override
            public ApiResponse<Boolean> call() {
                try {
                    HttpURLConnection connection = createConnection("/inscription", "POST", false);
                    writeBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == 201) {
                        return new ApiResponse<>(true, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(false, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Registration error", e);
                    return new ApiResponse<>(false, e.getMessage(), false);
                }
            }
        });
    }

    // Product methods
    public Future<ApiResponse<List<Product>>> getProducts() {
        return executorService.submit(new Callable<ApiResponse<List<Product>>>() {
            @Override
            public ApiResponse<List<Product>> call() {
                try {
                    HttpURLConnection connection = createConnection("/produits", "GET", true);

                    if (connection.getResponseCode() == 200) {
                        JSONArray jsonArray = new JSONArray(readResponse(connection));
                        List<Product> products = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Product product = jsonToProduct(jsonObject);
                            products.add(product);
                        }
                        return new ApiResponse<>(products, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Get products error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<Product>> getProduct(String productId) {
        return executorService.submit(new Callable<ApiResponse<Product>>() {
            @Override
            public ApiResponse<Product> call() {
                try {
                    HttpURLConnection connection = createConnection("/produits/" + productId, "GET", true);

                    if (connection.getResponseCode() == 200) {
                        JSONObject jsonObject = new JSONObject(readResponse(connection));
                        Product product = jsonToProduct(jsonObject);
                        return new ApiResponse<>(product, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Get product error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<List<Product>>> searchProducts(String query) {
        return executorService.submit(new Callable<ApiResponse<List<Product>>>() {
            @Override
            public ApiResponse<List<Product>> call() {
                try {
                    HttpURLConnection connection = createConnection("/produits?nom=" + query, "GET", true);

                    if (connection.getResponseCode() == 200) {
                        JSONArray jsonArray = new JSONArray(readResponse(connection));
                        List<Product> products = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Product product = jsonToProduct(jsonObject);
                            products.add(product);
                        }
                        return new ApiResponse<>(products, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Search products error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<String>> createProduct(Product product) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("nom", product.getNom());
            jsonBody.put("quantite", product.getQuantite());
            jsonBody.put("unite", product.getUnite());
            jsonBody.put("prix", product.getPrix());
            if (product.getDescription() != null) {
                jsonBody.put("description", product.getDescription());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return executorService.submit(new Callable<ApiResponse<String>>() {
            @Override
            public ApiResponse<String> call() {
                try {
                    HttpURLConnection connection = createConnection("/produits", "POST", true);
                    writeBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == 201) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        String productId = response.getString("_id");
                        return new ApiResponse<>(productId, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Create product error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<List<ProductList>>> getLists() {
        return executorService.submit(new Callable<ApiResponse<List<ProductList>>>() {
            @Override
            public ApiResponse<List<ProductList>> call() {
                try {
                    HttpURLConnection connection = createConnection("/listes", "GET", true);

                    if (connection.getResponseCode() == 200) {
                        JSONArray jsonArray = new JSONArray(readResponse(connection));
                        List<ProductList> lists = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            ProductList list = jsonToProductList(jsonObject);
                            lists.add(list);
                        }
                        return new ApiResponse<>(lists, null, true);
                    } else if (connection.getResponseCode() == 404) {
                        // No lists found is not an error, return empty list
                        return new ApiResponse<>(new ArrayList<>(), null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Get lists error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<ProductList>> getList(String listId) {
        return executorService.submit(new Callable<ApiResponse<ProductList>>() {
            @Override
            public ApiResponse<ProductList> call() {
                try {
                    HttpURLConnection connection = createConnection("/listes/" + listId, "GET", true);

                    if (connection.getResponseCode() == 200) {
                        JSONObject jsonObject = new JSONObject(readResponse(connection));
                        ProductList list = jsonToProductList(jsonObject);
                        return new ApiResponse<>(list, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Get list error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<Boolean>> deleteProduct(String productId) {
        return executorService.submit(new Callable<ApiResponse<Boolean>>() {
            @Override
            public ApiResponse<Boolean> call() {
                try {
                    HttpURLConnection connection = createConnection("/produits/" + productId, "DELETE", true);

                    if (connection.getResponseCode() == 200) {
                        return new ApiResponse<>(true, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(false, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Delete product error", e);
                    return new ApiResponse<>(false, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<Product>> updateProduct(String productId, Product product) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("nom", product.getNom());
            jsonBody.put("quantite", product.getQuantite());
            jsonBody.put("unite", product.getUnite());
            jsonBody.put("prix", product.getPrix());

            if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                jsonBody.put("description", product.getDescription());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return executorService.submit(new Callable<ApiResponse<Product>>() {
            @Override
            public ApiResponse<Product> call() {
                try {
                    HttpURLConnection connection = createConnection("/produits/" + productId, "PUT", true);
                    writeBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == 200) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        Product updatedProduct = jsonToProduct(response);
                        return new ApiResponse<>(updatedProduct, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Update product error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<ProductList>> createList(ProductList list) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("nom", list.getNom());
            if (list.getDescription() != null && !list.getDescription().isEmpty()) {
                jsonBody.put("description", list.getDescription());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return executorService.submit(new Callable<ApiResponse<ProductList>>() {
            @Override
            public ApiResponse<ProductList> call() {
                try {
                    HttpURLConnection connection = createConnection("/listes", "POST", true);
                    writeBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == 201) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        ProductList createdList = jsonToProductList(response);
                        return new ApiResponse<>(createdList, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Create list error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }



    public Future<ApiResponse<ProductList>> addProductToList(String listId, String productId, double quantity) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("produit_id", productId);
            jsonBody.put("quantite", quantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return executorService.submit(new Callable<ApiResponse<ProductList>>() {
            @Override
            public ApiResponse<ProductList> call() {
                try {
                    HttpURLConnection connection = createConnection("/listes/" + listId + "/produits", "POST", true);
                    writeBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == 200) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        ProductList updatedList = jsonToProductList(response);
                        return new ApiResponse<>(updatedList, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Add product to list error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }

    public Future<ApiResponse<ProductList>> copyList(String listId, String newName) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("nom", newName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return executorService.submit(new Callable<ApiResponse<ProductList>>() {
            @Override
            public ApiResponse<ProductList> call() {
                try {
                    HttpURLConnection connection = createConnection("/listes/" + listId + "/copier", "POST", true);
                    writeBody(connection, jsonBody.toString());

                    if (connection.getResponseCode() == 201) {
                        JSONObject response = new JSONObject(readResponse(connection));
                        ProductList copiedList = jsonToProductList(response);
                        return new ApiResponse<>(copiedList, null, true);
                    } else {
                        String errorMessage = readErrorResponse(connection);
                        return new ApiResponse<>(null, errorMessage, false);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Copy list error", e);
                    return new ApiResponse<>(null, e.getMessage(), false);
                }
            }
        });
    }



    private HttpURLConnection createConnection(String endpoint, String method, boolean requireAuth) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        if (requireAuth && sessionManager.getToken() != null) {
            connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
        }

        if (method.equals("POST") || method.equals("PUT")) {
            connection.setDoOutput(true);
        }

        return connection;
    }

    private void writeBody(HttpURLConnection connection, String body) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine);
            }
            return response.toString();
        }
    }

    private String readErrorResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine);
            }
            JSONObject errorJson = new JSONObject(response.toString());
            return errorJson.optString("erreur", "Unknown error");
        } catch (Exception e) {
            return "Error reading error response: " + e.getMessage();
        }
    }

    private JSONObject readJsonResponse(HttpURLConnection connection) throws IOException, JSONException {
        return new JSONObject(readResponse(connection));
    }

    private Product jsonToProduct(JSONObject json) throws JSONException {
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

    private ProductList jsonToProductList(JSONObject json) throws JSONException {
        ProductList list = new ProductList();
        list.setId(json.getString("_id"));
        list.setNom(json.getString("nom"));

        if (json.has("description") && !json.isNull("description")) {
            list.setDescription(json.getString("description"));
        }

        if (json.has("produits") && !json.isNull("produits")) {
            JSONArray produitsArray = json.getJSONArray("produits");
            List<Product> products = new ArrayList<>();

            for (int i = 0; i < produitsArray.length(); i++) {
                JSONObject produitObject = produitsArray.getJSONObject(i);
                Product product = new Product();

                if (produitObject.has("produit_id")) {
                    product.setId(produitObject.getString("produit_id"));
                }

                product.setNom(produitObject.getString("nom"));
                product.setQuantite(produitObject.getDouble("quantite"));
                product.setUnite(produitObject.getString("unite"));
                product.setPrix(produitObject.getDouble("prix"));

                products.add(product);
            }

            list.setProduits(products);
        }

        return list;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}