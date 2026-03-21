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

    public LiveData<List<Order>> getOrdersLiveData() { return ordersLiveData; }
    public LiveData<Order> getOrderDetailLiveData() { return orderDetailLiveData; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

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
                        List<Order> orders = value.toObjects(Order.class);
                        Log.d("OrderViewModel", "Orders received. Count: " + orders.size());
                        ordersLiveData.setValue(orders);
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

}
