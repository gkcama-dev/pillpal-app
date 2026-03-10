package com.pillpal.app.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.R;
import com.pillpal.app.adapter.OrderAdapter;
import com.pillpal.app.model.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private TextView tvNoOrders;
    private FirebaseFirestore db;


    public OrderHistoryFragment() {

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
        View view = inflater.inflate(R.layout.fragment_order_history, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_orders);
        tvNoOrders = view.findViewById(R.id.tv_no_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        orderList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

//        uploadSampleDataToFirebase();

        loadOrdersFromFirestore();

        return view;
    }


    private void loadOrdersFromFirestore() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("orders")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    orderList.clear();
                    orderList.addAll(queryDocumentSnapshots.toObjects(Order.class));
                    updateUI();
                });
    }

    private void updateUI() {
        if (orderList.isEmpty()) {
            tvNoOrders.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoOrders.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new OrderAdapter(orderList);
            recyclerView.setAdapter(adapter);
        }
    }

    private void uploadSampleDataToFirebase() {
        List<Order> sampleOrders = new ArrayList<>();
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId == null) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }


        sampleOrders.add(new Order("ORD-88219", "Pending", "2026-03-05", "1,450.00", currentUserId));
        sampleOrders.add(new Order("ORD-77102", "Delivered", "2026-02-28", "2,100.00", currentUserId));
        sampleOrders.add(new Order("ORD-65431", "Cancelled", "2026-02-15", "850.00", currentUserId));

        for (Order order : sampleOrders) {
            db.collection("orders")
                    .document(order.getOrderId())
                    .set(order)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Order added: " + order.getOrderId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Error adding order", e);
                    });
        }

        Toast.makeText(getContext(), "Sample data uploaded!", Toast.LENGTH_SHORT).show();
    }
}