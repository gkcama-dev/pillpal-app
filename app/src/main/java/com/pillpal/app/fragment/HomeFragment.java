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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.adapter.ProductAdapter;
import com.pillpal.app.databinding.FragmentHomeBinding;
import com.pillpal.app.viewModel.ProductViewModel;
import com.pillpal.app.viewModel.ProfileViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ProductViewModel productViewModel;
    private ProfileViewModel profileViewModel;
    private ProductAdapter adapter;
    private FirebaseAuth mAuth;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        productViewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupRecyclerView();

        if (mAuth.getCurrentUser() != null) {
            profileViewModel.fetchUserProfile(mAuth.getCurrentUser().getUid());
        }
        observeViewModel();

        productViewModel.fetchRecentProducts();

    }


    private void observeViewModel() {

        profileViewModel.getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {

                if (user.getName() != null && !user.getName().isEmpty()) {
                    String fullName = user.getName().trim();
                    String firstName = fullName.split(" ")[0];

                    binding.tvWelcomeName.setText("Hello, " + firstName + " 👋");
                }

                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    Glide.with(this)
                            .load(user.getProfileImageUrl())
                            .placeholder(R.drawable.avatar)
                            .circleCrop()
                            .into(binding.ivProfileAvatar);
                }
            }
        });

        productViewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null && !products.isEmpty()) {
                adapter.setProductList(products);
                Log.d("HomeFragment", "Products loaded: " + products.size());
            } else {
                Log.d("HomeFragment", "No products found");
            }
        });
    }

    private void setupRecyclerView() {
        binding.rvRecentProducts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // Updated constructor call
        adapter = new ProductAdapter(getContext());
        binding.rvRecentProducts.setAdapter(adapter);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}