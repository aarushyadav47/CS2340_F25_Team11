package com.example.spendwise.view;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.adapter.InvitationAdapter;
import com.example.spendwise.model.SavingCircleInvitation;
import com.example.spendwise.viewModel.SavingCircleViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class InvitationsActivity extends AppCompatActivity implements InvitationAdapter.InvitationActionHandler {

    private SavingCircleViewModel savingCircleViewModel;
    private InvitationAdapter invitationAdapter;
    private View emptyState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        savingCircleViewModel = new ViewModelProvider(this).get(SavingCircleViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.invitations_list);
        emptyState = findViewById(R.id.invitations_empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        invitationAdapter = new InvitationAdapter();
        invitationAdapter.setActionHandler(this);
        recyclerView.setAdapter(invitationAdapter);

        savingCircleViewModel.getInvitations().observe(this, invitations -> {
            invitationAdapter.setInvitations(invitations);
            if (invitations == null || invitations.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onAccept(SavingCircleInvitation invitation) {
        showPersonalAllocationDialog(invitation);
    }

    private void showPersonalAllocationDialog(SavingCircleInvitation invitation) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_personal_allocation, null);
        
        TextView totalGoalAmount = dialogView.findViewById(R.id.totalGoalAmount);
        TextInputEditText allocationInput = dialogView.findViewById(R.id.personalAllocationInput);
        TextInputLayout allocationLayout = dialogView.findViewById(R.id.personalAllocationLayout);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelAllocationButton);
        MaterialButton acceptButton = dialogView.findViewById(R.id.acceptAllocationButton);

        double goalAmount = invitation.getGoalAmount();
        totalGoalAmount.setText(String.format(Locale.US, "Total Goal: $%.2f", goalAmount));

        Dialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        acceptButton.setOnClickListener(v -> {
            String allocationText = allocationInput.getText().toString().trim();
            
            if (TextUtils.isEmpty(allocationText)) {
                allocationLayout.setError("Please enter your personal allocation");
                return;
            }

            double personalAllocation;
            try {
                personalAllocation = Double.parseDouble(allocationText);
            } catch (NumberFormatException e) {
                allocationLayout.setError("Please enter a valid number");
                return;
            }

            if (personalAllocation < 0) {
                allocationLayout.setError("Allocation must be non-negative");
                return;
            }

            allocationLayout.setError(null);
            dialog.dismiss();

            // Accept the invitation with the personal allocation
            savingCircleViewModel.acceptInvitation(invitation, personalAllocation, new SavingCircleViewModel.OnInvitationActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(InvitationsActivity.this, "Invitation accepted", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(InvitationsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    @Override
    public void onDecline(SavingCircleInvitation invitation) {
        savingCircleViewModel.declineInvitation(invitation, new SavingCircleViewModel.OnInvitationActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(InvitationsActivity.this, "Invitation declined", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(InvitationsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
