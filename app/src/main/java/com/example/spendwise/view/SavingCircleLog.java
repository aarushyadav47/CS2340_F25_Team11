package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.adapter.SavingCircleAdapter;
import com.example.spendwise.databinding.SavingcirclelogBinding;
import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.viewModel.SavingCircleViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.google.android.material.textfield.TextInputEditText;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

public class SavingCircleLog extends AppCompatActivity {
    private SavingCircleViewModel savingCircleViewModel;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    private long dashboardTimestamp; // Store dashboard date as timestamp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        SavingcirclelogBinding binding = SavingcirclelogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel setup
        savingCircleViewModel = new ViewModelProvider(this).get(SavingCircleViewModel.class);
        binding.setLifecycleOwner(this);

        // Receive Dashboard-selected date
        Intent intent = getIntent();
        String dashboardDate = intent.getStringExtra("selected_date");
        if (dashboardDate != null && !dashboardDate.isEmpty()) {
            try {
                Date date = dateFormat.parse(dashboardDate);
                calendar.setTime(date);
                dashboardTimestamp = date.getTime(); // Convert to timestamp
            } catch (ParseException e) {
                e.printStackTrace();
                dashboardTimestamp = System.currentTimeMillis(); // Fallback to current time
            }
        } else {
            dashboardTimestamp = System.currentTimeMillis(); // Fallback to current time
        }

        setupNavBar(dashboardDate);

        View savingCircleForm = findViewById(R.id.form_Container);
        View formScrollView = findViewById(R.id.form_scroll_view);
        View savingCircleMsg = findViewById(R.id.savingCircle_msg);
        View savingCircleRecycler = findViewById(R.id.savingCircle_recycler_view);

        // Add Circle button
        View addCircleButton = findViewById(R.id.add_savingCircle_button);
        addCircleButton.setOnClickListener(v -> {
            clearForm();
            savingCircleForm.setVisibility(View.VISIBLE);
            formScrollView.setVisibility(View.VISIBLE);
            savingCircleRecycler.setVisibility(View.GONE);
            savingCircleMsg.setVisibility(View.GONE);
        });

        // Setup frequency dropdown
        String[] freqOptions = {"Weekly", "Monthly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item, freqOptions);
        ((AutoCompleteTextView) findViewById(R.id.frequencyInput))
                .setAdapter(freqAdapter);

        View createCirclebtn = findViewById(R.id.create_Challenge);
        createCirclebtn.setOnClickListener(v -> saveSavingCircle());

        setUpRecyclerView();
        observeUserEmail();

        View inviteButton = findViewById(R.id.invite_button);
        View viewInvitationsButton = findViewById(R.id.view_invitations_button);
        inviteButton.setOnClickListener(v -> showInviteDialog());
        viewInvitationsButton.setOnClickListener(v -> {
            Intent invitationIntent = new Intent(this, InvitationsActivity.class);
            startActivity(invitationIntent);
        });
    }

    private void setupNavBar(String dashboardDate) {
        View dashboardNavigate = findViewById(R.id.dashboard_navigate);
        View chatbotNavigate = findViewById(R.id.chatbot_navigate);

        dashboardNavigate.setOnClickListener(v -> startActivity(new Intent(this, Dashboard.class)));
        findViewById(R.id.expenseLog_navigate).setOnClickListener(v -> {
            Intent expenseIntent = new Intent(this, ExpenseLog.class);
            expenseIntent.putExtra("selected_date", dashboardDate);
            startActivity(expenseIntent);
        });
        findViewById(R.id.budget_navigate).setOnClickListener(v -> {
            Intent budgetIntent = new Intent(this, Budgetlog.class);
            budgetIntent.putExtra("selected_date", dashboardDate);
            startActivity(budgetIntent);
        });
        findViewById(R.id.savingCircle_navigate).setOnClickListener(v -> {
            Intent savingIntent = new Intent(this, SavingCircleLog.class);
            savingIntent.putExtra("selected_date", dashboardDate);
            startActivity(savingIntent);
        });
        chatbotNavigate.setOnClickListener(v -> startActivity(new Intent(this, Chatbot.class)));
    }

