package com.pillpal.app.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class NotificationViewModel extends ViewModel {
    private MutableLiveData<List<DocumentSnapshot>> notificationsLiveData = new MutableLiveData<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<DocumentSnapshot>> getNotifications(String userId) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        notificationsLiveData.setValue(value.getDocuments());
                    }
                });
        return notificationsLiveData;
    }

    public void markAsRead(String notificationId) {
        db.collection("notifications").document(notificationId).update("isRead", true);
    }
}
