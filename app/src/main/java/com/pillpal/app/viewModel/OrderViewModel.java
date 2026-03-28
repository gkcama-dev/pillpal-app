package com.pillpal.app.viewModel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.pillpal.app.model.Order;

import java.util.List;

public class OrderViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<Order>> ordersLiveData = new MutableLiveData<>();
    private final MutableLiveData<Order> orderDetailLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Order> lastOrderLiveData = new MutableLiveData<>();

    public LiveData<Order> getLastOrderLiveData() {
        return lastOrderLiveData;
    }

    public LiveData<List<Order>> getOrdersLiveData() {
        return ordersLiveData;
    }

    public LiveData<Order> getOrderDetailLiveData() {
        return orderDetailLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void fetchOrders(String uid) {
        Log.d("OrderViewModel", "Fetching orders for UID: " + uid);

        db.collection("orders")
                .whereEqualTo("userId", uid)
                .orderBy("pendingTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("OrderViewModel", "Fetch Orders Failed: " + error.getMessage());
                        errorMessage.setValue("Orders loading failed: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        try {
                            List<Order> orders = value.toObjects(Order.class);
                            ordersLiveData.setValue(orders);
                        } catch (Exception e) {
                            Log.e("OrderViewModel", "Mapping Error: " + e.getMessage());
                            errorMessage.setValue("Data format error in Database!");
                        }
                    }
                });
    }

    public void fetchOrderDetails(String orderId) {
        Log.d("OrderViewModel", "Fetching details for OrderID: " + orderId);

        db.collection("orders").document(orderId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("OrderViewModel", "Fetch Order Details Failed: " + error.getMessage());
                        errorMessage.setValue("Order details loading failed!");
                        return;
                    }
                    if (value != null && value.exists()) {
                        Order order = value.toObject(Order.class);
                        Log.d("OrderViewModel", "Order detail received for: " + (order != null ? order.getOrderId() : "null"));
                        orderDetailLiveData.setValue(order);
                    } else {
                        Log.w("OrderViewModel", "Order document does not exist for ID: " + orderId);
                    }
                });
    }

    public void fetchLastOrder(String uid) {
        db.collection("orders")
                .whereEqualTo("userId", uid)
                .orderBy("pendingTimestamp", Query.Direction.DESCENDING)
                .limit(1) //Last One
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        List<Order> orders = value.toObjects(Order.class);
                        if (!orders.isEmpty()) {
                            lastOrderLiveData.setValue(orders.get(0));
                        }
                    }
                });
    }

}
