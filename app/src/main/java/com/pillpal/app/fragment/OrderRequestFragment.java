package com.pillpal.app.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.pillpal.app.R;
import com.pillpal.app.activity.SelectLocationActivity;
import com.pillpal.app.databinding.FragmentOrderRequestBinding;


public class OrderRequestFragment extends Fragment {

    private Uri imageUri;

    private FragmentOrderRequestBinding binding;

    public OrderRequestFragment() {
        // Required empty public constructor
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
        // Inflate the layout for this fragment
        binding = FragmentOrderRequestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    binding.imgPrescriptionPreview.setImageURI(uri);
                    binding.imgPrescriptionPreview.setVisibility(View.VISIBLE);
                    binding.layoutUploadPlaceholder.setVisibility(View.GONE);
                }
            }
    );

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Select Image Click Listener
        binding.cardUploadPrescription.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // Next Step Button click -> SelectLocationActivity
        binding.btnNextStep.setOnClickListener(v -> {
            if (imageUri == null) {
                Toast.makeText(getContext(), "Please upload your prescription first!", Toast.LENGTH_SHORT).show();
                return;
            }

            String notes = binding.etOrderNotes.getText().toString().trim();

            // Location Activity එකට data යැවීම
            Intent intent = new Intent(getContext(), SelectLocationActivity.class);
            intent.putExtra("ORDER_NOTES", notes);
            intent.putExtra("PRESCRIPTION_URI", imageUri.toString());
            startActivity(intent);
        });

        // Get Home Image
        if (getArguments() != null) {
            String imageUriString = getArguments().getString("selected_image_uri");
            if (imageUriString != null) {
                imageUri = Uri.parse(imageUriString);

                binding.layoutUploadPlaceholder.setVisibility(View.GONE);

                binding.imgPrescriptionPreview.setVisibility(View.VISIBLE);

                Glide.with(this)
                        .load(imageUri)
                        .centerCrop()
                        .into(binding.imgPrescriptionPreview);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}