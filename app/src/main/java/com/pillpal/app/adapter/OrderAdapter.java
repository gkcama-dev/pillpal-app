package com.pillpal.app.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pillpal.app.R;
import com.pillpal.app.model.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
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
        holder.date.setText("Date: " + order.getDate());
        holder.status.setText(order.getStatus());

        // Default values
        holder.total.setVisibility(View.GONE);
        holder.btnPayNow.setVisibility(View.GONE);
        holder.btnMarkReceived.setVisibility(View.GONE);

        String status = (order.getStatus() != null) ? order.getStatus() : "Pending";
        holder.status.setText(status);

        switch (status) {
            case "Pending":
                holder.status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.orange)));
                break;

            case "Approved":
                holder.status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.green)));
                holder.total.setVisibility(View.VISIBLE);
                holder.total.setText("Total Amount: LKR " + order.getTotal());
                holder.btnPayNow.setVisibility(View.VISIBLE);
                break;

            case "Payment Done":
                holder.status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue))); // Blue for payment success
                holder.total.setVisibility(View.VISIBLE);
                holder.total.setText("Total Paid: LKR " + order.getTotal());
                break;

            case "Accepted":
                holder.status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primaryPurple)));
                break;

            case "Delivered":
                holder.status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.green)));
                holder.btnMarkReceived.setVisibility(View.VISIBLE);
                break;

            case "Rejected":
                holder.status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.red)));
                break;
        }

        holder.itemView.setOnClickListener(v -> listener.onOrderClick(order));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public void updateList(List<Order> newList) {
        this.orderList = newList;
        notifyDataSetChanged();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, status, date, total;
        Button btnPayNow,btnMarkReceived;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            status = itemView.findViewById(R.id.order_status);
            date = itemView.findViewById(R.id.order_date);
            total = itemView.findViewById(R.id.order_total);
            btnPayNow = itemView.findViewById(R.id.btn_pay_now);
            btnMarkReceived = itemView.findViewById(R.id.btn_mark_received);
        }
    }

}
