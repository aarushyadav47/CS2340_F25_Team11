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
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;

import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.card.MaterialCardView;
import android.widget.Button;

public class Budgetlog extends AppCompatActivity {

    private BudgetViewModel budgetViewModel;
    private BudgetlogBinding binding;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        binding = BudgetlogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel setup
        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        binding.setLifecycleOwner(this);

        budgetViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Use findViewById for buttons and views
        View dashboardNavigate = findViewById(R.id.dashboard_navigate);
        View expenseLogNavigate = findViewById(R.id.expenseLog_navigate);
        View budgetNavigate = findViewById(R.id.budget_navigate);
        View savingCircleNavigate = findViewById(R.id.savingCircle_navigate);
        View chatbotNavigate = findViewById(R.id.chatbot_navigate);

        View budgetLogForm = findViewById(R.id.form_Container);
        View budgetLogMsg = findViewById(R.id.budgetLog_msg);
        View budgetRecycler = findViewById(R.id.budget_recycler_view);

        // Set click listeners using lambdas for the routing
        dashboardNavigate.setOnClickListener(v -> startActivity(new Intent(this, Dashboard.class)));

        expenseLogNavigate.setOnClickListener(v -> startActivity(new Intent(this, ExpenseLog.class)));

        budgetNavigate.setOnClickListener(v -> startActivity(new Intent(this, Budgetlog.class)));

        savingCircleNavigate.setOnClickListener(v -> startActivity(new Intent(this, SavingCircle.class)));

        chatbotNavigate.setOnClickListener(v -> startActivity(new Intent(this, Chatbot.class)));

        // Add Budget button
        View addBudgetButton = findViewById(R.id.add_budget_button);
        addBudgetButton.setOnClickListener(v -> {
            clearForm();
            budgetLogForm.setVisibility(View.VISIBLE);
            budgetRecycler.setVisibility(View.GONE);
            budgetLogMsg.setVisibility(View.GONE);
        });

        FloatingActionButton calcButton = findViewById(R.id.budget_calculator_button);
        calcButton.setOnClickListener(v -> showBudgetCalculatorDialog());

        setupDatePicker();
        setupRecyclerView();

        // Setup category dropdown
        String[] categoryOptions = {"Food", "Transport", "Entertainment", "Bills", "Health", "Shopping", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item, categoryOptions);

        AutoCompleteTextView categoryDropdown = findViewById(R.id.categoryInput);
        categoryDropdown.setAdapter(categoryAdapter);

        // Setup frequency dropdown
        String[] freqOptions = {"Weekly", "Monthly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item, freqOptions);

        AutoCompleteTextView freqDropdown = findViewById(R.id.freqInput);
        freqDropdown.setAdapter(freqAdapter);

        // Create Budget button
        View createBudgetBtn = findViewById(R.id.create_Budget);
        createBudgetBtn.setOnClickListener(v -> {
            saveBudget();
        });
    }

