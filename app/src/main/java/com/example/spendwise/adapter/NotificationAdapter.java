package com.example.spendwise.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.viewModel.NotificationViewModel.NotificationItem;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationItem> notifications = new ArrayList<>();

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setNotifications(List<NotificationItem> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView titleText;
        private final TextView subtitleText;
        private final TextView timeText;
        private final View urgencyIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.notification_icon);
            titleText = itemView.findViewById(R.id.notification_title);
            subtitleText = itemView.findViewById(R.id.notification_subtitle);
            timeText = itemView.findViewById(R.id.notification_time);
            urgencyIndicator = itemView.findViewById(R.id.urgency_indicator);
        }

        public void bind(NotificationItem item) {
            titleText.setText(item.getTitle());
            subtitleText.setText(item.getSubtitle());
            timeText.setText(item.getTimeMessage());

            // Set icon based on type
            if (item.getType() == NotificationItem.Type.NO_EXPENSES) {
                iconView.setImageResource(R.drawable.ic_budget);
            } else if (item.getType() == NotificationItem.Type.BUDGET_90_PERCENT) {
                iconView.setImageResource(R.drawable.ic_savings);
            }

            // Set urgency color based on days remaining
            if (item.getDaysRemaining() == 0) {
                urgencyIndicator.setBackgroundColor(0xFFFF3B30); // Red
            } else if (item.getDaysRemaining() == 1) {
                urgencyIndicator.setBackgroundColor(0xFFFF9500); // Orange
            } else {
                urgencyIndicator.setBackgroundColor(0xFFFFCC00); // Yellow
            }
        }
    }
}