package com.pillpal.app.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.pillpal.app.model.User;

import java.util.Map;

public class ProfileViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();

    public LiveData<User> getUserLiveData() { return userLiveData; }
    public LiveData<String> getStatusMessage() { return statusMessage; }

    // Get User Data Firestore
    public void fetchUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userLiveData.setValue(documentSnapshot.toObject(User.class));
                    }
                })
                .addOnFailureListener(e -> statusMessage.setValue("Error loading profile"));
    }

    // Data Update (Image URL or Text Fields)
    public void updateProfile(String uid, Map<String, Object> updates) {
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> statusMessage.setValue("Profile Updated!"))
                .addOnFailureListener(e -> statusMessage.setValue("Update Failed: " + e.getMessage()));
    }

}
