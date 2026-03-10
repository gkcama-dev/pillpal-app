package com.pillpal.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.adapter.ProductAdapter;
import com.pillpal.app.databinding.FragmentProductListBinding;
import com.pillpal.app.model.Product;
import java.util.List;

public class ProductListFragment extends Fragment {
    private FragmentProductListBinding binding;
    private String categoryId;
    private FirebaseFirestore db;

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
        db = FirebaseFirestore.getInstance();
        binding.recyclerViewListing.setLayoutManager(new GridLayoutManager(getContext(), 2));
        loadProducts();
    }

    private void loadProducts() {
        if (categoryId == null) return;

        db.collection("products")
                .whereEqualTo("categoryId", categoryId) // Filter logic
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null || !isAdded()) return;

                    List<Product> productList = queryDocumentSnapshots.toObjects(Product.class);
                    ProductAdapter adapter = new ProductAdapter(productList, product -> {
                        // Product Details Activity එකට යාම මෙහි ලියන්න
                    });
                    binding.recyclerViewListing.setAdapter(adapter);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}