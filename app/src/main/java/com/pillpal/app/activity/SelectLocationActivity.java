package com.pillpal.app.activity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.databinding.ActivitySelectLocationBinding;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.net.Uri;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SelectLocationActivity extends AppCompatActivity {

    private ActivitySelectLocationBinding binding;
    private Point selectedPoint;
    private Point userCurrentLocation;
    private static final int REQUEST_CHECK_SETTINGS = 1001;
    private final String IMGBB_API_KEY = "2957bdbd2622b55cf85dc26cb4ea002d";

    // Location Permission Request Launcher
    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    enableGPSAndSetupMap();
                } else {
                    Toast.makeText(this, "Location permission is required to deliver your medicine", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySelectLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkPermissions();

        binding.btnConfirmOrder.setOnClickListener(v -> {
            if (selectedPoint != null) {
                // මුලින්ම Image එක Upload කරන මෙතඩ් එක call කරනවා
                startOrderSubmissionProcess();
            } else {
                Toast.makeText(this, "Please wait for the map to load", Toast.LENGTH_SHORT).show();
            }
        });

        // My Location Button Click Logic
        binding.btnMyLocation.setOnClickListener(v -> {
            if (userCurrentLocation != null) {
                zoomToLocation(userCurrentLocation);
                selectedPoint = userCurrentLocation;
            } else {
                Toast.makeText(this, "Finding your location...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 1. Image එක Upload කිරීම ආරම්භ කරන මෙතඩ් එක
    private void startOrderSubmissionProcess() {
        String uriString = getIntent().getStringExtra("PRESCRIPTION_URI");
        if (uriString == null) return;

        Uri imageUri = Uri.parse(uriString);
        binding.btnConfirmOrder.setEnabled(false);
        binding.btnConfirmOrder.setText("Uploading Image...");

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] bytes = getBytes(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", IMGBB_API_KEY)
                    .addFormDataPart("image", "prescription.jpg",
                            RequestBody.create(bytes, MediaType.parse("image/*")))
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imgbb.com/1/upload")
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        binding.btnConfirmOrder.setEnabled(true);
                        binding.btnConfirmOrder.setText("Confirm & Submit Order");
                        Toast.makeText(SelectLocationActivity.this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            String imageUrl = jsonObject.getJSONObject("data").getString("url");

                            // Image URL එක ලැබුණා! දැන් Firestore එකට යවමු
                            runOnUiThread(() -> submitOrderToFirestore(imageUrl));

                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            enableGPSAndSetupMap();
        }
    }

    private void enableGPSAndSetupMap() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> setupMap());

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(SelectLocationActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    sendEx.printStackTrace();
                }
            }
        });
    }

    private void setupMap() {
        binding.mapView.getMapboxMap().loadStyleUri("mapbox://styles/mapbox/streets-v12", style -> {
            enableLocationComponent();
            setupMapGestures();
        });
    }

    private void enableLocationComponent() {
        LocationComponentPlugin locationComponentPlugin = LocationComponentUtils.getLocationComponent(binding.mapView);
        locationComponentPlugin.setEnabled(true);
        locationComponentPlugin.setLocationPuck(new LocationPuck2D()); // User ගේ location එක පෙන්වන puck එක

// Indicator එක වෙනස් වන සෑම විටම userCurrentLocation එක update කරනවා
        locationComponentPlugin.addOnIndicatorPositionChangedListener(point -> {
            userCurrentLocation = point; // සැබෑ location එක මෙතන තබා ගනී

            if (selectedPoint == null) {
                selectedPoint = point;
                zoomToLocation(point);
            }
        });
    }

    private void setupMapGestures() {
        GesturesUtils.getGestures(binding.mapView).addOnMoveListener(new OnMoveListener() {
            @Override
            public void onMoveBegin(@NonNull MoveGestureDetector detector) {}

            @Override
            public boolean onMove(@NonNull MoveGestureDetector detector) {
                return false;
            }

            @Override
            public void onMoveEnd(@NonNull MoveGestureDetector detector) {
                // Map එක drag කරලා ඉවර වුණාම මැද තියෙන point එක ලබාගන්නවා
                selectedPoint = binding.mapView.getMapboxMap().getCameraState().getCenter();
            }
        });
    }

    // 🔥 1. Latitude/Longitude -> Address -> Helper
    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Address not found";
    }

    private void submitOrderToFirestore(String prescriptionImageUrl) {
        String uid = FirebaseAuth.getInstance().getUid();
        String notes = getIntent().getStringExtra("ORDER_NOTES");
        String customOrderId = "ORD-" + (int)(Math.random() * 100000);

        binding.btnConfirmOrder.setText("Finalizing Order...");

        String readableAddress = getAddressFromLocation(selectedPoint.latitude(), selectedPoint.longitude());

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", customOrderId);
        orderData.put("userId", uid);
        orderData.put("status", "Pending");
        orderData.put("pendingTimestamp", com.google.firebase.Timestamp.now());
        orderData.put("approvedTimestamp", null);
        orderData.put("paymentTimestamp", null);
        orderData.put("acceptedTimestamp", null);
        orderData.put("deliveredTimestamp", null);
        orderData.put("total", Double.valueOf(0.0));
        orderData.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        orderData.put("notes", notes);
        orderData.put("prescriptionUrl", prescriptionImageUrl); // ImgBB URL
        orderData.put("latitude", selectedPoint.latitude());
        orderData.put("longitude", selectedPoint.longitude());
        orderData.put("address", readableAddress);
        orderData.put("receivedTimestamp", null);

        FirebaseFirestore.getInstance().collection("orders")
                .document(customOrderId)
                .set(orderData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Order Submitted Successfully!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SelectLocationActivity.this, OrderSuccessActivity.class);

                    // Order ID screen
                    intent.putExtra("ORDER_ID", customOrderId);

                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnConfirmOrder.setEnabled(true);
                    Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void zoomToLocation(Point point) {
        binding.mapView.getMapboxMap().setCamera(
                new CameraOptions.Builder()
                        .center(point)
                        .zoom(16.0)
                        .build()
        );
    }
}