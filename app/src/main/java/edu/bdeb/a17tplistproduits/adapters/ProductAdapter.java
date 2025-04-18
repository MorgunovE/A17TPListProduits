package edu.bdeb.a17tplistproduits.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.model.Product;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final List<Product> products;
    private final OnProductClickListener listener;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onDeleteProductClick(Product product);
        void onEditProductClick(Product product);
    }

    public ProductAdapter(List<Product> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewProductName;
        private final TextView textViewProductQuantity;
        private final TextView textViewProductPrice;
        private final ImageButton buttonDeleteProduct;
        private final ImageButton buttonEditProduct;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewProductName = itemView.findViewById(R.id.textViewProductName);
            textViewProductQuantity = itemView.findViewById(R.id.textViewProductQuantity);
            textViewProductPrice = itemView.findViewById(R.id.textViewProductPrice);
            buttonDeleteProduct = itemView.findViewById(R.id.buttonDeleteProduct);
            buttonEditProduct = itemView.findViewById(R.id.buttonEditProduct);

        }

        public void bind(Product product, OnProductClickListener listener) {
            textViewProductName.setText(product.getNom());

            String quantityText = product.getQuantite() + " " + product.getUnite();
            textViewProductQuantity.setText(quantityText);

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);
            textViewProductPrice.setText(currencyFormat.format(product.getPrix()));

            itemView.setOnClickListener(v -> listener.onProductClick(product));
            buttonDeleteProduct.setOnClickListener(v -> listener.onDeleteProductClick(product));
            buttonEditProduct.setOnClickListener(v -> listener.onEditProductClick(product));
        }
    }
}