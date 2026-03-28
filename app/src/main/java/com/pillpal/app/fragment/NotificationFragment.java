package com.pillpal.app.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.pillpal.app.R;
import com.pillpal.app.adapter.NotificationAdapter;
import com.pillpal.app.databinding.FragmentNotificationBinding;
import com.pillpal.app.viewModel.NotificationViewModel;


public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;
    private NotificationViewModel viewModel;
    private NotificationAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        String userId = FirebaseAuth.getInstance().getUid();

        setupRecyclerView();

        if (userId != null) {
            viewModel.getNotifications(userId).observe(getViewLifecycleOwner(), notifications -> {
                if (notifications != null) {
                    adapter = new NotificationAdapter(notifications, viewModel);
                    binding.rvNotifications.setAdapter(adapter);
                }
            });
        }
    }

    private void setupRecyclerView() {
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}