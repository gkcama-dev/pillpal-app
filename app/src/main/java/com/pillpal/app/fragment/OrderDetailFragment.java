package com.pillpal.app.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pillpal.app.R;
import com.pillpal.app.databinding.FragmentOrderDetailBinding;
import com.pillpal.app.databinding.ItemTimelineStepBinding;


public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private String orderId;

    public OrderDetailFragment() {
        // Required empty public constructor
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

        if (orderId != null) {
            binding.tvDetailOrderId.setText("#" + orderId);
        }
        setupTimelineData("Pending");
    }


    private void setupTimelineData(String status) {
        resetTimeline(); // මුලින්ම reset කරන්න

        if (status == null) return;

        // Status අනුව පියවරවල් පෙන්වීම
        switch (status) {
            case "Pending":
                setStepActive(binding.stepPending, "Order Placed", "Your order is pending admin approval.");
                break;

            case "Approved":
                setStepCompleted(binding.stepPending, "Order Placed");
                setStepActive(binding.stepAdminApprove, "Admin Approved", "Please complete your payment now.");
                break;

            case "Payment Done":
                setStepCompleted(binding.stepPending, "Order Placed");
                setStepCompleted(binding.stepAdminApprove, "Admin Approved");
                setStepActive(binding.stepPaymentDone, "Payment Completed", "We have received your payment.");
                break;

            case "Accepted":
                setStepCompleted(binding.stepPending, "Order Placed");
                setStepCompleted(binding.stepAdminApprove, "Admin Approved");
                setStepCompleted(binding.stepPaymentDone, "Payment Completed");
                setStepActive(binding.stepOrderAccept, "Order Accepted", "Pharmacy is preparing your medicines.");
                break;

            case "Delivered":
                setStepCompleted(binding.stepPending, "Order Placed");
                setStepCompleted(binding.stepAdminApprove, "Admin Approved");
                setStepCompleted(binding.stepPaymentDone, "Payment Completed");
                setStepCompleted(binding.stepOrderAccept, "Order Accepted");
                setStepActive(binding.stepDelivered, "Order Delivered", "Order has arrived! Please confirm receipt.");
                // අන්තිම එක නිසා පල්ලෙහා ඉර අයින් කරන්න
                binding.stepDelivered.viewLine.setVisibility(View.GONE);
                break;
        }
    }

    private void setStepActive(ItemTimelineStepBinding step, String title, String desc) {
        step.dot.setBackgroundResource(R.drawable.circle_blue);
        step.tvStepStatus.setText(title);
        step.tvStepStatus.setAlpha(1.0f);
        step.tvStepDesc.setText(desc);
    }

    private void setStepCompleted(ItemTimelineStepBinding step, String title) {
        step.dot.setBackgroundResource(R.drawable.circle_green);
        step.viewLine.setBackgroundColor(getResources().getColor(R.color.green)); // ඉර කොළ පාට කිරීම
        step.tvStepStatus.setText(title);
        step.tvStepStatus.setAlpha(1.0f);
        step.tvStepDesc.setText("Done");
    }

    private void resetTimeline() {
        // List එකක් ලෙස දමා පහසුවෙන් reset කරමු
        ItemTimelineStepBinding[] steps = {
                binding.stepPending,
                binding.stepAdminApprove,
                binding.stepPaymentDone,
                binding.stepOrderAccept,
                binding.stepDelivered
        };

        for (ItemTimelineStepBinding step : steps) {
            step.dot.setBackgroundResource(R.drawable.circle_gray); // circle_gray එකක් හදාගන්න
            step.viewLine.setBackgroundColor(getResources().getColor(R.color.gray));
            step.tvStepStatus.setAlpha(0.5f); // ටිකක් පේන්නේ නැති වෙන්න (faded)
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