    private void setupDatePicker() {
        TextInputEditText dateInput = findViewById(R.id.dateInput);

        // Set today's date as default
        dateInput.setText(dateFormat.format(calendar.getTime()));

        // Make it non-editable but clickable
        dateInput.setFocusable(false);
        dateInput.setClickable(true);

        // Show date picker on click
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
        // Get all input fields
        TextInputEditText budgetNameInput = findViewById(R.id.budgetNameInput);
        TextInputEditText amountInput = findViewById(R.id.amountInput);
        AutoCompleteTextView categoryInput = findViewById(R.id.categoryInput);
        TextInputEditText dateInput = findViewById(R.id.dateInput);
        AutoCompleteTextView freqInput = findViewById(R.id.freqInput);

        // Get the text from each field
        String name = budgetNameInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        String categoryName = categoryInput.getText().toString().trim();
        String date = dateInput.getText().toString();
        String freq = freqInput.getText().toString().trim();

        // Validation
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

        // Find matching category from your enum
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

        // **NEW CODE: Check if a budget already exists for this category**
        final Category selectedCategory = category;
        final String selectedFreq = freq;
        final double finalAmount = amount;

        budgetViewModel.getBudgets().observe(this, budgets -> {
            boolean categoryExists = false;

            for (Budget existingBudget : budgets) {
                if (existingBudget.getCategory() == selectedCategory &&
                        existingBudget.getfreq().equalsIgnoreCase(selectedFreq)) {
                    categoryExists = true;
                    break;
                }
            }

            if (categoryExists) {
                Toast.makeText(this,
                        "A " + selectedFreq.toLowerCase() + " budget already exists for " + categoryName +
                                ". Please delete it first or choose a different category.",
                        Toast.LENGTH_LONG).show();
                categoryInput.setError("Budget already exists for this category");
                categoryInput.requestFocus();
                // Remove the observer to prevent multiple triggers
                budgetViewModel.getBudgets().removeObservers(this);
            } else {
                // Save to Firebase through ViewModel
                budgetViewModel.addBudget(name, finalAmount, selectedCategory, date, selectedFreq);

                // Hide form, show RecyclerView
                View budgetLogForm = findViewById(R.id.form_Container);
                View budgetRecycler = findViewById(R.id.budget_recycler_view);
                View budgetLogMsg = findViewById(R.id.budgetLog_msg);

                budgetLogForm.setVisibility(View.GONE);
                budgetRecycler.setVisibility(View.VISIBLE);
                budgetLogMsg.setVisibility(View.GONE);

                clearForm();

                // Remove the observer to prevent multiple triggers
                budgetViewModel.getBudgets().removeObservers(this);
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
        dateInput.setText(dateFormat.format(calendar.getTime())); // Reset to today
        freqInput.setText("");

        // Clear any errors
        budgetNameInput.setError(null);
        amountInput.setError(null);
        categoryInput.setError(null);
        freqInput.setError(null);

        // Clear the tag (used for storing budget ID when editing)
        budgetNameInput.setTag(null);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.budget_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        BudgetAdapter adapter = new BudgetAdapter();
        recyclerView.setAdapter(adapter);

        // Observe budgets from Firebase
        budgetViewModel.getBudgets().observe(this, budgets -> {
            // Sort budgets from newest to oldest
            List<Budget> sortedBudgets = new ArrayList<>(budgets);
            Collections.sort(sortedBudgets, new Comparator<Budget>() {
                @Override
                public int compare(Budget b1, Budget b2) {
                    try {
                        Date date1 = dateFormat.parse(b1.getDate());
                        Date date2 = dateFormat.parse(b2.getDate());
                        // Sort in descending order (newest first)
                        return date2.compareTo(date1);
                    } catch (ParseException e) {
                        return 0;
                    }
                }
            });

            adapter.setBudgets(sortedBudgets);

            // Show/hide message based on whether there are budgets
            View budgetLogMsg = findViewById(R.id.budgetLog_msg);
            if (sortedBudgets.isEmpty()) {
                budgetLogMsg.setVisibility(View.VISIBLE);
            } else {
                budgetLogMsg.setVisibility(View.GONE);
            }
        });

        // Swipe to delete
        setupSwipeToDelete(recyclerView, adapter);
    }

    private void setupSwipeToDelete(RecyclerView recyclerView, BudgetAdapter adapter) {
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
                String budgetId = adapter.getBudgetAt(viewHolder.getAdapterPosition()).getId();
                budgetViewModel.deleteBudget(budgetId);
            }
        }).attachToRecyclerView(recyclerView);
    }


    private void showBudgetCalculatorDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.budget_calculator_popup, null);

        EditText totalBudgetInput = dialogView.findViewById(R.id.total_budget_input);
        EditText spentInput = dialogView.findViewById(R.id.spent_input);
        EditText remainingInput = dialogView.findViewById(R.id.remaining_input);
        Button calcButton = dialogView.findViewById(R.id.calculate_button);

        calcButton.setOnClickListener(v -> {
            String totalStr = totalBudgetInput.getText().toString();
            String spentStr = spentInput.getText().toString();
            String remainingStr = remainingInput.getText().toString();

            int filledCount = 0;
            if (!totalStr.isEmpty()) filledCount++;
            if (!spentStr.isEmpty()) filledCount++;
            if (!remainingStr.isEmpty()) filledCount++;

            if (filledCount < 2) {
                Toast.makeText(this, "Fill any two fields to compute the third.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                if (totalStr.isEmpty()) {
                    double spent = Double.parseDouble(spentStr);
                    double remaining = Double.parseDouble(remainingStr);
                    totalBudgetInput.setText(String.format("%.2f", spent + remaining));
                } else if (spentStr.isEmpty()) {
                    double total = Double.parseDouble(totalStr);
                    double remaining = Double.parseDouble(remainingStr);
                    spentInput.setText(String.format("%.2f", total - remaining));
                } else if (remainingStr.isEmpty()) {
                    double total = Double.parseDouble(totalStr);
                    double spent = Double.parseDouble(spentStr);
                    remainingInput.setText(String.format("%.2f", total - spent));
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Close", (d, which) -> d.dismiss())
                .create();

        dialog.show();
    }
}