package edu.bdeb.a17tplistproduits.ui.lists;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.adapters.ProductAdapter;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.ui.products.ProductDetailActivity;
import edu.bdeb.a17tplistproduits.ui.products.ProductsActivity;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ListDetailActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private static final int REQUEST_ADD_PRODUCT = 1;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    private TextView textViewListName;
    private TextView textViewListDescription;
    private RecyclerView recyclerViewProducts;
    private TextView textViewEmptyList;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddProduct;
    private Button buttonCopyList;

    private ProductAdapter adapter;
    private List<Product> productList;
    private String listId;
    private ProductList currentList;

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

        // Récupérer l'ID de la liste depuis l'intent
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
            chargerDetailsList();
        } else {
            Toast.makeText(this, "Erreur: ID de liste manquant", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Configurer les écouteurs
        fabAddProduct.setOnClickListener(v -> ouvrirEcranAjoutProduit());
        buttonCopyList.setOnClickListener(v -> afficherDialogueCopieList());
    }

    private void chargerDetailsList() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.getList(listId).get();

            if (response.isSuccess() && response.getData() != null) {
                currentList = response.getData();
                mettreAJourInterface();
            } else {
                Toast.makeText(this,
                        "Erreur lors du chargement de la liste: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    "Erreur réseau: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void mettreAJourInterface() {
        // Mettre à jour les informations de la liste
        textViewListName.setText(currentList.getNom());

        if (currentList.getDescription() != null && !currentList.getDescription().isEmpty()) {
            textViewListDescription.setText(currentList.getDescription());
            textViewListDescription.setVisibility(View.VISIBLE);
        } else {
            textViewListDescription.setVisibility(View.GONE);
        }

        // Mettre à jour la liste des produits
        if (currentList.getProduits() != null && !currentList.getProduits().isEmpty()) {
            productList.clear();
            productList.addAll(currentList.getProduits());
            adapter.notifyDataSetChanged();

            recyclerViewProducts.setVisibility(View.VISIBLE);
            textViewEmptyList.setVisibility(View.GONE);
        } else {
            recyclerViewProducts.setVisibility(View.GONE);
            textViewEmptyList.setVisibility(View.VISIBLE);
        }
    }

    private void ouvrirEcranAjoutProduit() {
        Intent intent = new Intent(this, ProductsActivity.class);
        intent.putExtra("list_id", listId);
        startActivityForResult(intent, REQUEST_ADD_PRODUCT);
    }

    private void afficherDialogueCopieList() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_copy_list, null);
        EditText editTextListName = view.findViewById(R.id.editTextListName);

        // Proposer un nom par défaut pour la copie
        String nomCopie = getString(R.string.list_copy, currentList.getNom());
        editTextListName.setText(nomCopie);

        builder.setView(view)
               .setTitle(R.string.copy_list)
               .setPositiveButton(R.string.add, (dialog, which) -> {
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
                Toast.makeText(this, "Liste copiée avec succès", Toast.LENGTH_SHORT).show();
                // Rafraîchir l'activité parente
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this,
                        "Erreur lors de la copie de la liste: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    "Erreur réseau: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PRODUCT && resultCode == RESULT_OK) {
            // Recharger la liste pour afficher les changements
            chargerDetailsList();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}