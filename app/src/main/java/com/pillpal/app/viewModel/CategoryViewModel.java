package com.pillpal.app.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.model.Category;

import java.util.List;

public class CategoryViewModel extends ViewModel {

    private MutableLiveData<List<Category>> categoryList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Category>> getCategories() {
        if (categoryList == null) {
            categoryList = new MutableLiveData<>();
            listenToCategories(); // Live update
        }
        return categoryList;
    }

    private void listenToCategories() {
        // .get() exists .addSnapshotListener can get live update
        db.collection("categories").addSnapshotListener((value, error) -> {
            if (error != null) return;

            if (value != null) {
                // Firestore Data -> Category objects list
                List<Category> categories = value.toObjects(Category.class);
                categoryList.setValue(categories);
            }
        });
    }

}
