package edu.bdeb.a17tplistproduits.ui.products;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class ProductDetailActivity extends AppCompatActivity {

    private ApiClient apiClient;
    private SessionManager sessionManager;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);

    private TextView textViewProductName;
    private TextView textViewProductQuantity;
    private TextView textViewProductUnit;
    private TextView textViewProductPrice;
    private TextView textViewProductDescription;
    private Button buttonAddToList;
    private ProgressBar progressBar;

    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.product_details);
        }

        textViewProductName = findViewById(R.id.textViewProductName);
        textViewProductQuantity = findViewById(R.id.textViewProductQuantity);
        textViewProductUnit = findViewById(R.id.textViewProductUnit);
        textViewProductPrice = findViewById(R.id.textViewProductPrice);
        textViewProductDescription = findViewById(R.id.textViewProductDescription);
        buttonAddToList = findViewById(R.id.buttonAddToList);
        progressBar = findViewById(R.id.progressBar);

        if (getIntent().hasExtra("product_id")) {
            productId = getIntent().getStringExtra("product_id");
            loadProductDetails();
        } else {
            Toast.makeText(this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonAddToList.setOnClickListener(v -> showListSelectionDialog());
    }

    private void loadProductDetails() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<Product> response = apiClient.getProduct(productId).get();

            if (response.isSuccess() && response.getData() != null) {
                currentProduct = response.getData();
                updateUI();
            } else {
                Toast.makeText(this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, R.string.network_error + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void updateUI() {
        textViewProductName.setText(currentProduct.getNom());
        textViewProductQuantity.setText(String.valueOf(currentProduct.getQuantite()));
        textViewProductUnit.setText(currentProduct.getUnite());
        textViewProductPrice.setText(currencyFormat.format(currentProduct.getPrix()));

        if (currentProduct.getDescription() != null && !currentProduct.getDescription().isEmpty()) {
            textViewProductDescription.setText(currentProduct.getDescription());
        } else {
            textViewProductDescription.setVisibility(View.GONE);
            findViewById(R.id.textViewProductDescriptionLabel).setVisibility(View.GONE);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentProduct.getNom());
        }
    }

    private void showListSelectionDialog() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<List<ProductList>> response = apiClient.getLists().get();

            if (response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                List<ProductList> lists = response.getData();
                showListsDialog(lists);
            } else {
                Toast.makeText(this, "Aucune liste disponible", Toast.LENGTH_SHORT).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, R.string.network_error + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showListsDialog(List<ProductList> lists) {
        View view = getLayoutInflater().inflate(R.layout.dialog_select_list, null);
        Spinner spinnerLists = view.findViewById(R.id.spinnerLists);

        List<String> listNames = new ArrayList<>();
        for (ProductList list : lists) {
            listNames.add(list.getNom());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLists.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.select_list)
                .setView(view)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    int position = spinnerLists.getSelectedItemPosition();
                    if (position != -1) {
                        String listId = lists.get(position).getId();
                        showQuantityDialog(listId);
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void showQuantityDialog(String listId) {
        View view = getLayoutInflater().inflate(R.layout.dialog_product_quantity, null);
        EditText editTextQuantity = view.findViewById(R.id.editTextQuantity);

        editTextQuantity.setText(String.valueOf(currentProduct.getQuantite()));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.quantity_for, currentProduct.getNom()))
                .setView(view)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String quantityStr = editTextQuantity.getText().toString();
                    if (!quantityStr.isEmpty()) {
                        try {
                            double quantity = Double.parseDouble(quantityStr);
                            addProductToList(listId, quantity);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.invalid_quantity, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, R.string.invalid_quantity, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void addProductToList(String listId, double quantity) {
        progressBar.setVisibility(View.VISIBLE);
        buttonAddToList.setEnabled(false);

        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.addProductToList(listId, productId, quantity).get();

            if (response.isSuccess()) {
                Toast.makeText(this, R.string.product_added_to_list, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Erreur lors de l'ajout du produit: " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this, R.string.network_error + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonAddToList.setEnabled(true);
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