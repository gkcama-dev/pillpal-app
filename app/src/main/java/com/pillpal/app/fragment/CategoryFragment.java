package com.pillpal.app.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.adapter.CategoryAdapter;
import com.pillpal.app.databinding.FragmentCategoryBinding;
import com.pillpal.app.model.Category;
import java.util.List;

public class CategoryFragment extends Fragment {
    private FragmentCategoryBinding binding;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        loadCategories();
    }

    private void loadCategories() {
        db.collection("categories").get().addOnCompleteListener(task -> {
            if (binding == null || !isAdded()) return;

            if (task.isSuccessful() && task.getResult() != null) {
                List<Category> categories = task.getResult().toObjects(Category.class);
                CategoryAdapter adapter = new CategoryAdapter(categories, category -> {

                    Bundle bundle = new Bundle();
                    bundle.putString("categoryId", category.getId());
                    ProductListFragment fragment = new ProductListFragment();
                    fragment.setArguments(bundle);

                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });
                binding.recyclerViewCategories.setAdapter(adapter);
            } else {
                Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}