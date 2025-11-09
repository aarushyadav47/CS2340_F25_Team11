package com.example.spendwise.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.model.SavingCircleInvitation;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder> {

    public interface InvitationActionHandler {
        void onAccept(SavingCircleInvitation invitation);
        void onDecline(SavingCircleInvitation invitation);
    }

    private final List<SavingCircleInvitation> invitations = new ArrayList<>();
    private InvitationActionHandler actionHandler;

    public void setActionHandler(InvitationActionHandler handler) {
        this.actionHandler = handler;
    }

    public void setInvitations(List<SavingCircleInvitation> newInvitations) {
        invitations.clear();
        if (newInvitations != null) {
            invitations.addAll(newInvitations);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        holder.bind(invitations.get(position));
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    class InvitationViewHolder extends RecyclerView.ViewHolder {

        private final TextView challengeTitle;
        private final TextView circleName;
        private final TextView goalAmount;
        private final TextView frequency;
        private final MaterialButton acceptButton;
        private final MaterialButton declineButton;

        InvitationViewHolder(@NonNull View itemView) {
            super(itemView);
            challengeTitle = itemView.findViewById(R.id.invitation_challenge_title);
            circleName = itemView.findViewById(R.id.invitation_circle_name);
            goalAmount = itemView.findViewById(R.id.invitation_goal_amount);
            frequency = itemView.findViewById(R.id.invitation_frequency);
            acceptButton = itemView.findViewById(R.id.invitation_accept_button);
            declineButton = itemView.findViewById(R.id.invitation_decline_button);
        }

        void bind(SavingCircleInvitation invitation) {
            challengeTitle.setText(invitation.getChallengeTitle());
            circleName.setText(invitation.getCircleName());
            goalAmount.setText(String.format(Locale.US, "Goal: $%.2f", invitation.getGoalAmount()));
            frequency.setText(String.format(Locale.US, "Frequency: %s", invitation.getFrequency()));

            if (invitation.isPending()) {
                configurePendingState(invitation);
            } else if (invitation.isAccepted()) {
                configureAcceptedState();
            } else {
                configureDeclinedState();
            }
        }

        private void configurePendingState(SavingCircleInvitation invitation) {
            acceptButton.setEnabled(true);
            acceptButton.setVisibility(View.VISIBLE);
            acceptButton.setText(R.string.invitation_accept);
            int accentColor = ContextCompat.getColor(acceptButton.getContext(), android.R.color.holo_green_dark);
            acceptButton.setBackgroundTintList(ColorStateList.valueOf(accentColor));
            acceptButton.setTextColor(ContextCompat.getColor(acceptButton.getContext(), android.R.color.white));

            declineButton.setEnabled(true);
            declineButton.setVisibility(View.VISIBLE);
            declineButton.setText(R.string.invitation_decline);
            declineButton.setBackgroundTintList(null);

            acceptButton.setOnClickListener(v -> {
                if (actionHandler != null) {
                    actionHandler.onAccept(invitation);
                }
            });

            declineButton.setOnClickListener(v -> {
                if (actionHandler != null) {
                    actionHandler.onDecline(invitation);
                }
            });
        }

        private void configureAcceptedState() {
            acceptButton.setEnabled(false);
            acceptButton.setVisibility(View.VISIBLE);
            acceptButton.setText(R.string.invitation_status_accepted);
            int accentColor = ContextCompat.getColor(acceptButton.getContext(), android.R.color.holo_green_dark);
            acceptButton.setBackgroundTintList(ColorStateList.valueOf(accentColor));
            acceptButton.setTextColor(ContextCompat.getColor(acceptButton.getContext(), android.R.color.white));

            declineButton.setVisibility(View.GONE);
            acceptButton.setOnClickListener(null);
            declineButton.setOnClickListener(null);
        }

        private void configureDeclinedState() {
            declineButton.setEnabled(false);
            declineButton.setVisibility(View.VISIBLE);
            declineButton.setText(R.string.invitation_status_declined);
            declineButton.setBackgroundTintList(null);

            acceptButton.setVisibility(View.GONE);
            acceptButton.setOnClickListener(null);
            declineButton.setOnClickListener(null);
        }
    }
}
