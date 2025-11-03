package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.adapter.SavingCircleAdapter;
import com.example.spendwise.databinding.SavingcirclelogBinding;
import com.example.spendwise.viewModel.SavingCircleViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;

import com.google.android.material.textfield.TextInputEditText;

import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

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

        // UPDATED: Pass dashboardTimestamp to ViewModel
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

            // Show/hide message based on whether there are saving circles
            View savingCircleMsg = findViewById(R.id.savingCircle_msg);
            if (savingCircles.isEmpty()) {
                savingCircleMsg.setVisibility(View.VISIBLE);
            } else {
                savingCircleMsg.setVisibility(View.GONE);
            }
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
}