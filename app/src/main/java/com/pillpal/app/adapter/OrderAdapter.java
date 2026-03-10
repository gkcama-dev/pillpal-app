package com.pillpal.app.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pillpal.app.R;
import com.pillpal.app.model.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder>{

    private List<Order> orderList;

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_history_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.orderId.setText("#" + order.getOrderId());
        holder.status.setText(order.getStatus());
        holder.date.setText("Date: " + order.getDate());
        holder.total.setText("Total: LKR " + order.getTotal());

        String status = order.getStatus();
        holder.status.setText(status);

        switch (status) {

            case "Pending":
                holder.status.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.orange))
                );
                break;

            case "Approved":
                holder.status.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.green))
                );
                break;

            case "Rejected":
                holder.status.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.red))
                );
                break;

            case "Delivered":
                holder.status.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primaryPurple))
                );
                break;

            default:
                holder.status.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.gray))
                );
        }

    }

    @Override
    public int getItemCount() { return orderList.size(); }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, status, date, total;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            status = itemView.findViewById(R.id.order_status);
            date = itemView.findViewById(R.id.order_date);
            total = itemView.findViewById(R.id.order_total);
        }
    }

}
