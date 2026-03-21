package com.pillpal.app.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.databinding.FragmentOrderDetailBinding;
import com.pillpal.app.databinding.ItemTimelineStepBinding;
import com.pillpal.app.viewModel.OrderViewModel;

import java.text.SimpleDateFormat;
import java.util.Locale;


public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private String orderId;
    private OrderViewModel viewModel;

    public OrderDetailFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString("orderId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        if (orderId != null) {
            binding.tvDetailOrderId.setText("#" + orderId);
            viewModel.fetchOrderDetails(orderId);
        }

        // ViewModel -> Observe
        viewModel.getOrderDetailLiveData().observe(getViewLifecycleOwner(), order -> {
            if (order != null) {
                updateTimelineWithTime(
                        order.getStatus(),
                        order.getPendingTimestamp(),
                        order.getApprovedTimestamp(),
                        order.getPaymentTimestamp(),
                        order.getAcceptedTimestamp(),
                        order.getDeliveredTimestamp()
                );
            }
        });
    }

    private void updateTimelineWithTime(String status, Timestamp t1, Timestamp t2, Timestamp t3, Timestamp t4, Timestamp t5) {
        resetTimeline();
        if (status == null) return;

        // 1. Pending Step
        setStepCompleted(binding.stepPending, "Order Placed", formatTime(t1));
        if ("Pending".equals(status)) {
            setStepActive(binding.stepPending, "Order Placed", "Your order is pending admin approval.");
        }

        // 2. Approved Step
        if (t2 != null) {
            setStepCompleted(binding.stepAdminApprove, "Admin Approved", formatTime(t2));
        }
        if ("Approved".equals(status)) {
            setStepActive(binding.stepAdminApprove, "Admin Approved", "Please complete your payment now.");
        }

        // 3. Payment Step
        if (t3 != null) {
            setStepCompleted(binding.stepPaymentDone, "Payment Completed", formatTime(t3));
        }
        if ("Payment Done".equals(status)) {
            setStepActive(binding.stepPaymentDone, "Payment Completed", "We have received your payment.");
        }

        // 4. Accepted Step
        if (t4 != null) {
            setStepCompleted(binding.stepOrderAccept, "Order Accepted", formatTime(t4));
        }
        if ("Accepted".equals(status)) {
            setStepActive(binding.stepOrderAccept, "Order Accepted", "Pharmacy is preparing your medicines.");
        }

        // 5. Delivered Step
        if (t5 != null) {
            setStepCompleted(binding.stepDelivered, "Order Delivered", formatTime(t5));
            binding.stepDelivered.viewLine.setVisibility(View.GONE);
        }
        if ("Delivered".equals(status)) {
            setStepActive(binding.stepDelivered, "Order Delivered", "Order has arrived! Please confirm receipt.");
        }
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "Waiting...";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    private void setStepCompleted(ItemTimelineStepBinding step, String title, String time) {
        step.dot.setBackgroundResource(R.drawable.circle_green);
        step.viewLine.setBackgroundColor(getResources().getColor(R.color.green));
        step.tvStepStatus.setText(title);
        step.tvStepStatus.setAlpha(1.0f);
        step.tvStepDesc.setText("Completed at " + time);
    }

    private void setStepActive(ItemTimelineStepBinding step, String title, String desc) {
        step.dot.setBackgroundResource(R.drawable.circle_blue);
        step.tvStepStatus.setText(title);
        step.tvStepStatus.setAlpha(1.0f);
        step.tvStepDesc.setText(desc);
    }

    private void resetTimeline() {
        ItemTimelineStepBinding[] steps = {
                binding.stepPending, binding.stepAdminApprove,
                binding.stepPaymentDone, binding.stepOrderAccept, binding.stepDelivered
        };

        for (ItemTimelineStepBinding step : steps) {
            step.dot.setBackgroundResource(R.drawable.circle_gray);
            step.viewLine.setBackgroundColor(getResources().getColor(R.color.gray));
            step.viewLine.setVisibility(View.VISIBLE);
            step.tvStepStatus.setAlpha(0.5f);
            step.tvStepDesc.setText("");
        }
    }
    private void updateStep(ItemTimelineStepBinding step, String title, String desc, int dotDrawable) {
        step.tvStepStatus.setText(title);
        step.tvStepDesc.setText(desc);
        step.dot.setBackgroundResource(dotDrawable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}