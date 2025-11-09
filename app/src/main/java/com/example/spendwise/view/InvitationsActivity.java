package com.example.spendwise.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.adapter.InvitationAdapter;
import com.example.spendwise.model.SavingCircleInvitation;
import com.example.spendwise.viewModel.SavingCircleViewModel;

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
        savingCircleViewModel.acceptInvitation(invitation, new SavingCircleViewModel.OnInvitationActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(InvitationsActivity.this, "Invitation accepted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(InvitationsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
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
