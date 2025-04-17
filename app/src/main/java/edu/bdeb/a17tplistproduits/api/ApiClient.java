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
    private static final String BASE_URL = "http://10.0.2.2:5000/api";
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final SessionManager sessionManager;

    public ApiClient(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    // Classe générique pour encapsuler les réponses API
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

    // Méthodes d'authentification
    public Future<ApiResponse<Boolean>> login(String username, String password) {
        return executor.submit(() -> {
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("username", username);
                requestBody.put("password", password);

                JSONObject response = sendRequest("POST", "/auth/login", requestBody.toString(), false);

                if (response.has("token")) {
                    sessionManager.saveAuthToken(response.getString("token"));
                    sessionManager.saveUserId(response.getString("userId"));
                    sessionManager.saveUsername(username);
                    return new ApiResponse<>(true);
                } else {
                    return new ApiResponse<>("Échec de la connexion");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la connexion", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<Boolean>> register(String username, String password, String email) {
        return executor.submit(() -> {
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("username", username);
                requestBody.put("password", password);
                requestBody.put("email", email);

                JSONObject response = sendRequest("POST", "/auth/register", requestBody.toString(), false);

                if (response.has("success") && response.getBoolean("success")) {
                    return new ApiResponse<>(true);
                } else {
                    String message = response.has("message") ? response.getString("message") : "Échec de l'inscription";
                    return new ApiResponse<>(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'inscription", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    // Méthodes de gestion des listes
    public Future<ApiResponse<List<ProductList>>> getLists() {
        return executor.submit(() -> {
            try {
                JSONObject response = sendRequest("GET", "/lists", null, true);

                if (response.has("lists")) {
                    JSONArray lists = response.getJSONArray("lists");
                    List<ProductList> productLists = new ArrayList<>();

                    for (int i = 0; i < lists.length(); i++) {
                        JSONObject listObj = lists.getJSONObject(i);
                        ProductList list = new ProductList();
                        list.setId(listObj.getString("_id"));
                        list.setNom(listObj.getString("nom"));

                        if (!listObj.isNull("description")) {
                            list.setDescription(listObj.getString("description"));
                        }

                        if (listObj.has("produits") && !listObj.isNull("produits")) {
                            JSONArray produitsArray = listObj.getJSONArray("produits");
                            List<Product> products = new ArrayList<>();

                            for (int j = 0; j < produitsArray.length(); j++) {
                                JSONObject produitObj = produitsArray.getJSONObject(j);
                                Product product = parseProductFromJson(produitObj);
                                products.add(product);
                            }

                            list.setProduits(products);
                        }

                        productLists.add(list);
                    }

                    return new ApiResponse<>(productLists);
                } else {
                    return new ApiResponse<>("Aucune liste trouvée");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du chargement des listes", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<ProductList>> getList(String listId) {
        return executor.submit(() -> {
            try {
                JSONObject response = sendRequest("GET", "/lists/" + listId, null, true);

                if (response.has("list")) {
                    JSONObject listObj = response.getJSONObject("list");
                    return new ApiResponse<>(response.toString());
                } else {
                    return new ApiResponse<>("Liste non trouvée");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du chargement de la liste", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<ProductList>> createList(ProductList list) {
        return executor.submit(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("nom", list.getNom());

                if (list.getDescription() != null && !list.getDescription().isEmpty()) {
                    requestBody.put("description", list.getDescription());
                }

                JSONObject response = sendRequest("POST", "/lists", requestBody.toString(), true);

                if (response.has("list")) {
                    JSONObject listObj = response.getJSONObject("list");
                    ProductList createdList = new ProductList();
                    createdList.setId(listObj.getString("_id"));
                    createdList.setNom(listObj.getString("nom"));

                    if (!listObj.isNull("description")) {
                        createdList.setDescription(listObj.getString("description"));
                    }

                    return new ApiResponse<>(createdList);
                } else {
                    return new ApiResponse<>("Échec de la création de la liste");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la création de la liste", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<ProductList>> copyList(String listId, String newName) {
        return executor.submit(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("nom", newName);

                JSONObject response = sendRequest("POST", "/lists/" + listId + "/copy", requestBody.toString(), true);

                if (response.has("list")) {
                    JSONObject listObj = response.getJSONObject("list");
                    ProductList copiedList = new ProductList();
                    copiedList.setId(listObj.getString("_id"));
                    copiedList.setNom(listObj.getString("nom"));

                    if (!listObj.isNull("description")) {
                        copiedList.setDescription(listObj.getString("description"));
                    }

                    return new ApiResponse<>(copiedList);
                } else {
                    return new ApiResponse<>("Échec de la copie de la liste");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la copie de la liste", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    // Méthodes de gestion des produits
    public Future<ApiResponse<List<Product>>> getProducts() {
        return executor.submit(() -> {
            try {
                JSONObject response = sendRequest("GET", "/products", null, true);

                if (response.has("products")) {
                    JSONArray products = response.getJSONArray("products");
                    List<Product> productList = new ArrayList<>();

                    for (int i = 0; i < products.length(); i++) {
                        JSONObject productObj = products.getJSONObject(i);
                        Product product = parseProductFromJson(productObj);
                        productList.add(product);
                    }

                    return new ApiResponse<>(productList);
                } else {
                    return new ApiResponse<>("Aucun produit trouvé");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du chargement des produits", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<List<Product>>> searchProducts(String query) {
        return executor.submit(() -> {
            try {
                JSONObject response = sendRequest("GET", "/products/search?q=" + query, null, true);

                if (response.has("products")) {
                    JSONArray products = response.getJSONArray("products");
                    List<Product> productList = new ArrayList<>();

                    for (int i = 0; i < products.length(); i++) {
                        JSONObject productObj = products.getJSONObject(i);
                        Product product = parseProductFromJson(productObj);
                        productList.add(product);
                    }

                    return new ApiResponse<>(productList);
                } else {
                    return new ApiResponse<>("Aucun produit trouvé");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la recherche de produits", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<Product>> getProduct(String productId) {
        return executor.submit(() -> {
            try {
                JSONObject response = sendRequest("GET", "/products/" + productId, null, true);

                if (response.has("product")) {
                    JSONObject productObj = response.getJSONObject("product");
                    Product product = parseProductFromJson(productObj);
                    return new ApiResponse<>(product);
                } else {
                    return new ApiResponse<>("Produit non trouvé");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du chargement du produit", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<String>> createProduct(Product product) {
        return executor.submit(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("nom", product.getNom());
                requestBody.put("quantite", product.getQuantite());
                requestBody.put("unite", product.getUnite());
                requestBody.put("prix", product.getPrix());

                if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                    requestBody.put("description", product.getDescription());
                }

                JSONObject response = sendRequest("POST", "/products", requestBody.toString(), true);

                if (response.has("product") && response.getJSONObject("product").has("_id")) {
                    return new ApiResponse<>(response.getJSONObject("product").getString("_id"));
                } else {
                    return new ApiResponse<>("Échec de la création du produit");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la création du produit", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    public Future<ApiResponse<ProductList>> addProductToList(String listId, String productId, double quantity) {
        return executor.submit(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("productId", productId);
                requestBody.put("quantite", quantity);

                JSONObject response = sendRequest("POST", "/lists/" + listId + "/products", requestBody.toString(), true);

                if (response.has("list")) {
                    JSONObject listObj = response.getJSONObject("list");
                    ProductList updatedList = new ProductList();
                    updatedList.setId(listObj.getString("_id"));
                    updatedList.setNom(listObj.getString("nom"));

                    if (!listObj.isNull("description")) {
                        updatedList.setDescription(listObj.getString("description"));
                    }

                    return new ApiResponse<>(updatedList);
                } else {
                    return new ApiResponse<>("Échec de l'ajout du produit à la liste");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'ajout du produit à la liste", e);
                return new ApiResponse<>("Erreur: " + e.getMessage());
            }
        });
    }

    // Méthode utilitaire pour parser un produit depuis un JSONObject
    private Product parseProductFromJson(JSONObject productJson) throws JSONException {
        Product product = new Product();

        // Si c'est un produit complet
        if (productJson.has("_id")) {
            product.setId(productJson.getString("_id"));
            product.setNom(productJson.getString("nom"));
            product.setUnite(productJson.getString("unite"));
            product.setPrix(productJson.getDouble("prix"));

            if (!productJson.isNull("description")) {
                product.setDescription(productJson.getString("description"));
            }

            if (productJson.has("quantite")) {
                product.setQuantite(productJson.getDouble("quantite"));
            }
        }
        // Si c'est une référence à un produit dans une liste
        else if (productJson.has("produit") && !productJson.isNull("produit")) {
            JSONObject produitDetails = productJson.getJSONObject("produit");

            product.setId(produitDetails.getString("_id"));
            product.setNom(produitDetails.getString("nom"));
            product.setUnite(produitDetails.getString("unite"));
            product.setPrix(produitDetails.getDouble("prix"));

            if (!produitDetails.isNull("description")) {
                product.setDescription(produitDetails.getString("description"));
            }

            if (productJson.has("quantite")) {
                product.setQuantite(productJson.getDouble("quantite"));
            }
        }

        return product;
    }

    // Méthode générique pour envoyer des requêtes HTTP
    private JSONObject sendRequest(String method, String endpoint, String requestBody, boolean requireAuth) throws IOException, JSONException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        // Ajouter le token d'authentification si nécessaire
        if (requireAuth && sessionManager.isLoggedIn()) {
            connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getAuthToken());
        }

        // Ajouter le corps de la requête pour les méthodes POST et PUT
        if (requestBody != null && (method.equals("POST") || method.equals("PUT"))) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        // Récupérer la réponse
        int responseCode = connection.getResponseCode();

        // Lire le corps de la réponse
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ?
                connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        // Fermer la connexion
        connection.disconnect();

        // Retourner la réponse sous forme de JSONObject
        return new JSONObject(response.toString());
    }
}