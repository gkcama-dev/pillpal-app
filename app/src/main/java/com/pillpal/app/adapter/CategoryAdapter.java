package com.pillpal.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.pillpal.app.R;
import com.pillpal.app.databinding.CategoryItemBinding;
import com.pillpal.app.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder>{

    private List<Category> categories;
    private OnCategoryClickListener listener;

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CategoryItemBinding binding = CategoryItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryAdapter.ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.binding.categoryName.setText(category.getName());

        Glide.with(holder.itemView.getContext())
                .load(category.getImageUrl())
                .placeholder(com.pillpal.app.R.drawable.medication)
                .centerCrop()
                .into(holder.binding.categoryImage);

        holder.itemView.setOnClickListener(v -> {
            Animation animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.click_animation);
            v.startAnimation(animation);
            if (listener != null) listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() { return categories.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CategoryItemBinding binding;
        public ViewHolder(CategoryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }
}
