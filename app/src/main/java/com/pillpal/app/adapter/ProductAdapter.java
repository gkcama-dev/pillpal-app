package com.pillpal.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.pillpal.app.R;
import com.pillpal.app.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> products;
    private OnProductClickListener listener;
    private final Context context;

    // Standard constructor that accepts only context
    public ProductAdapter(Context context) {
        this.context = context;
        this.products = new ArrayList<>(); // Initialize to avoid null pointer exceptions
    }

    // Constructor that accepts context and product list (for ProductListFragment)
    public ProductAdapter(Context context, List<Product> products, OnProductClickListener listener) {
        this.context = context;
        this.products = products != null ? products : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item_recycler, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);

        // Setting text data
        holder.name.setText(product.getName());
        holder.price.setText("LKR " + product.getPrice());

        String status = product.getStatus();
        if (status == null || status.isEmpty()) {
            status = "In Stock"; // Default value
        }

        holder.btnAvailable.setClickable(false);
        holder.btnAvailable.setFocusable(false);
        
        holder.btnAvailable.setText(status);

        switch (status) {
            case "In Stock":
                holder.btnAvailable.setText("Available");
                holder.btnAvailable.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8E8FFA")));
                holder.btnAvailable.setEnabled(true);
                break;

            case "Low Stock":
                holder.btnAvailable.setText("Low Stock");
                holder.btnAvailable.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA500"))); // Orange
                holder.btnAvailable.setEnabled(true);
                break;

            case "Out of Stock":
                holder.btnAvailable.setText("Out of Stock");
                holder.btnAvailable.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
                holder.btnAvailable.setEnabled(false); // Disable interaction
                break;

            default:
                holder.btnAvailable.setText("Available");
                holder.btnAvailable.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8E8FFA")));
                break;
        }

        // Handling image loading with Glide
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.medication) // Default icon while loading
                    .error(R.drawable.medication)       // Default icon on error
                    .centerCrop()
                    .into(holder.image);
        } else {
            // Set default image if URL is missing
            holder.image.setImageResource(R.drawable.medication);
        }

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    // Update product list dynamically from ViewModel
    @SuppressLint("NotifyDataSetChanged")
    public void setProductList(List<Product> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    // Set click listener from Fragment
    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, price;
        ImageView image;
        Button btnAvailable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_product_recycler_name);
            price = itemView.findViewById(R.id.item_product_recycler_price);
            image = itemView.findViewById(R.id.item_product_recycler_image);
            btnAvailable = itemView.findViewById(R.id.btn_add_request);
        }
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }
}