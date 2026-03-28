package com.pillpal.app.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pillpal.app.R;
import com.pillpal.app.databinding.ActivityMainBinding;
import com.pillpal.app.databinding.SideNavHeaderBinding;
import com.pillpal.app.fragment.CategoryFragment;
import com.pillpal.app.fragment.HomeFragment;
import com.pillpal.app.fragment.NotificationFragment;
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

    private ListenerRegistration notificationListener;
    private static final int NOTIFICATION_PERMISSION_CODE = 101; //Request Code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.content.SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("IsDarkMode", false);

        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Check user session and FCM Token
        if (firebaseAuth.getCurrentUser() != null) {
            updateFCMToken();
            askNotificationPermission(); // Ask Permission
            listenForNotifications();
        } else {
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

        binding.btnNotification.setOnClickListener(v -> {
            loadFragment(new NotificationFragment());
        });

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

    /**
     * Android 13+ Notification Permission
     */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    /**
     * Firestore Notifications
     */
    private void listenForNotifications() {
        String uid = firebaseAuth.getUid();
        if (uid == null) return;

        if (notificationListener != null) notificationListener.remove();

        notificationListener = firebaseFirestore.collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isRead", false) // not read
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        int count = value.size();
                        if (count > 0) {
                            binding.tvNotificationCount.setVisibility(View.VISIBLE);
                            binding.tvNotificationCount.setText(String.valueOf(count));

                            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake_anim);
                            binding.btnNotification.startAnimation(shake);
                        } else {
                            binding.tvNotificationCount.setVisibility(View.GONE);
                        }
                    }
                });
    }

    /**
     * FCM Token Firestore Update
     */
    private void updateFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    String uid = firebaseAuth.getUid();

                    if (uid != null && token != null) {
                        firebaseFirestore.collection("users")
                                .document(uid)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("FCM", "Token Updated Successfully"))
                                .addOnFailureListener(e -> Log.e("FCM", "Token Update Failed", e));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("FCM", "Notification Permission Granted");
            } else {
                Toast.makeText(this, "Notification permission denied. You won't receive order updates.", Toast.LENGTH_LONG).show();
            }
        }
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
                }else if (fragment instanceof NotificationFragment) {
                    binding.toolbarTitle.setText("Notifications");
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
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);

        com.pillpal.app.databinding.LayoutLogoutDialogBinding dialogBinding =
                com.pillpal.app.databinding.LayoutLogoutDialogBinding.inflate(getLayoutInflater());

        builder.setView(dialogBinding.getRoot());
        builder.setCancelable(true);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Cancel
        dialogBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Confirm Logout
        dialogBinding.btnConfirmLogout.setOnClickListener(v -> {
            dialog.dismiss();

            // FCM Token Clear
            clearFCMTokenAndLogout();
        });

        dialog.show();
    }

    // Logout Token
    private void clearFCMTokenAndLogout() {
        String uid = firebaseAuth.getUid();
        if (uid != null) {
            firebaseFirestore.collection("users").document(uid)
                    .update("fcmToken", null) // Token null
                    .addOnCompleteListener(task -> {
                        firebaseAuth.signOut();
                        Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        navigateToSignIn();
                    });
        } else {
            firebaseAuth.signOut();
            navigateToSignIn();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Listener
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}