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
    private EditText editTextQuantity;
    private EditText editTextDescription;
    private Button buttonSaveProduct;
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

        editTextName = findViewById(R.id.editTextName);
        editTextPrice = findViewById(R.id.editTextPrice);
        editTextUnit = findViewById(R.id.editTextUnit);
        editTextQuantity = findViewById(R.id.editTextQuantity);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonSaveProduct = findViewById(R.id.buttonSaveProduct);
        progressBar = findViewById(R.id.progressBar);

        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
        }

        if (getIntent().getBooleanExtra("is_edit_mode", false)) {
            isEditMode = true;
            productId = getIntent().getStringExtra("product_id");

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.edit_product);
            }

            loadProductDetails();
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.add_product);
            }

            // Set default values
            editTextQuantity.setText("1");
            editTextUnit.setText(getString(R.string.default_unit));
        }

        buttonSaveProduct.setOnClickListener(v -> saveProduct());
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
                editTextQuantity.setText(String.valueOf(product.getQuantite()));

                if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                    editTextDescription.setText(product.getDescription());
                }
            } else {
                Toast.makeText(this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
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
        String priceString = editTextPrice.getText().toString().trim();
        String unit = editTextUnit.getText().toString().trim();
        String quantityString = editTextQuantity.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        if (name.isEmpty()) {
            editTextName.setError(getString(R.string.product_name_required));
            editTextName.requestFocus();
            return;
        }

        if (priceString.isEmpty()) {
            editTextPrice.setError(getString(R.string.product_price_required));
            editTextPrice.requestFocus();
            return;
        }

        if (unit.isEmpty()) {
            editTextUnit.setError(getString(R.string.product_unit_required));
            editTextUnit.requestFocus();
            return;
        }

        if (quantityString.isEmpty()) {
            editTextQuantity.setError(getString(R.string.product_quantity_required));
            editTextQuantity.requestFocus();
            return;
        }

        double price;
        double quantity;

        try {
            price = Double.parseDouble(priceString);
            if (price <= 0) {
                editTextPrice.setError(getString(R.string.invalid_price));
                editTextPrice.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            editTextPrice.setError(getString(R.string.invalid_price));
            editTextPrice.requestFocus();
            return;
        }

        try {
            quantity = Double.parseDouble(quantityString);
            if (quantity <= 0) {
                editTextQuantity.setError(getString(R.string.invalid_quantity));
                editTextQuantity.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            editTextQuantity.setError(getString(R.string.invalid_quantity));
            editTextQuantity.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSaveProduct.setEnabled(false);

        Product product = new Product();
        product.setNom(name);
        product.setPrix(price);
        product.setUnite(unit);
        product.setQuantite(quantity);
        if (!description.isEmpty()) {
            product.setDescription(description);
        }

        if (isEditMode) {
            product.setId(productId);
            updateProduct(product);
        } else {
            createProduct(product);
        }
    }

    private void createProduct(Product product) {
        try {
            ApiClient.ApiResponse<String> response = apiClient.createProduct(product).get();

            if (response.isSuccess() && response.getData() != null) {
                Toast.makeText(this, R.string.product_created, Toast.LENGTH_SHORT).show();

                if (listId != null) {
                    addProductToList(response.getData(), product.getQuantite());
                } else {
                    setResult(RESULT_OK);
                    finish();
                }
            } else {
                Toast.makeText(this,
                        getString(R.string.product_creation_error) + ": " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                buttonSaveProduct.setEnabled(true);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            buttonSaveProduct.setEnabled(true);
        }
    }

    private void updateProduct(Product product) {
        try {
            ApiClient.ApiResponse<Product> response = apiClient.updateProduct(product.getId(), product).get();

            if (response.isSuccess()) {
                Toast.makeText(this, R.string.product_updated, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this,
                        getString(R.string.product_update_error) + ": " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonSaveProduct.setEnabled(true);
        }
    }

    private void addProductToList(String productId, double quantity) {
        try {
            ApiClient.ApiResponse<?> response = apiClient.addProductToList(listId, productId, quantity).get();

            if (response.isSuccess()) {
                Toast.makeText(this, R.string.product_added_to_list, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
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
            progressBar.setVisibility(View.GONE);
            buttonSaveProduct.setEnabled(true);
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