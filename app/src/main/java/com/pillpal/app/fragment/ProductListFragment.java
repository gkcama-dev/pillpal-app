package com.pillpal.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.pillpal.app.adapter.ProductAdapter;
import com.pillpal.app.databinding.FragmentProductListBinding;
import com.pillpal.app.viewModel.ProductViewModel;

public class ProductListFragment extends Fragment {
    private FragmentProductListBinding binding;
    private String categoryId;
    private ProductViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString("categoryId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProductListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        binding.recyclerViewListing.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Initialize adapter with Context
        ProductAdapter adapter = new ProductAdapter(getContext());
        binding.recyclerViewListing.setAdapter(adapter);

        if (categoryId != null) {
            viewModel.fetchProducts(categoryId);
        }

        viewModel.getProducts().observe(getViewLifecycleOwner(), productList -> {
            if (productList != null && binding != null) {
                // Update data using the new method
                adapter.setProductList(productList);

                // Handle clicks if needed
                adapter.setOnProductClickListener(product -> {
                    // Navigate to Product Detail Fragment
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}