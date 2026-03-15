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

            // Adapter එක සාදන විට click listener එක ලබා දීම
            adapter = new OrderAdapter(orderList, order -> {

                // Order details පෙන්වන fragment එකට යාම
                OrderDetailFragment detailFragment = new OrderDetailFragment();

                // අවශ්‍ය නම් තෝරාගත් order එකේ දත්ත bundle එකක් හරහා යැවිය හැක
                Bundle bundle = new Bundle();
                bundle.putString("orderId", order.getOrderId()); // Order model එකේ ඇති ID එක
                detailFragment.setArguments(bundle);

                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null) // Back පැමිණීමට මෙය අනිවාර්යයි
                        .commit();
            });
            recyclerView.setAdapter(adapter);
        }
    }

}