    private void saveSavingCircle() {
        // Get all input fields
        TextInputEditText groupNameInput = findViewById(R.id.groupNameInput);
        TextInputEditText creatorEmailInput = findViewById(R.id.creatorEmailInput);
        TextInputEditText challengeTitleInput = findViewById(R.id.challengeTitleInput);
        TextInputEditText goalAmountInput = findViewById(R.id.goalAmountInput);
        TextInputEditText personalAllocationInput = findViewById(R.id.personalAllocationInput);
        AutoCompleteTextView frequencyInput = findViewById(R.id.frequencyInput);
        TextInputEditText notesInput = findViewById(R.id.notesInput);

        // Get the text from each field
        String groupName = groupNameInput.getText().toString().trim();
        String creatorEmail = creatorEmailInput.getText().toString().trim();
        String challengeTitle = challengeTitleInput.getText().toString().trim();
        String goalAmountStr = goalAmountInput.getText().toString().trim();
        String personalAllocationStr = personalAllocationInput.getText().toString().trim();
        String frequency = frequencyInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        // Validation - Group Name
        if (groupName.isEmpty()) {
            groupNameInput.setError("Group name is required");
            groupNameInput.requestFocus();
            return;
        }

        // Validation - Creator Email (should be auto-filled, but check anyway)
        if (creatorEmail.isEmpty()) {
            creatorEmailInput.setError("Creator email is required");
            creatorEmailInput.requestFocus();
            return;
        }

        // Validation - Challenge Title
        if (challengeTitle.isEmpty()) {
            challengeTitleInput.setError("Challenge title is required");
            challengeTitleInput.requestFocus();
            return;
        }

        // Validation - Goal Amount
        if (goalAmountStr.isEmpty()) {
            goalAmountInput.setError("Goal amount is required");
            goalAmountInput.requestFocus();
            return;
        }

        double goalAmount;
        try {
            goalAmount = Double.parseDouble(goalAmountStr);
            if (goalAmount <= 0) {
                goalAmountInput.setError("Amount must be positive");
                goalAmountInput.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            goalAmountInput.setError("Invalid amount");
            goalAmountInput.requestFocus();
            return;
        }

        // Validation - Personal Allocation
        if (personalAllocationStr.isEmpty()) {
            personalAllocationInput.setError("Personal allocation is required");
            personalAllocationInput.requestFocus();
            return;
        }

        double personalAllocation;
        try {
            personalAllocation = Double.parseDouble(personalAllocationStr);
            if (personalAllocation <= 0) {
                personalAllocationInput.setError("Amount must be positive");
                personalAllocationInput.requestFocus();
                return;
            }
            // Optional: Check if personal allocation exceeds goal amount
            if (personalAllocation > goalAmount) {
                personalAllocationInput.setError("Cannot exceed goal amount");
                personalAllocationInput.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            personalAllocationInput.setError("Invalid amount");
            personalAllocationInput.requestFocus();
            return;
        }

        // Validation - Frequency
        if (frequency.isEmpty()) {
            frequencyInput.setError("Frequency is required");
            frequencyInput.requestFocus();
            return;
        }

        // Validate frequency is either "Weekly" or "Monthly"
        if (!frequency.equals("Weekly") && !frequency.equals("Monthly")) {
            frequencyInput.setError("Please select a valid frequency");
            frequencyInput.requestFocus();
            return;
        }

        // Notes are optional, so no validation needed

        // UPDATED: Pass dashboardTimestamp and frequency to ViewModel
        savingCircleViewModel.addSavingCircle(groupName, creatorEmail, challengeTitle,
                goalAmount, frequency, notes, personalAllocation, dashboardTimestamp);

        // Hide form, show RecyclerView
        View formContainer = findViewById(R.id.form_Container);
        View formScrollView = findViewById(R.id.form_scroll_view);
        View recyclerView = findViewById(R.id.savingCircle_recycler_view);
        View emptyMsg = findViewById(R.id.savingCircle_msg);

        formContainer.setVisibility(View.GONE);
        formScrollView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyMsg.setVisibility(View.GONE);

        clearForm();
    }

    private void clearForm() {
        TextInputEditText groupNameInput = findViewById(R.id.groupNameInput);
        TextInputEditText challengeTitleInput = findViewById(R.id.challengeTitleInput);
        TextInputEditText goalAmountInput = findViewById(R.id.goalAmountInput);
        TextInputEditText personalAllocationInput = findViewById(R.id.personalAllocationInput);
        AutoCompleteTextView frequencyInput = findViewById(R.id.frequencyInput);
        TextInputEditText notesInput = findViewById(R.id.notesInput);

        groupNameInput.setText("");
        challengeTitleInput.setText("");
        goalAmountInput.setText("");
        personalAllocationInput.setText("");
        frequencyInput.setText("");
        notesInput.setText("");

        // Clear any errors
        groupNameInput.setError(null);
        challengeTitleInput.setError(null);
        goalAmountInput.setError(null);
        personalAllocationInput.setError(null);
        frequencyInput.setError(null);
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.savingCircle_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SavingCircleAdapter adapter = new SavingCircleAdapter();
        recyclerView.setAdapter(adapter);

        // Observe savingCircles from Firebase
        savingCircleViewModel.getSavingCircles().observe(this, savingCircles -> {
            adapter.setSavingCircles(savingCircles);
            View savingCircleMsg = findViewById(R.id.savingCircle_msg);
            savingCircleMsg.setVisibility(savingCircles.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // Pass the dashboard timestamp to detail activity
        adapter.setOnItemClickListener(savingCircle -> {
            Intent intent = new Intent(this, SavingCircleDetailActivity.class);
            intent.putExtra("CIRCLE_ID", savingCircle.getId());
            intent.putExtra("SELECTED_DATE", dashboardTimestamp);
            Log.d("SavingCircleLog", "Opening detail with date: " + dateFormat.format(dashboardTimestamp));
            startActivity(intent);
        });

        // Swipe to delete
        setupSwipeToDelete(recyclerView, adapter);
    }

    private void setupSwipeToDelete(RecyclerView recyclerView, SavingCircleAdapter adapter) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String savingCircleId = adapter.getSavingCircleAt(position).getId();
                    savingCircleViewModel.deleteSavingCircle(savingCircleId);
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void observeUserEmail() {
        TextInputEditText creatorEmailInput = findViewById(R.id.creatorEmailInput);

        savingCircleViewModel.getCurrentUserEmail().observe(this, email -> {
            if (email != null && !email.isEmpty()) {
                creatorEmailInput.setText(email);
                // Make it read-only so users can't change it
                creatorEmailInput.setEnabled(false);
                creatorEmailInput.setFocusable(false);
            }
        });
    }


    // Add this method to show the invitation dialog
    private void showInviteDialog() {
        // Create dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_send_invitation, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        // Get views from dialog
        AutoCompleteTextView circleDropdown = dialogView.findViewById(R.id.circleDropdown);
        TextInputEditText inviteeEmailInput = dialogView.findViewById(R.id.inviteeEmailInput);
        View sendButton = dialogView.findViewById(R.id.sendButton);
        View cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Load user's circles into dropdown
        savingCircleViewModel.getSavingCircles().observe(this, circles -> {
            if (circles != null && !circles.isEmpty()) {
                // Filter to only show circles where user is the creator
                List<SavingCircle> ownedCircles = new ArrayList<>();
                String currentUserEmail = savingCircleViewModel.getCurrentUserEmail().getValue();

                for (SavingCircle circle : circles) {
                    if (circle.getCreatorEmail().equals(currentUserEmail)) {
                        ownedCircles.add(circle);
                    }
                }

                if (ownedCircles.isEmpty()) {
                    Toast.makeText(this, "You don't own any circles to invite people to",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }

                // Create dropdown items with circle names
                String[] circleNames = new String[ownedCircles.size()];
                for (int i = 0; i < ownedCircles.size(); i++) {
                    circleNames[i] = ownedCircles.get(i).getGroupName() + " - " +
                            ownedCircles.get(i).getChallengeTitle();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, circleNames);
                circleDropdown.setAdapter(adapter);

                // Handle send button click
                sendButton.setOnClickListener(v -> {
                    int selectedPosition = -1;
                    String selectedText = circleDropdown.getText().toString();

                    // Find which circle was selected
                    for (int i = 0; i < circleNames.length; i++) {
                        if (circleNames[i].equals(selectedText)) {
                            selectedPosition = i;
                            break;
                        }
                    }

                    if (selectedPosition == -1) {
                        Toast.makeText(this, "Please select a circle", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String inviteeEmail = inviteeEmailInput.getText().toString().trim();

                    if (inviteeEmail.isEmpty()) {
                        inviteeEmailInput.setError("Email is required");
                        inviteeEmailInput.requestFocus();
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(inviteeEmail).matches()) {
                        inviteeEmailInput.setError("Invalid email address");
                        inviteeEmailInput.requestFocus();
                        return;
                    }

                    // Check if inviting self
                    if (inviteeEmail.equals(currentUserEmail)) {
                        Toast.makeText(this, "You cannot invite yourself", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SavingCircle selectedCircle = ownedCircles.get(selectedPosition);

                    // Send invitation
                    savingCircleViewModel.sendInvitation(selectedCircle.getId(), inviteeEmail,
                            new SavingCircleViewModel.OnInvitationSentListener() {
                                @Override
                                public void onInvitationSent() {
                                    Toast.makeText(SavingCircleLog.this,
                                            "Invitation sent to " + inviteeEmail, Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(SavingCircleLog.this,
                                            "Error: " + message, Toast.LENGTH_SHORT).show();
                                }
                            });
                });
            } else {
                Toast.makeText(this, "You don't have any saving circles yet",
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}