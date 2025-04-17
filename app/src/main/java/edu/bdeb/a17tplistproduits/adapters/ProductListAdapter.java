package edu.bdeb.a17tplistproduits.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.bdeb.a17tplistproduits.R;
import edu.bdeb.a17tplistproduits.model.ProductList;

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {

    private final List<ProductList> productLists;
    private final OnListClickListener listener;

    public interface OnListClickListener {
        void onListClick(ProductList productList);
        void onCopyListClick(ProductList productList);
    }

    public ProductListAdapter(List<ProductList> productLists, OnListClickListener listener) {
        this.productLists = productLists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_product_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductList productList = productLists.get(position);
        holder.bind(productList, listener);
    }

    @Override
    public int getItemCount() {
        return productLists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewListName;
        private final TextView textViewListDescription;
        private final TextView textViewProductCount;
        private final ImageButton buttonCopyList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewListName = itemView.findViewById(R.id.textViewListName);
            textViewListDescription = itemView.findViewById(R.id.textViewListDescription);
            textViewProductCount = itemView.findViewById(R.id.textViewProductCount);
            buttonCopyList = itemView.findViewById(R.id.buttonCopyList);
        }

        public void bind(ProductList productList, OnListClickListener listener) {
            textViewListName.setText(productList.getNom());

            if (productList.getDescription() != null && !productList.getDescription().isEmpty()) {
                textViewListDescription.setText(productList.getDescription());
                textViewListDescription.setVisibility(View.VISIBLE);
            } else {
                textViewListDescription.setVisibility(View.GONE);
            }

            int productCount = productList.getProduits() != null ? productList.getProduits().size() : 0;
            textViewProductCount.setText(String.valueOf(productCount));

            itemView.setOnClickListener(v -> listener.onListClick(productList));
            buttonCopyList.setOnClickListener(v -> listener.onCopyListClick(productList));
        }
    }
}