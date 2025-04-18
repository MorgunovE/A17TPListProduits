package edu.bdeb.a17tplistproduits.ui.products;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class AddProductActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextPrice;
    private EditText editTextUnit;
    private EditText editTextDescription;
    private Button buttonSave;
    private ProgressBar progressBar;

    private ApiClient apiClient;
    private SessionManager sessionManager;

    private String productId;
    private String listId;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        editTextName = findViewById(R.id.editTextProductName);
        editTextPrice = findViewById(R.id.editTextProductPrice);
        editTextUnit = findViewById(R.id.editTextProductUnit);
        editTextDescription = findViewById(R.id.editTextProductDescription);
        buttonSave = findViewById(R.id.buttonSaveProduct);
        progressBar = findViewById(R.id.progressBar);

        // Check if editing existing product
        if (getIntent().hasExtra("product_id")) {
            isEditMode = true;
            productId = getIntent().getStringExtra("product_id");
            loadProductDetails();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.edit_product);
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.add_product);
            }
        }

        // Get list ID if provided
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
        }

        buttonSave.setOnClickListener(v -> saveProduct());
    }

    private void loadProductDetails() {
        progressBar.setVisibility(View.VISIBLE);

        try {
            ApiClient.ApiResponse<Product> response = apiClient.getProduct(productId).get();

            if (response.isSuccess() && response.getData() != null) {
                Product product = response.getData();
                editTextName.setText(product.getNom());
                editTextPrice.setText(String.valueOf(product.getPrix()));
                editTextUnit.setText(product.getUnite());
                if (product.getDescription() != null) {
                    editTextDescription.setText(product.getDescription());
                }
            } else {
                Toast.makeText(this, R.string.error_loading_product, Toast.LENGTH_SHORT).show();
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

    private void saveProduct() {
        String name = editTextName.getText().toString().trim();
        String priceStr = editTextPrice.getText().toString().trim();
        String unit = editTextUnit.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        if (name.isEmpty()) {
            editTextName.setError(getString(R.string.product_name_required));
            editTextName.requestFocus();
            return;
        }

        if (priceStr.isEmpty()) {
            editTextPrice.setError(getString(R.string.product_price_required));
            editTextPrice.requestFocus();
            return;
        }

        if (unit.isEmpty()) {
            editTextUnit.setError(getString(R.string.product_unit_required));
            editTextUnit.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            editTextPrice.setError(getString(R.string.invalid_price));
            editTextPrice.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSave.setEnabled(false);

        Product product = new Product();
        product.setNom(name);
        product.setPrix(price);
        product.setUnite(unit);
        if (!description.isEmpty()) {
            product.setDescription(description);
        }

        try {
            ApiClient.ApiResponse<?> response;

            if (isEditMode) {
                product.setId(productId);
                response = apiClient.updateProduct(product.getId(), product).get();
            } else {
                response = apiClient.createProduct(product).get();
            }

            if (response.isSuccess()) {
                Toast.makeText(this, isEditMode ?
                    R.string.product_updated : R.string.product_created,
                    Toast.LENGTH_SHORT).show();

                // If list ID is provided, add product to list
                if (listId != null && !isEditMode && response.getData() instanceof Product) {
                    Product createdProduct = (Product) response.getData();
                    addProductToList(createdProduct.getId());
                } else {
                    setResult(RESULT_OK);
                    finish();
                }
            } else {
                Toast.makeText(this,
                    getString(isEditMode ? R.string.update_failed : R.string.creation_failed) +
                    ": " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                buttonSave.setEnabled(true);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                getString(R.string.network_error) + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            buttonSave.setEnabled(true);
        }
    }

    private void addProductToList(String productId) {
        try {
            ApiClient.ApiResponse<?> response = apiClient.addProductToList(listId, productId, 1).get();

            if (response.isSuccess()) {
                Toast.makeText(this, R.string.product_added_to_list, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                    getString(R.string.add_product_error) + ": " + response.getErrorMessage(),
                    Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                getString(R.string.network_error) + ": " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        } finally {
            setResult(RESULT_OK);
            finish();
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