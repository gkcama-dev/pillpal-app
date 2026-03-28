package com.pillpal.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.pillpal.app.R;
import com.pillpal.app.viewModel.NotificationViewModel;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<DocumentSnapshot> notifications;
    private NotificationViewModel viewModel;

    public NotificationAdapter(List<DocumentSnapshot> notifications, NotificationViewModel viewModel) {
        this.notifications = notifications;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = notifications.get(position);

        if (doc.exists()) {
            holder.title.setText(doc.getString("title"));
            holder.body.setText(doc.getString("body"));

            Boolean isRead = doc.getBoolean("isRead");
            if (isRead == null) isRead = false;

            holder.itemView.setAlpha(isRead ? 0.5f : 1.0f);

            Boolean finalIsRead = isRead;
            holder.itemView.setOnClickListener(v -> {
                if (!finalIsRead && viewModel != null) {
                    viewModel.markAsRead(doc.getId());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, body;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_notif_title);
            body = itemView.findViewById(R.id.tv_notif_body);
        }
    }
}
