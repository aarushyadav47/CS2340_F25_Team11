package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.lifecycle.ViewModelProvider;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.BudgetlogBinding;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Budget;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import android.app.DatePickerDialog;

import com.example.spendwise.adapter.SavingCircleAdapter;
import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.model.SavingCircleMember;
import com.example.spendwise.model.MemberCycle;
import com.example.spendwise.viewModel.SavingCircleViewModel;
import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.example.spendwise.adapter.BudgetAdapter;

import com.example.spendwise.viewModel.BudgetViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;

import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import android.widget.Button;

public class Budgetlog extends AppCompatActivity {

    private BudgetViewModel budgetViewModel;
    private BudgetlogBinding binding;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy",
            Locale.US);

    private SavingCircleViewModel savingCircleViewModel;
    private SavingCircleAdapter savingCircleAdapter;
    private int currentLoadId = 0;
    private Map<String, Integer> circleLoadIds = new HashMap<>();
    private long dashboardTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate layout
        binding = BudgetlogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel setup
        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        binding.setLifecycleOwner(this);

        // Observe status messages
        budgetViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

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

        // Initialize SavingCircleViewModel
        savingCircleViewModel = new ViewModelProvider(this).get(SavingCircleViewModel.class);

        setupDatePicker();

        // Setup bottom navigation
        findViewById(R.id.dashboard_navigate).setOnClickListener(v ->
                startActivity(new Intent(this, Dashboard.class)));
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
        findViewById(R.id.chatbot_navigate).setOnClickListener(v -> {
            Intent chatbotIntent = new Intent(this, Chatbot.class);
            chatbotIntent.putExtra("selected_date", dashboardDate);
            startActivity(chatbotIntent);
        });

        // Add Budget button
        View budgetLogForm = findViewById(R.id.form_Container);
        View budgetLogMsg = findViewById(R.id.budgetLog_msg);
        View budgetRecycler = findViewById(R.id.budget_recycler_view);

        findViewById(R.id.add_budget_button).setOnClickListener(v -> {
            clearForm();
            budgetLogForm.setVisibility(View.VISIBLE);
            budgetRecycler.setVisibility(View.GONE);
            budgetLogMsg.setVisibility(View.GONE);
            findViewById(R.id.saving_circles_section).setVisibility(View.GONE);
        });

        // Floating Budget Calculator
        FloatingActionButton calcButton = findViewById(R.id.budget_calculator_button);
        calcButton.setOnClickListener(v -> showBudgetCalculatorDialog());

        // Setup RecyclerView
        setupRecyclerView();

        // Setup category dropdown
        String[] categoryOptions = {"Food", "Transport", "Entertainment",
                                    "Bills", "Health", "Shopping", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item, categoryOptions);
        ((AutoCompleteTextView) findViewById(R.id.categoryInput))
                .setAdapter(categoryAdapter);

        // Setup frequency dropdown
        String[] freqOptions = {"Weekly", "Monthly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item, freqOptions);
        ((AutoCompleteTextView) findViewById(R.id.freqInput))
                .setAdapter(freqAdapter);

        // Create Budget button
        findViewById(R.id.create_Budget).setOnClickListener(v -> saveBudget());

        // Setup Saving Circles RecyclerView (NEW)
        setupSavingCirclesRecyclerView();
    }

    private void setupDatePicker() {
        TextInputEditText dateInput = findViewById(R.id.dateInput);

        // Initialize with dashboard date
        dateInput.setText(dateFormat.format(calendar.getTime()));

        dateInput.setFocusable(false);
        dateInput.setClickable(true);

        dateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        dateInput.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void saveBudget() {
        TextInputEditText budgetNameInput = findViewById(R.id.budgetNameInput);
        TextInputEditText amountInput = findViewById(R.id.amountInput);
        AutoCompleteTextView categoryInput = findViewById(R.id.categoryInput);
        TextInputEditText dateInput = findViewById(R.id.dateInput);
        AutoCompleteTextView freqInput = findViewById(R.id.freqInput);

        String name = budgetNameInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        String categoryName = categoryInput.getText().toString().trim();
        String date = dateInput.getText().toString();
        String freq = freqInput.getText().toString().trim();

        if (name.isEmpty()) {
            budgetNameInput.setError("Name is required");
            budgetNameInput.requestFocus();
            return;
        }
        if (amountStr.isEmpty()) {
            amountInput.setError("Amount is required");
            amountInput.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountInput.setError("Amount must be positive");
                amountInput.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            amountInput.setError("Invalid amount");
            amountInput.requestFocus();
            return;
        }

        if (categoryName.isEmpty()) {
            categoryInput.setError("Category is required");
            categoryInput.requestFocus();
            return;
        }

        Category category = null;
        for (Category cat : Category.values()) {
            if (cat.getDisplayName().equals(categoryName)) {
                category = cat;
                break;
            }
        }
        if (category == null) {
            categoryInput.setError("Please select a valid category");
            categoryInput.requestFocus();
            return;
        }

        if (freq.isEmpty()) {
            freqInput.setError("Frequency is required");
            freqInput.requestFocus();
            return;
        }

        final Category selectedCategory = category;
        final String selectedFreq = freq;
        final double finalAmount = amount;

        // Check if ANY budget exists for this category (regardless of frequency)
        budgetViewModel.getBudgets().observe(this,
                new androidx.lifecycle.Observer<List<Budget>>() {
                    @Override
                    public void onChanged(List<Budget> budgets) {
                        boolean exists = false;
                        for (Budget b : budgets) {
                            if (b.getCategory() == selectedCategory) {
                                exists = true;
                                break;
                            }
                        }

                        if (exists) {
                            Toast.makeText(Budgetlog.this,
                                    "A budget already exists for " + categoryName
                                            + ". Delete the existing one first.",
                                    Toast.LENGTH_LONG).show();
                            categoryInput.setError("Budget already exists for this category");
                            categoryInput.requestFocus();
                        } else {
                            budgetViewModel.addBudget(name, finalAmount,
                                    selectedCategory, date, selectedFreq);

                            findViewById(R.id.form_Container).setVisibility(View.GONE);
                            findViewById(R.id.budget_recycler_view)
                                    .setVisibility(View.VISIBLE);
                            findViewById(R.id.budgetLog_msg).setVisibility(View.GONE);
                            findViewById(R.id.saving_circles_section).setVisibility(View.VISIBLE);

                            clearForm();
                        }

                        // Remove this specific observer after handling
                        budgetViewModel.getBudgets().removeObserver(this);
                    }
                });
    }

    private void clearForm() {
        TextInputEditText budgetNameInput = findViewById(R.id.budgetNameInput);
        TextInputEditText amountInput = findViewById(R.id.amountInput);
        AutoCompleteTextView categoryInput = findViewById(R.id.categoryInput);
        TextInputEditText dateInput = findViewById(R.id.dateInput);
        AutoCompleteTextView freqInput = findViewById(R.id.freqInput);

        budgetNameInput.setText("");
        amountInput.setText("");
        categoryInput.setText("");
        dateInput.setText(dateFormat.format(calendar.getTime())); // dashboard date
        freqInput.setText("");

        // Clear any errors
        budgetNameInput.setError(null);
        amountInput.setError(null);
        categoryInput.setError(null);

        // Clear the tag (used for storing expense ID when editing)
        budgetNameInput.setTag(null);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.budget_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        BudgetAdapter adapter = new BudgetAdapter();
        recyclerView.setAdapter(adapter);

        budgetViewModel.getBudgets().observe(this, budgets -> {
            List<Budget> sorted = new ArrayList<>(budgets);
            Collections.sort(sorted, (b1, b2) -> {
                try {
                    Date d1 = dateFormat.parse(b1.getDate());
                    Date d2 = dateFormat.parse(b2.getDate());
                    return d2.compareTo(d1);
                } catch (ParseException e) {
                    return 0;
                }
            });

            adapter.setBudgets(sorted);
            findViewById(R.id.budgetLog_msg).setVisibility(sorted.isEmpty()
                    ? View.VISIBLE : View.GONE);
        });

        setupSwipeToDelete(recyclerView, adapter);
    }

    private void setupSwipeToDelete(RecyclerView recyclerView,
                                    BudgetAdapter adapter) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView r, RecyclerView.ViewHolder vh,
                                  RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder vh, int dir) {
                String budgetId = adapter.getBudgetAt(vh.getAdapterPosition())
                        .getId();
                budgetViewModel.deleteBudget(budgetId);
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void showBudgetCalculatorDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.budget_calculator_popup,
                null);

        EditText totalBudgetInput = dialogView.findViewById(R.id.total_budget_input);
        EditText spentInput = dialogView.findViewById(R.id.spent_input);
        EditText remainingInput = dialogView.findViewById(R.id.remaining_input);
        Button calcButton = dialogView.findViewById(R.id.calculate_button);

        calcButton.setOnClickListener(v -> {
            String totalStr = totalBudgetInput.getText().toString();
            String spentStr = spentInput.getText().toString();
            String remainingStr = remainingInput.getText().toString();

            int filled = 0;
            if (!totalStr.isEmpty()) {
                filled++;
            }
            if (!spentStr.isEmpty()) {
                filled++;
            }
            if (!remainingStr.isEmpty()) {
                filled++;
            }
            if (filled < 2) {
                Toast.makeText(this,
                        "Fill any two fields to compute the third.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                if (totalStr.isEmpty()) {
                    double spent = Double.parseDouble(spentStr);
                    double rem = Double.parseDouble(remainingStr);
                    totalBudgetInput.setText(String.format("%.2f", spent + rem));
                } else if (spentStr.isEmpty()) {
                    double total = Double.parseDouble(totalStr);
                    double rem = Double.parseDouble(remainingStr);
                    spentInput.setText(String.format("%.2f", total - rem));
                } else if (remainingStr.isEmpty()) {
                    double total = Double.parseDouble(totalStr);
                    double spent = Double.parseDouble(spentStr);
                    remainingInput.setText(String.format("%.2f", total - spent));
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format",
                        Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView)
                .setNegativeButton("Close", (d, w) -> d.dismiss()).create();
        dialog.show();
    }

    // Add this new method to setup the saving circles RecyclerView
    private void setupSavingCirclesRecyclerView() {
        RecyclerView savingCirclesRecycler = findViewById(R.id.saving_circles_recycler_view);
        savingCirclesRecycler.setLayoutManager(new LinearLayoutManager(this));

        savingCircleAdapter = new SavingCircleAdapter();
        savingCirclesRecycler.setAdapter(savingCircleAdapter);

        final boolean[] isFirstLoad = {true};

        savingCircleViewModel.getSavingCircles().observe(this, savingCircles -> {
            savingCircleAdapter.setSavingCircles(savingCircles);

            // Show/hide empty message
            View emptyMsg = findViewById(R.id.saving_circles_empty_msg);
            emptyMsg.setVisibility(savingCircles.isEmpty() ? View.VISIBLE : View.GONE);

            // Calculate progress for all circles
            if (!isFirstLoad[0]) {
                currentLoadId++;
            } else {
                isFirstLoad[0] = false;
            }

            if (savingCircles != null && !savingCircles.isEmpty()) {
                for (SavingCircle circle : savingCircles) {
                    calculateCircleProgress(circle, currentLoadId);
                }
            }
        });

        // Optional: Add click listener to navigate to detail
        savingCircleAdapter.setOnItemClickListener(savingCircle -> {
            Intent detailIntent = new Intent(this, SavingCircleDetailActivity.class);
            detailIntent.putExtra("CIRCLE_ID", savingCircle.getId());
            detailIntent.putExtra("SELECTED_DATE", dashboardTimestamp);
            startActivity(detailIntent);
        });
    }

    // Add this method to calculate circle progress
    private void calculateCircleProgress(SavingCircle circle, int loadId) {
        String circleId = circle.getId();
        circleLoadIds.put(circleId, loadId);

        savingCircleViewModel.getSavingCircleMembersOnce(circleId, members -> {
            Integer currentCircleLoadId = circleLoadIds.get(circleId);
            if (currentCircleLoadId == null || currentCircleLoadId != loadId) {
                return;
            }

            if (members == null || members.isEmpty()) {
                savingCircleAdapter.setCircleProgress(circleId, 0, circle.getGoalAmount());
                return;
            }

            final int[] membersProcessed = {0};
            final double[] totalProgress = {0};
            final int totalMembers = members.size();

            for (SavingCircleMember member : members) {
                if (member.getJoinedAt() > dashboardTimestamp) {
                    synchronized (membersProcessed) {
                        membersProcessed[0]++;
                        if (membersProcessed[0] >= totalMembers) {
                            savingCircleAdapter.setCircleProgress(circleId, totalProgress[0], circle.getGoalAmount());
                        }
                    }
                    continue;
                }

                savingCircleViewModel.getMemberCycleHistoryOnce(circleId, member.getEmail(), cycles -> {
                    Integer loadIdCheck = circleLoadIds.get(circleId);
                    if (loadIdCheck == null || loadIdCheck != loadId) {
                        return;
                    }

                    double memberContribution = 0;
                    if (cycles != null && !cycles.isEmpty()) {
                        for (MemberCycle cycle : cycles) {
                            if (cycle.isComplete() && cycle.getEndDate() <= dashboardTimestamp) {
                                memberContribution += cycle.getEndAmount();
                            }
                        }
                    }

                    synchronized (membersProcessed) {
                        totalProgress[0] += memberContribution;
                        membersProcessed[0]++;

                        if (membersProcessed[0] >= totalMembers) {
                            savingCircleAdapter.setCircleProgress(circleId, totalProgress[0], circle.getGoalAmount());
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recalculate saving circles progress when returning to this page
        currentLoadId++;
        List<SavingCircle> currentCircles = savingCircleViewModel.getSavingCircles().getValue();
        if (currentCircles != null && !currentCircles.isEmpty()) {
            for (SavingCircle circle : currentCircles) {
                calculateCircleProgress(circle, currentLoadId);
            }
        }
    }
}