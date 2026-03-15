package com.pillpal.app.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pillpal.app.R;
import com.pillpal.app.adapter.ProductAdapter;
import com.pillpal.app.databinding.FragmentHomeBinding;
import com.pillpal.app.viewModel.ProductViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ProductViewModel productViewModel;
    private ProductAdapter adapter;

    public HomeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        binding.rvRecentProducts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // Updated constructor call
        adapter = new ProductAdapter(getContext());
        binding.rvRecentProducts.setAdapter(adapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        setupRecyclerView();
        productViewModel.fetchRecentProducts();

        productViewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null && !products.isEmpty()) {
                adapter.setProductList(products);

                Log.d("HomeFragment", "Products loaded: " + products.size());
            } else {
                Log.d("HomeFragment", "No products found");
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Avoid memory leaks by nullifying binding
        binding = null;
    }
}