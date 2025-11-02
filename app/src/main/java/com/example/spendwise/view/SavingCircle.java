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
import com.example.spendwise.databinding.SavingcircleBinding;
import com.example.spendwise.viewModel.SavingCircleViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.viewModel.SavingCircleViewModel;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.AutoCompleteTextView;

public class SavingCircle extends AppCompatActivity {
    private SavingCircleViewModel savingCircleViewModel;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        SavingcircleBinding binding = SavingcircleBinding.inflate(getLayoutInflater());
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
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        setupNavBar(dashboardDate);

        View savingCircleForm = findViewById(R.id.form_Container);
        View savingCircleMsg = findViewById(R.id.savingCircle_msg);
        View savingCircleRecycler = findViewById(R.id.savingCircle_recycler_view);

        // Add Circle button
        View addCircleButton = findViewById(R.id.add_savingCircle_button);
        addCircleButton.setOnClickListener(v -> {
            clearForm();
            savingCircleForm.setVisibility(View.VISIBLE);
            savingCircleRecycler.setVisibility(View.GONE);
            savingCircleMsg.setVisibility(View.GONE);
        });

        View createCirclebtn =findViewById(R.id.create_Challenge);
        createCirclebtn.setOnClickListener(v -> saveSavingCircle());

        setUpRecyclerView();
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
            Intent savingIntent = new Intent(this, SavingCircle.class);
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
        AutoCompleteTextView frequencyInput = findViewById(R.id.frequencyInput);
        TextInputEditText notesInput = findViewById(R.id.notesInput);

        // Get the text from each field
        String groupName = groupNameInput.getText().toString().trim();
        String creatorEmail = creatorEmailInput.getText().toString().trim();
        String challengeTitle = challengeTitleInput.getText().toString().trim();
        String goalAmountStr = goalAmountInput.getText().toString().trim();
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

        // Save to Firebase through ViewModel
        savingCircleViewModel.addSavingCircle(groupName, creatorEmail, challengeTitle, goalAmount, frequency, notes);

        // Hide form, show RecyclerView
        View formContainer = findViewById(R.id.form_Container);
        View recyclerView = findViewById(R.id.savingCircle_recycler_view);
        View emptyMsg = findViewById(R.id.savingCircle_msg);

        formContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyMsg.setVisibility(View.GONE);

        clearForm();
    }

    private void clearForm() {
        TextInputEditText groupNameInput = findViewById(R.id.groupNameInput);
        TextInputEditText challengeTitleInput = findViewById(R.id.challengeTitleInput);
        TextInputEditText goalAmountInput = findViewById(R.id.goalAmountInput);
        AutoCompleteTextView frequencyInput = findViewById(R.id.frequencyInput);
        TextInputEditText notesInput = findViewById(R.id.notesInput);

        groupNameInput.setText("");
        challengeTitleInput.setText("");
        goalAmountInput.setText("");
        frequencyInput.setText("");
        notesInput.setText("");

        // Clear any errors
        groupNameInput.setError(null);
        challengeTitleInput.setError(null);
        goalAmountInput.setError(null);
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
                String savingCircleId = adapter.getSavingCircleAt(viewHolder.getAdapterPosition()).getId();
                savingCircleViewModel.deleteSavingCircle(savingCircleId);
            }
        }).attachToRecyclerView(recyclerView);
    }
}

