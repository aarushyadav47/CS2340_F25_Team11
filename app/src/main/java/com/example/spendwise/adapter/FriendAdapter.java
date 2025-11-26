package com.example.spendwise.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;

import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<String> friendEmails = new ArrayList<>();
    private OnRemoveClickListener removeClickListener;

    public interface OnRemoveClickListener {
        void onRemoveClick(String email);
    }

    public void setFriendEmails(List<String> friendEmails) {
        this.friendEmails = friendEmails;
        notifyDataSetChanged();
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.removeClickListener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        String email = friendEmails.get(position);
        holder.emailText.setText(email);
        holder.removeButton.setOnClickListener(v -> {
            if (removeClickListener != null) {
                removeClickListener.onRemoveClick(email);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendEmails.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView emailText;
        Button removeButton;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            emailText = itemView.findViewById(R.id.friend_email);
            removeButton = itemView.findViewById(R.id.remove_friend_button);
        }
    }
}
