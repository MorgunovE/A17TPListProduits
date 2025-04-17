package edu.bdeb.a17tplistproduits.ui.products;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.concurrent.ExecutionException;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.api.ApiClient;
import edu.bdeb.a17tplistproduits.model.Product;
import edu.bdeb.a17tplistproduits.model.ProductList;
import edu.bdeb.a17tplistproduits.utils.SessionManager;

public class AddProductActivity extends AppCompatActivity {

    private TextView textViewTitle;
    private EditText editTextProductName;
    private EditText editTextProductQuantity;
    private EditText editTextProductUnit;
    private EditText editTextProductPrice;
    private EditText editTextProductDescription;
    private Button buttonAddProduct;
    private ProgressBar progressBar;

    private ApiClient apiClient;
    private SessionManager sessionManager;
    private String listId; // ID of the list to add the product to (optional)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Initialize services
        sessionManager = new SessionManager(this);
        apiClient = new ApiClient(sessionManager);

        // Initialize views
        textViewTitle = findViewById(R.id.textViewTitle);
        editTextProductName = findViewById(R.id.editTextProductName);
        editTextProductQuantity = findViewById(R.id.editTextProductQuantity);
        editTextProductUnit = findViewById(R.id.editTextProductUnit);
        editTextProductPrice = findViewById(R.id.editTextProductPrice);
        editTextProductDescription = findViewById(R.id.editTextProductDescription);
        buttonAddProduct = findViewById(R.id.buttonAddProduct);
        progressBar = findViewById(R.id.progressBar);

        // Get list ID if passed
        if (getIntent().hasExtra("list_id")) {
            listId = getIntent().getStringExtra("list_id");
        }

        // Set up button listener
        buttonAddProduct.setOnClickListener(v -> addProduct());
    }

    private void addProduct() {
        // Get input values
        String name = editTextProductName.getText().toString().trim();
        String quantityStr = editTextProductQuantity.getText().toString().trim();
        String unit = editTextProductUnit.getText().toString().trim();
        String priceStr = editTextProductPrice.getText().toString().trim();
        String description = editTextProductDescription.getText().toString().trim();

        // Validate inputs
        if (name.isEmpty()) {
            editTextProductName.setError(getString(R.string.product_name_required));
            editTextProductName.requestFocus();
            return;
        }

        if (quantityStr.isEmpty()) {
            editTextProductQuantity.setError(getString(R.string.quantity_required));
            editTextProductQuantity.requestFocus();
            return;
        }

        if (unit.isEmpty()) {
            editTextProductUnit.setError(getString(R.string.unit_required));
            editTextProductUnit.requestFocus();
            return;
        }

        if (priceStr.isEmpty()) {
            editTextProductPrice.setError(getString(R.string.price_required));
            editTextProductPrice.requestFocus();
            return;
        }

        double quantity;
        double price;

        try {
            quantity = Double.parseDouble(quantityStr);
        } catch (NumberFormatException e) {
            editTextProductQuantity.setError(getString(R.string.invalid_quantity));
            editTextProductQuantity.requestFocus();
            return;
        }

        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            editTextProductPrice.setError(getString(R.string.invalid_price));
            editTextProductPrice.requestFocus();
            return;
        }

        // Create Product object
        Product newProduct = new Product();
        newProduct.setNom(name);
        newProduct.setQuantite(quantity);
        newProduct.setUnite(unit);
        newProduct.setPrix(price);
        newProduct.setDescription(description);

        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        buttonAddProduct.setEnabled(false);

        // Send API request
        try {
            ApiClient.ApiResponse<Product> response = apiClient.createProduct(newProduct).get();

            if (response.isSuccess() && response.getData() != null) {
                Product createdProduct = response.getData();

                if (listId != null) {
                    // If list ID is provided, add product to this list
                    addProductToList(createdProduct.getId(), quantity);
                } else {
                    // Otherwise, just notify user that product was created
                    Toast.makeText(this, getString(R.string.product_created_success), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            } else {
                Toast.makeText(this,
                        getString(R.string.product_creation_error) + ": " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
                buttonAddProduct.setEnabled(true);
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            buttonAddProduct.setEnabled(true);
        }
    }

    private void addProductToList(String productId, double quantity) {
        try {
            ApiClient.ApiResponse<ProductList> response = apiClient.addProductToList(listId, productId, quantity).get();

            if (response.isSuccess()) {
                Toast.makeText(this, getString(R.string.product_added_to_list_success), Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this,
                        getString(R.string.product_add_to_list_error) + ": " + response.getErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(this,
                    getString(R.string.network_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } finally {
            progressBar.setVisibility(View.GONE);
            buttonAddProduct.setEnabled(true);
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