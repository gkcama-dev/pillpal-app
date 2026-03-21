package com.pillpal.app.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.activity.SignInActivity;
import com.pillpal.app.databinding.FragmentProfileBinding;
import com.pillpal.app.model.User;
import com.pillpal.app.viewModel.ProfileViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private final String IMGBB_API_KEY = "2957bdbd2622b55cf85dc26cb4ea002d";



    public ProfileFragment() {
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
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        observeViewModel();
        viewModel.fetchUserProfile(currentUserId);

        setupListeners();

    }

    private void observeViewModel() {
        // දත්ත වෙනස් වන විට UI එක Update කිරීම
        viewModel.getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null && binding != null) {
                binding.etProfileFullName.setText(user.getName());
                binding.etProfileMobile.setText(user.getMobile());
                binding.etProfileAddress.setText(user.getAddress());
                binding.etProfileNic.setText(user.getNic());
                binding.etProfileEmergency.setText(user.getEmergencyContact());

                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    Glide.with(requireContext())
                            .load(user.getProfileImageUrl())
                            .centerCrop()
                            .into(binding.profileImageLarge);
                }
            }
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {

                binding.btnUpdateProfile.setEnabled(true);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        // Image Picking
        binding.profileImageLarge.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        // Update Button
        binding.btnUpdateProfile.setOnClickListener(v -> {

            if (validateInputs()) {

                // Validation
                binding.btnUpdateProfile.setEnabled(false);

                Map<String, Object> updates = new HashMap<>();
                updates.put("name", binding.etProfileFullName.getText().toString().trim());
                updates.put("mobile", binding.etProfileMobile.getText().toString().trim());
                updates.put("address", binding.etProfileAddress.getText().toString().trim());
                updates.put("nic", binding.etProfileNic.getText().toString().trim());
                updates.put("emergencyContact", binding.etProfileEmergency.getText().toString().trim());

                viewModel.updateProfile(currentUserId, updates);
            } else {

                Toast.makeText(getContext(), "Please correct the errors above", Toast.LENGTH_SHORT).show();
            }
        });

        // Logout Button
        binding.btnLogoutProfile.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(requireActivity(), SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private boolean validateInputs() {
        String name = binding.etProfileFullName.getText().toString().trim();
        String mobile = binding.etProfileMobile.getText().toString().trim();
        String nic = binding.etProfileNic.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etProfileFullName.setError("Full name is required");
            binding.etProfileFullName.requestFocus();
            return false;
        }

        if (mobile.isEmpty() || mobile.length() < 10) {
            binding.etProfileMobile.setError("Enter a valid phone number");
            binding.etProfileMobile.requestFocus();
            return false;
        }

        if (!nic.isEmpty() && nic.length() < 10) {
            binding.etProfileNic.setError("Invalid NIC format");
            binding.etProfileNic.requestFocus();
            return false;
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data != null) {
            uploadImageToImgBB(data.getData());
        }
    }


    // ImgBB Upload Logic (Multipart)
    private void uploadImageToImgBB(Uri imageUri) {
        binding.btnUpdateProfile.setEnabled(false);
        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            byte[] bytes = getBytes(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", IMGBB_API_KEY)
                    .addFormDataPart("image", "profile.jpg",
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
                    requireActivity().runOnUiThread(() -> {
                        binding.btnUpdateProfile.setEnabled(true);
                        Toast.makeText(getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.body().string());
                            String imageUrl = jsonObject.getJSONObject("data").getString("url");

                            // Image URL එක Update කිරීමට ViewModel භාවිතා කිරීම
                            Map<String, Object> update = new HashMap<>();
                            update.put("profileImageUrl", imageUrl);
                            requireActivity().runOnUiThread(() -> {
                                viewModel.updateProfile(currentUserId, update);
                                binding.btnUpdateProfile.setEnabled(true);
                            });

                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
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



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}