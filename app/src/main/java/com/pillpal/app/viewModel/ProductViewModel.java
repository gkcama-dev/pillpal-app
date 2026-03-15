package com.pillpal.app.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.model.Product;

import java.util.List;

public class ProductViewModel extends ViewModel {

    private MutableLiveData<List<Product>> productList = new MutableLiveData<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Product>> getProducts() {
        return productList;
    }

    public void fetchProducts(String categoryId) {
        if (categoryId == null) return;

        // .get() exists .addSnapshotListener can get live update
        db.collection("products")
                .whereEqualTo("categoryId", categoryId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        List<Product> products = value.toObjects(Product.class);
                        productList.setValue(products);
                    }
                });
    }

    // Recent Products
    public void fetchRecentProducts() {
        db.collection("products")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        List<Product> products = value.toObjects(Product.class);
                        productList.setValue(products);
                    }
                });
    }
}
