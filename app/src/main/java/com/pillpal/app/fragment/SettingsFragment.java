package com.pillpal.app.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.pillpal.app.R;
import com.pillpal.app.activity.SignInActivity;
import com.pillpal.app.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private android.hardware.SensorManager sensorManager;
    private android.hardware.Sensor lightSensor;
    private android.hardware.SensorEventListener lightEventListener;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                updateSwitches();
            });

    public SettingsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        setupListeners();
        updateSwitches();

        return binding.getRoot();
    }

    private void setupListeners() {
        binding.swNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        });

        binding.swGps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });

        binding.swCall.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
                }
            } else {
                updateSwitches();
            }
        });

        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        binding.swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Dark Mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                saveThemePreference(true);
            } else {
                // Light Mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                saveThemePreference(false);
            }
        });

        binding.swAutoBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!android.provider.Settings.System.canWrite(requireContext())) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(intent);
                        binding.swAutoBrightness.setChecked(false);
                    } else {
                        startAutoBrightness();
                        saveAutoBrightnessPreference(true);
                    }
                }
            } else {
                stopAutoBrightness();
                saveAutoBrightnessPreference(false);
            }
        });
    }

    private void startAutoBrightness() {
        sensorManager = (android.hardware.SensorManager) requireContext().getSystemService(android.content.Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_LIGHT);

        if (lightSensor == null) {
            Toast.makeText(getContext(), "Light sensor not available!", Toast.LENGTH_SHORT).show();
            binding.swAutoBrightness.setChecked(false);
            return;
        }

        lightEventListener = new android.hardware.SensorEventListener() {
            @Override
            public void onSensorChanged(android.hardware.SensorEvent event) {
                float luxValue = event.values[0]; // natural light
                setBrightness((int) luxValue);
            }

            @Override
            public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}
        };

        sensorManager.registerListener(lightEventListener, lightSensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void setBrightness(int lux) {
        // Lux 0-255 Brightness
        int brightness = lux > 255 ? 255 : (lux < 10 ? 10 : lux);

        android.provider.Settings.System.putInt(
                requireContext().getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness
        );
    }

    private void stopAutoBrightness() {
        if (sensorManager != null && lightEventListener != null) {
            sensorManager.unregisterListener(lightEventListener);
        }
    }

    private void saveAutoBrightnessPreference(boolean isEnabled) {
        if (getContext() != null) {
            android.content.SharedPreferences sharedPreferences =
                    getContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("AutoBrightness", isEnabled);
            editor.apply();
        }
    }

    private void saveThemePreference(boolean isDarkMode) {
        if (getContext() != null) {
            android.content.SharedPreferences sharedPreferences =
                    getContext().getSharedPreferences("ThemePrefs", android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("IsDarkMode", isDarkMode);
            editor.apply();
        }
    }

    private boolean isDarkModeEnabled() {
        if (getContext() != null) {
            android.content.SharedPreferences sharedPreferences =
                    getContext().getSharedPreferences("ThemePrefs", android.content.Context.MODE_PRIVATE);
            return sharedPreferences.getBoolean("IsDarkMode", false);
        }
        return false;
    }

    private void updateSwitches() {
        if (getContext() == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.swNotification.setChecked(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        }
        binding.swGps.setChecked(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        binding.swCall.setChecked(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED);

        // Dark Mode
        binding.swDarkMode.setChecked(isDarkModeEnabled());

        // Auto Brightness
        android.content.SharedPreferences sharedPreferences =
                getContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
        boolean isAutoEnabled = sharedPreferences.getBoolean("AutoBrightness", false);

        binding.swAutoBrightness.setChecked(isAutoEnabled);

        if (isAutoEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (android.provider.Settings.System.canWrite(requireContext())) {
                    startAutoBrightness();
                } else {

                    binding.swAutoBrightness.setChecked(false);
                    saveAutoBrightnessPreference(false);
                }
            } else {
                startAutoBrightness();
            }
        }
    }

    private void showLogoutConfirmation() {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_logout_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        dialog.show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitches();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding.swAutoBrightness.isChecked()) {
            stopAutoBrightness();
        }
    }
}