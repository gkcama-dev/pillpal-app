package com.pillpal.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.databinding.ActivityMainBinding;
import com.pillpal.app.databinding.SideNavHeaderBinding;
import com.pillpal.app.fragment.CategoryFragment;
import com.pillpal.app.fragment.HomeFragment;
import com.pillpal.app.fragment.OrderDetailFragment;
import com.pillpal.app.fragment.OrderHistoryFragment;
import com.pillpal.app.fragment.OrderRequestFragment;
import com.pillpal.app.fragment.ProfileFragment;
import com.pillpal.app.fragment.SettingsFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private SideNavHeaderBinding sideNavHeaderBinding;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Check user session
        if (firebaseAuth.getCurrentUser() == null) {
            navigateToSignIn();
            return;
        }

        // Initialize UI components
        drawerLayout = binding.drawerLayout;
        toolbar = binding.toolbar;
        navigationView = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;

        // Setup Side Navigation Header
        View headerView = navigationView.getHeaderView(0);
        sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Handle Back Navigation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });

        // Setup Listeners
        binding.menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        // Load Default Fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null) {
                // Update the toolbar to match the fragment we just returned to
                updateToolbarUI(currentFragment);

                // Update Bottom/Side Navigation selection to match current fragment
                updateNavigationSelection(currentFragment);
            }
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        // Handle Side and Bottom Nav navigation
        if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home) {
            loadFragment(new HomeFragment());
        } else if (itemId == R.id.side_nav_category || itemId == R.id.bottom_nav_category) {
            loadFragment(new CategoryFragment());
        } else if (itemId == R.id.side_nav_order || itemId == R.id.bottom_nav_order_request) {
            loadFragment(new OrderRequestFragment());
        } else if (itemId == R.id.side_nav_history || itemId == R.id.bottom_nav_order_history) {
            loadFragment(new OrderHistoryFragment());
        } else if (itemId == R.id.side_nav_profile) {
            loadFragment(new ProfileFragment());
        } else if (itemId == R.id.side_nav_settings) {
            loadFragment(new SettingsFragment());
        } else if (itemId == R.id.side_nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("GO_TO_HISTORY", false)) {
            loadFragment(new OrderHistoryFragment());
            binding.bottomNavigationView.setSelectedItemId(R.id.bottom_nav_order_history);
        }
    }

    private void loadFragment(Fragment fragment) {
        if (fragment == null) return;

        // Prevent reloading the same fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        // Manage Toolbar UI based on fragment type
        updateToolbarUI(fragment);

        // Manage Backstack logic: Only Home fragment clears the stack
        var transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(R.id.fragment_container, fragment);

        if (!(fragment instanceof HomeFragment)) {
            transaction.addToBackStack(null);
        } else {
            // Clear backstack when returning Home
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        transaction.commitAllowingStateLoss();
    }

    private void updateNavigationSelection(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
            navigationView.getMenu().findItem(R.id.side_nav_home).setChecked(true);
        } else if (fragment instanceof CategoryFragment) {
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_category).setChecked(true);
            navigationView.getMenu().findItem(R.id.side_nav_category).setChecked(true);
        } else if (fragment instanceof OrderRequestFragment) {
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_order_request).setChecked(true);
        } else if (fragment instanceof OrderHistoryFragment || fragment instanceof OrderDetailFragment) {
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_order_history).setChecked(true);
        }
    }

    private void updateToolbarUI(Fragment fragment) {
        // Show Search Bar for specific fragments
        if (fragment instanceof CategoryFragment ||
                fragment instanceof OrderHistoryFragment ||
                fragment.getClass().getSimpleName().equals("ProductListFragment")) {

            binding.textInputSearch.setVisibility(View.VISIBLE);
            binding.toolbarTitle.setVisibility(View.GONE);

            // Show Menu Icon
            binding.menuIcon.setImageResource(R.drawable.menu);
            binding.menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        } else {
            // Show Centered Title for specific fragments
            binding.textInputSearch.setVisibility(View.GONE);
            binding.toolbarTitle.setVisibility(View.VISIBLE);
            binding.toolbarTitle.setTextColor(getResources().getColor(R.color.primaryPurple));

            if (fragment instanceof HomeFragment) {
                binding.toolbarTitle.setText("Home");
                binding.menuIcon.setImageResource(R.drawable.menu);
                binding.menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
            } else {
                // Show Back Arrow for Profile, Settings, etc.
                binding.menuIcon.setImageResource(R.drawable.ic_back);
                binding.menuIcon.setOnClickListener(v -> getSupportFragmentManager().popBackStack());

                if (fragment instanceof OrderRequestFragment) {
                    binding.toolbarTitle.setText("Order Request");
                } else if (fragment instanceof ProfileFragment) {
                    binding.toolbarTitle.setText("My Profile");
                } else if (fragment instanceof SettingsFragment) {
                    binding.toolbarTitle.setText("Settings");
                } else if (fragment instanceof OrderDetailFragment) {
                    binding.toolbarTitle.setText("Order Details");
                }
            }
        }
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    private void logoutUser() {
        firebaseAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToSignIn();
    }
}