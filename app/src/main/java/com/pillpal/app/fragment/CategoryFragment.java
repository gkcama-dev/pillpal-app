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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.adapter.CategoryAdapter;
import com.pillpal.app.databinding.FragmentCategoryBinding;
import com.pillpal.app.model.Category;
import com.pillpal.app.viewModel.CategoryViewModel;

import java.util.List;

public class CategoryFragment extends Fragment {
    private FragmentCategoryBinding binding;
    private CategoryViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // connect viewModel
        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // ViewModel (Observe)
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && binding != null) {
                CategoryAdapter adapter = new CategoryAdapter(categories, category -> {

                    // Category click -> ProductListFragment
                    Bundle bundle = new Bundle();
                    bundle.putString("categoryId", category.getId());
                    ProductListFragment fragment = new ProductListFragment();
                    fragment.setArguments(bundle);

                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null) // Back button
                            .commit();
                });
                binding.recyclerViewCategories.setAdapter(adapter);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}