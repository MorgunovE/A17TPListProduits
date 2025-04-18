package edu.bdeb.a17tplistproduits.ui.lists;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.adapters.ProductAdapter;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.ui.products.ProductsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ListDetailActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private static final int REQUEST_ADD_PRODUCT = 1;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    private TextView textViewListName;
    private TextView textViewListDescription;
    private TextView textViewProductsTitle;
    private RecyclerView recyclerViewProducts;
    private TextView textViewEmptyList;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddProduct;
    private Button buttonCopyList;

    private String listId;
    private ProductList currentList;
    private List<Product> productList;
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        // Initialiser les services
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialiser les vues
        textViewListName = findViewById(R.id.textViewListName);
        textViewListDescription = findViewById(R.id.textViewListDescription);
        textViewProductsTitle = findViewById(R.id.textViewProductsTitle);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        textViewEmptyList = findViewById(R.id.textViewEmptyList);
        progressBar = findViewById(R.id.progressBar);
        fabAddProduct = findViewById(R.id.fabAddProduct);
        buttonCopyList = findViewById(R.id.buttonCopyList);

        // Configurer le RecyclerView
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList, this);
        recyclerViewProducts.setAdapter(adapter);

        // Récupérer l'ID de la liste
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
            chargerDetailsDeLaListe();
        } else {
            Toast.makeText(this, R.string.list_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Configurer les écouteurs
        fabAddProduct.setOnClickListener(v -> ouvrirEcranAjoutProduit());
        buttonCopyList.setOnClickListener(v -> afficherDialogueCopierListe());
    }

    private void chargerDetailsDeLaListe() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.getList(listId).get();

            if (response.isSuccess() && response.getData() != null) {
                // Remove the unnecessary conversion
                currentList = response.getData();
                mettreAJourInterface();
                chargerProduitsDeLaListe();
            } else {
                Toast.makeText(this, R.string.list_not_found, Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private ProductList convertStringToProductList(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            ProductList productList = new ProductList();

            productList.setId(jsonObject.getString("_id"));
            productList.setNom(jsonObject.getString("nom"));

            if (!jsonObject.isNull("description")) {
                productList.setDescription(jsonObject.getString("description"));
            }

            if (!jsonObject.isNull("produits") && jsonObject.has("produits")) {
                JSONArray produitsArray = jsonObject.getJSONArray("produits");
                List<Product> products = new ArrayList<>();

                for (int i = 0; i < produitsArray.length(); i++) {
                    JSONObject produitObject = produitsArray.getJSONObject(i);
                    Product product = new Product();

                    // Check if produit has reference to a product document
                    if (produitObject.has("produit") && !produitObject.isNull("produit")) {
                        JSONObject produitDetails = produitObject.getJSONObject("produit");
                        product.setId(produitDetails.getString("_id"));
                        product.setNom(produitDetails.getString("nom"));
                        product.setUnite(produitDetails.getString("unite"));
                        product.setPrix(produitDetails.getDouble("prix"));

                        if (!produitDetails.isNull("description")) {
                            product.setDescription(produitDetails.getString("description"));
                        }
                    }

                    // Get the quantity from the list item
                    if (produitObject.has("quantite")) {
                        product.setQuantite(produitObject.getDouble("quantite"));
                    }

                    products.add(product);
                }

                productList.setProduits(products);
            }

            return productList;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void mettreAJourInterface() {
        textViewListName.setText(currentList.getNom());

        if (currentList.getDescription() != null && !currentList.getDescription().isEmpty()) {
            textViewListDescription.setText(currentList.getDescription());
            textViewListDescription.setVisibility(View.VISIBLE);
        } else {
            textViewListDescription.setVisibility(View.GONE);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentList.getNom());
        }
    }

    private void chargerProduitsDeLaListe() {
        if (currentList.getProduits() != null && !currentList.getProduits().isEmpty()) {
            productList.clear();
            productList.addAll(currentList.getProduits());
            adapter.notifyDataSetChanged();

            textViewEmptyList.setVisibility(View.GONE);
            recyclerViewProducts.setVisibility(View.VISIBLE);
        } else {
            textViewEmptyList.setVisibility(View.VISIBLE);
            recyclerViewProducts.setVisibility(View.GONE);
        }

        progressBar.setVisibility(View.GONE);
    }

    private void ouvrirEcranAjoutProduit() {
        Intent intent = new Intent(this, ProductsActivity.class);
        intent.putExtra("list_id", listId);
        startActivityForResult(intent, REQUEST_ADD_PRODUCT);
    }

    private void afficherDialogueCopierListe() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_copy_list, null);
        EditText editTextListName = view.findViewById(R.id.editTextListName);

        // Suggérer un nom par défaut pour la copie
        String nomCopie = getString(R.string.list_copy, currentList.getNom());
        editTextListName.setText(nomCopie);

        builder.setView(view)
               .setTitle(R.string.copy_list)
               .setPositiveButton(R.string.copy, (dialog, which) -> {
                   String nouveauNom = editTextListName.getText().toString().trim();
                   if (!nouveauNom.isEmpty()) {
                       copierListe(nouveauNom);
                   } else {
                       Toast.makeText(this, R.string.list_name_required, Toast.LENGTH_SHORT).show();
                   }
               })
               .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void copierListe(String nouveauNom) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.copyList(listId, nouveauNom).get();

            if (response.isSuccess() && response.getData() != null) {
                Toast.makeText(this, R.string.list_copied_success, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this,
                    "Erreur lors de la copie: " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                getString(R.string.network_error) + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProductClick(Product product) {
        // Afficher les détails du produit (optionnel)
        // Pourrait être utilisé pour modifier la quantité ou supprimer le produit
    }

    public void copyList(View view) {
        afficherDialogueCopierListe();
    }

    @Override
    public void onDeleteProductClick(Product product) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(getString(R.string.confirm_delete_product, product.getNom()))
            .setPositiveButton(R.string.delete, (dialog, which) -> deleteProductFromList(product))
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void deleteProductFromList(Product product) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<Boolean> response = apiClient.deleteProduct(product.getId()).get();

            if (response.isSuccess() && Boolean.TRUE.equals(response.getData())) {
                Toast.makeText(this, R.string.product_deleted, Toast.LENGTH_SHORT).show();
                // Refresh the product list
                chargerDetailsDeLaListe();
            } else {
                Toast.makeText(this,
                    getString(R.string.delete_failed) + ": " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                getString(R.string.network_error) + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            // Rafraîchir la liste
            chargerDetailsDeLaListe();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_logout) {
            sessionManager.logout();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}