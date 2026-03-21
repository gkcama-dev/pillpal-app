package com.pillpal.app.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import com.pillpal.app.viewModel.OrderViewModel;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private OrderViewModel orderViewModel;


    public OrderHistoryFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_history, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel Initialize
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        // User UID -> fetchOrders Call
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            orderViewModel.fetchOrders(uid);
        }

        // ViewModel Data Observe (Real-time Update)
        orderViewModel.getOrdersLiveData().observe(getViewLifecycleOwner(), orders -> {
            if (orders == null || orders.isEmpty()) {
                view.findViewById(R.id.recycler_view_orders).setVisibility(View.GONE);
                view.findViewById(R.id.layout_empty_state).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.layout_empty_state).setVisibility(View.GONE);
                view.findViewById(R.id.recycler_view_orders).setVisibility(View.VISIBLE);
                setupAdapter(orders);
            }
        });

        // Error
        orderViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapter(java.util.List<com.pillpal.app.model.Order> orders) {
        if (adapter == null) {

            adapter = new OrderAdapter(orders, order -> {
                OrderDetailFragment detailFragment = new OrderDetailFragment();
                Bundle bundle = new Bundle();
                bundle.putString("orderId", order.getOrderId());
                detailFragment.setArguments(bundle);

                // Animation රහිතව පිරිසිදු Fragment Transition එකක්
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            });
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(orders);
        }
    }
}