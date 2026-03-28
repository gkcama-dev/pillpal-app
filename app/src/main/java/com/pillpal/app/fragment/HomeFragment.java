package com.pillpal.app.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.pillpal.app.viewModel.OrderViewModel;
import com.pillpal.app.viewModel.ProductViewModel;
import com.pillpal.app.viewModel.ProfileViewModel;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ProductViewModel productViewModel;
    private ProfileViewModel profileViewModel;
    private ProductAdapter adapter;
    private FirebaseAuth mAuth;
    private OrderViewModel orderViewModel;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        navigateToOrderRequestWithImage();
                    }
                }
            }
    );

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
        orderViewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);

        binding.btnUploadPrescription.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        setupRecyclerView();

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            profileViewModel.fetchUserProfile(uid);
            orderViewModel.fetchLastOrder(uid);
        }
        observeViewModel();
        productViewModel.fetchRecentProducts();

        // Pharmacy Location (Google Maps)
        binding.btnHomeLocation.setOnClickListener(v -> {
            double latitude = 6.7980039;
            double longitude = 79.8964259;
            String label = "PillPal Pharmacy";

            // Google Maps Intent
            String uri = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(" + label + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps"); //Google Maps app open

            // Error
            try {
                startActivity(intent);
            } catch (Exception e) {
                //If not Maps app Browser open
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                startActivity(browserIntent);
            }
        });

        // Call Us (Direct Dial)
        binding.btnHomeCall.setOnClickListener(v -> {
            String phoneNumber = "0112345678";

            // Permission
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                // If Has Permission
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
                startActivity(intent);

            } else {
                // Request Permission
                requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 101);

                // Optional `Dial Pad
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            }
        });

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


        orderViewModel.getLastOrderLiveData().observe(getViewLifecycleOwner(), lastOrder -> {

            binding.layoutLastOrderStatus.setVisibility(View.VISIBLE);

            if (lastOrder == null) {
                binding.tvLastOrderId.setText("Special Offer! 🎉");

                if (binding.tvWelcomeMsg != null) {
                    binding.tvWelcomeMsg.setText("Get your meds delivered fast.");
                }

                binding.tvLastOrderStatus.setText("Start Now");
                binding.tvLastOrderStatus.setTextColor(Color.parseColor("#7E57C2"));
                binding.tvLastOrderStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#157E57C2")));
                binding.indicatorStatus.setBackgroundColor(Color.parseColor("#E0E0E0"));
                binding.iconOrder.setImageResource(R.drawable.list_alt);


                binding.layoutLastOrderStatus.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    galleryLauncher.launch(intent);
                });

            } else {
                binding.tvLastOrderId.setText("#" + lastOrder.getOrderId());

                if (binding.tvWelcomeMsg != null) {
                    binding.tvWelcomeMsg.setText("Current Order Status");
                }

                String status = lastOrder.getStatus();
                binding.tvLastOrderStatus.setText(status);

                int color, bgColor;


                if ("Pending".equals(status)) {
                    color = getResources().getColor(R.color.orange);
                    bgColor = Color.parseColor("#15FF9800");
                } else if ("Approved".equals(status) || "Payment Done".equals(status)) {
                    color = getResources().getColor(R.color.blue);
                    bgColor = Color.parseColor("#152196F3");
                } else if ("Delivered".equals(status) || "Received".equals(status)) {
                    color = getResources().getColor(R.color.green);
                    bgColor = Color.parseColor("#154CAF50");
                } else {
                    color = getResources().getColor(R.color.primaryPurple);
                    bgColor = Color.parseColor("#15673AB7");
                }


                binding.tvLastOrderStatus.setTextColor(color);
                binding.tvLastOrderStatus.setBackgroundTintList(ColorStateList.valueOf(bgColor));
                binding.indicatorStatus.setBackgroundColor(color);
                binding.iconOrder.setImageResource(R.drawable.request_quote);


                binding.layoutLastOrderStatus.setOnClickListener(v -> {
                    OrderDetailFragment detailFragment = new OrderDetailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("orderId", lastOrder.getOrderId());
                    detailFragment.setArguments(bundle);

                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.fragment_container, detailFragment)
                            .addToBackStack(null)
                            .commit();
                });
            }
        });
    }

    private void navigateToOrderRequestWithImage() {
        if (selectedImageUri != null) {
            OrderRequestFragment orderRequestFragment = new OrderRequestFragment();
            Bundle bundle = new Bundle();
            bundle.putString("selected_image_uri", selectedImageUri.toString());
            orderRequestFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, orderRequestFragment)
                    .addToBackStack(null)
                    .commit();
        }
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