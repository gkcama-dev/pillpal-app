package com.pillpal.app.fragment;

import static lk.payhere.androidsdk.PHConfigs.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.databinding.FragmentOrderDetailBinding;
import com.pillpal.app.databinding.ItemTimelineStepBinding;
import com.pillpal.app.model.Order;
import com.pillpal.app.model.User;
import com.pillpal.app.viewModel.OrderViewModel;
import com.pillpal.app.viewModel.ProfileViewModel;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private String orderId;
    private OrderViewModel viewModel;
    private String currentProcessingOrderId;
    private ProfileViewModel profileViewModel;


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

        viewModel = new ViewModelProvider(requireActivity()).get(OrderViewModel.class);
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

        if (uid != null) {
            profileViewModel.fetchUserProfile(uid);
        }
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

                String status = order.getStatus();

                // Approved -> Pay Button Enable
                if ("Approved".equals(status)) {
                    binding.cardPaymentInfo.setVisibility(View.VISIBLE);
                    binding.btnPayNow.setVisibility(View.VISIBLE);
                    binding.btnPayNow.setEnabled(true);

                    binding.tvOrderTotal.setText("LKR " + order.getTotal());
                    binding.btnPayNow.setOnClickListener(v -> initiatePayHerePayment(order));

                }

                // Payment Done,Accepted,Delivered  -> Pay Button Disable
                else if ("Payment Done".equals(status) || "Accepted".equals(status) || "Delivered".equals(status)) {
                    binding.cardPaymentInfo.setVisibility(View.VISIBLE);
                    binding.btnPayNow.setVisibility(View.VISIBLE);
                    binding.btnPayNow.setEnabled(false);
                    binding.btnPayNow.setText("Paid Successfully");
                    binding.btnPayNow.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray)));

                    binding.tvOrderTotal.setText("LKR " + order.getTotal());
                }
                //Pending, Rejected
                else {
                    binding.cardPaymentInfo.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateTimelineWithTime(String status, Timestamp t1, Timestamp t2, Timestamp t3, Timestamp t4, Timestamp t5) {
        resetTimeline();
        if (status == null) return;

        // 1. Order Placed (අනිවාර්යයෙන්ම සම්පූර්ණයි)
        setStepCompleted(binding.stepPending, "Order Placed", formatTime(t1));

        // 2. Admin Approved
        if (t2 != null || "Approved".equals(status) || "Payment Done".equals(status) || "Accepted".equals(status) || "Delivered".equals(status)) {
            setStepCompleted(binding.stepAdminApprove, "Admin Approved", formatTime(t2));
        }
        if ("Pending".equals(status)) {
            setStepActive(binding.stepPending, "Order Placed", "Your order is pending admin approval.");
        }

        // 3. Payment Completed
        if (t3 != null || "Payment Done".equals(status) || "Accepted".equals(status) || "Delivered".equals(status)) {
            setStepCompleted(binding.stepPaymentDone, "Payment Completed", formatTime(t3));
        }
        if ("Approved".equals(status)) {
            setStepActive(binding.stepAdminApprove, "Admin Approved", "Please complete your payment now.");
        }

        // 4. Order Accepted
        if (t4 != null || "Accepted".equals(status) || "Delivered".equals(status)) {
            setStepCompleted(binding.stepOrderAccept, "Order Accepted", formatTime(t4));
        }
        if ("Payment Done".equals(status)) {
            setStepActive(binding.stepPaymentDone, "Payment Completed", "We have received your payment.");
        }

        // 5. Order Delivered
        if (t5 != null || "Delivered".equals(status)) {
            setStepCompleted(binding.stepDelivered, "Order Delivered", formatTime(t5));
            binding.stepDelivered.viewLine.setVisibility(View.GONE);
        }
        if ("Accepted".equals(status)) {
            setStepActive(binding.stepOrderAccept, "Order Accepted", "Pharmacy is preparing your medicines.");
        }

        if ("Delivered".equals(status)) {
            setStepActive(binding.stepDelivered, "Order Delivered", "Order has arrived! Please confirm receipt.");
        }
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    private void setStepCompleted(ItemTimelineStepBinding step, String title, String time) {
        step.dot.setBackgroundResource(R.drawable.circle_green);
        step.viewLine.setBackgroundColor(getResources().getColor(R.color.green));
        step.tvStepStatus.setText(title);
        step.tvStepStatus.setAlpha(1.0f);
        step.tvStepDesc.setText("Completed");
        step.tvStepTime.setText(time);
    }

    private void setStepActive(ItemTimelineStepBinding step, String title, String desc) {
        step.dot.setBackgroundResource(R.drawable.circle_blue);
        step.tvStepStatus.setText(title);
        step.tvStepStatus.setAlpha(1.0f);
        step.tvStepDesc.setText(desc);
    }

    private void resetTimeline() {
        Object[][] stepsData = {
                {binding.stepPending, "Order Placed"},
                {binding.stepAdminApprove, "Admin Approved"},
                {binding.stepPaymentDone, "Payment Completed"},
                {binding.stepOrderAccept, "Order Accepted"},
                {binding.stepDelivered, "Order Delivered"}
        };

        for (Object[] data : stepsData) {
            ItemTimelineStepBinding step = (ItemTimelineStepBinding) data[0];
            String title = (String) data[1];

            step.dot.setBackgroundResource(R.drawable.circle_gray);
            step.viewLine.setBackgroundColor(getResources().getColor(R.color.gray));
            step.viewLine.setVisibility(View.VISIBLE);

            step.tvStepStatus.setText(title);
            step.tvStepStatus.setAlpha(0.5f); // Disable ප

            step.tvStepDesc.setText("");
            step.tvStepTime.setText("");
        }
    }

    private void initiatePayHerePayment(Order order) {
        Log.d("PAYHERE_TRACE", "--- Payment Process Started ---");
        this.currentProcessingOrderId = order.getOrderId();

        User currentUser = null;
        if (profileViewModel != null && profileViewModel.getUserLiveData().getValue() != null) {
            currentUser = profileViewModel.getUserLiveData().getValue();
            Log.d("PAYHERE_TRACE", "User data found in ViewModel: " + currentUser.getName());
        } else {
            Log.w("PAYHERE_TRACE", "User data NOT found in ViewModel. Falling back to Auth.");
        }

        String city = "Colombo"; // Default value
        String fullAddress = "No.1, Galle Road";

        try {
            // order එකේ තියෙන Lat/Lng
            double lat = Double.parseDouble(String.valueOf(order.getLatitude()));
            double lng = Double.parseDouble(String.valueOf(order.getLongitude()));

            Log.d("PAYHERE_TRACE", "Lat/Lng from Order: " + lat + ", " + lng);

            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address returnedAddress = addresses.get(0);

                // City(Locality or SubAdminArea)
                if (returnedAddress.getLocality() != null) {
                    city = returnedAddress.getLocality();
                } else if (returnedAddress.getSubAdminArea() != null) {
                    city = returnedAddress.getSubAdminArea();
                }

                //(No, Street)
                if (returnedAddress.getAddressLine(0) != null) {
                    fullAddress = returnedAddress.getAddressLine(0);
                }
                Log.d("LOCATION", "Found City: " + city + ", Address: " + fullAddress);
            }
        } catch (Exception e) {
            Log.e("LOCATION", "Error getting address: " + e.getMessage());
        }

        InitRequest req = new InitRequest();
        req.setSandBox(true);

        req.setMerchantId("1225156");
        req.setMerchantSecret("MjM4NzU3MjE1OTQwMjgwMTMwMDYzMDExNDI3ODEyMjMxMzI0MzI2OQ==");
        req.setCurrency("LKR");

        double amount = 0.0;
        try {
            String totalStr = String.valueOf(order.getTotal()).replaceAll("[^\\d.]", "");
            amount = Double.parseDouble(totalStr);
        } catch (Exception e) { amount = 0.0; }

        if (amount <= 0) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Payment Not Ready")
                    .setMessage("Admin has not yet priced this order. Please wait for approval with a total amount.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        req.setAmount(amount);
        req.setOrderId(order.getOrderId());
        req.setItemsDescription("Medicine Order #" + order.getOrderId());

        Log.d("PAYHERE_TRACE", "MerchantID: 1225156, Amount: " + amount + ", OrderID: " + order.getOrderId());

        if (currentUser != null) {
            // ඔබේ User model එකේ variables වල නම් අනුව මේවා වෙනස් කරන්න
            req.getCustomer().setFirstName(currentUser.getName());
            req.getCustomer().setLastName(""); // පවුලේ නම වෙනම තිබේ නම් එය මෙතැනට දෙන්න
            req.getCustomer().setEmail(currentUser.getEmail());
            req.getCustomer().setPhone(currentUser.getMobile());

            // Address Details (Geocoder එකෙන් ගත් දත්ත හෝ Profile එකේ දත්ත)
            req.getCustomer().getAddress().setAddress(fullAddress);
            req.getCustomer().getAddress().setCity(city);
            req.getCustomer().getAddress().setCountry("Sri Lanka");

            Log.d("PAYHERE_TRACE", "Customer: " + req.getCustomer().getFirstName() + ", Email: " + req.getCustomer().getEmail());
            Log.d("PAYHERE_TRACE", "Address: " + req.getCustomer().getAddress().getAddress() + ", City: " + city);
        } else {

            req.getCustomer().setFirstName("PillPal User");
            req.getCustomer().setEmail(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail());
            req.getCustomer().setPhone("+94000000000");
        }

//                req.setNotifyUrl("https://12342.requestcatcher.com/");
        try {
        Intent intent = new Intent(getActivity(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        Log.d("PAYHERE_TRACE", "Launching PayHere Activity...");
        payhereLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("PAYHERE_TRACE", "Activity Launch Error: " + e.getMessage());
        }
    }

    private void updatePaymentStatusInFirestore(String orderId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Payment Done");
        updates.put("paymentTimestamp", Timestamp.now());

        db.collection("orders").document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Order Paid Successfully!", Toast.LENGTH_SHORT).show();

                    viewModel.fetchOrderDetails(orderId);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_UPDATE", "Failed: " + e.getMessage());
                });
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

    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                                PHResponse<StatusResponse> response = (PHResponse<StatusResponse>)
                                        data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                                if (response != null && response.isSuccess()) {
                                    if (currentProcessingOrderId != null) {

                                        savePaymentRecord(currentProcessingOrderId, response);

                                        notifyAdmin(currentProcessingOrderId, response.getData().getPrice());

                                        updatePaymentStatusInFirestore(currentProcessingOrderId);

                                        Toast.makeText(getContext(), "Payment Success!", Toast.LENGTH_SHORT).show();
                                    }
                                } else if (response != null) {
                                    Toast.makeText(getContext(), "Payment Failed: " + response.getData().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

    private void savePaymentRecord(String orderId, PHResponse<StatusResponse> response) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("orderId", orderId);

        if (response.getData() != null) {

            paymentData.put("transactionId", String.valueOf(response.getData().getPaymentNo()));

            paymentData.put("amount", response.getData().getPrice());
            paymentData.put("statusMessage", response.getData().getMessage());
        }

        paymentData.put("timestamp", Timestamp.now());
        paymentData.put("userId", FirebaseAuth.getInstance().getUid());
        paymentData.put("status", "SUCCESS");

        db.collection("payments")
                .add(paymentData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("PAYMENT_DB", "Payment record saved: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("PAYMENT_DB", "Error saving payment record: " + e.getMessage());
                });
    }

    private void notifyAdmin(String orderId, double amount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "New Payment Received! 💰");
        notification.put("message", "Order #" + orderId + " has been paid. Amount: LKR " + amount + ". Please start preparing the medicines.");
        notification.put("orderId", orderId);
        notification.put("timestamp", Timestamp.now());
        notification.put("status", "UNREAD"); // Admin කියෙව්වාද නැද්ද බලන්න
        notification.put("type", "PAYMENT_DONE");

        db.collection("admin_notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("ADMIN_NOTI", "Admin notified successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("ADMIN_NOTI", "Failed to notify admin: " + e.getMessage());
                });
    }

}