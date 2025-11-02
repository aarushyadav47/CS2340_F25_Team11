package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.lifecycle.ViewModelProvider;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.ExpenselogBinding;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import android.app.DatePickerDialog;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.example.spendwise.adapter.ExpenseAdapter;

import com.example.spendwise.viewModel.ExpenseViewModel;
import com.google.android.material.textfield.TextInputEditText;

import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;

public class ExpenseLog extends AppCompatActivity {

    private ExpenseViewModel expenseViewModel;
    private ExpenselogBinding binding;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        binding = ExpenselogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel setup
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
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

        setupDatePicker();

        // Rest of your initialization...
        expenseViewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        setupNavBar(dashboardDate);


        View expenseLogForm = findViewById(R.id.form_Container);
        View expenseLogMsg = findViewById(R.id.expenseLog_msg);
        View expenseRecycler = findViewById(R.id.expense_recycler_view);

        // Add Expense button
        View addExpenseButton = findViewById(R.id.add_expense_button);
        addExpenseButton.setOnClickListener(v -> {
            clearForm();
            expenseLogForm.setVisibility(View.VISIBLE);
            expenseRecycler.setVisibility(View.GONE);
            expenseLogMsg.setVisibility(View.GONE);
        });
        setupRecyclerView();

        // Category dropdown setup...
        String[] options = {"Food", "Transport", "Entertainment",
                            "Bills", "Health", "Shopping", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, options);
        AutoCompleteTextView dropdown = findViewById(R.id.categoryInput);
        dropdown.setAdapter(adapter);

        // Create Expense button
        View createExpenseBtn = findViewById(R.id.create_Expense);
        createExpenseBtn.setOnClickListener(v -> saveExpense());
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

    private void saveExpense() {
        // Get all input fields
        TextInputEditText expenseNameInput = findViewById(R.id.expenseNameInput);
        TextInputEditText amountInput = findViewById(R.id.amountInput);
        AutoCompleteTextView categoryInput = findViewById(R.id.categoryInput);
        TextInputEditText dateInput = findViewById(R.id.dateInput);
        TextInputEditText notesInput = findViewById(R.id.notesInput);

        // Get the text from each field
        String name = expenseNameInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        String categoryName = categoryInput.getText().toString().trim();
        String date = dateInput.getText().toString();
        String notes = notesInput.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            expenseNameInput.setError("Name is required");
            expenseNameInput.requestFocus();
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

        // Save to Firebase through ViewModel
        expenseViewModel.addExpense(name, amount, category, date, notes);

        // Hide form, show RecyclerView
        View expenseLogForm = findViewById(R.id.form_Container);
        View expenseRecycler = findViewById(R.id.expense_recycler_view);
        View expenseLogMsg = findViewById(R.id.expenseLog_msg);

        expenseLogForm.setVisibility(View.GONE);
        expenseRecycler.setVisibility(View.VISIBLE);
        expenseLogMsg.setVisibility(View.GONE);

        clearForm();
    }

    private void clearForm() {
        TextInputEditText expenseNameInput = findViewById(R.id.expenseNameInput);
        TextInputEditText amountInput = findViewById(R.id.amountInput);
        AutoCompleteTextView categoryInput = findViewById(R.id.categoryInput);
        TextInputEditText dateInput = findViewById(R.id.dateInput);
        TextInputEditText notesInput = findViewById(R.id.notesInput);

        expenseNameInput.setText("");
        amountInput.setText("");
        categoryInput.setText("");
        dateInput.setText(dateFormat.format(calendar.getTime())); // dashboard date
        notesInput.setText("");

        // Clear any errors
        expenseNameInput.setError(null);
        amountInput.setError(null);
        categoryInput.setError(null);

        // Clear the tag (used for storing expense ID when editing)
        expenseNameInput.setTag(null);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.expense_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ExpenseAdapter adapter = new ExpenseAdapter();
        recyclerView.setAdapter(adapter);

        // Observe expenses from Firebase
        expenseViewModel.getExpenses().observe(this, expenses -> {
            // Sort expenses from newest to oldest
            List<Expense> sortedExpenses = new ArrayList<>(expenses);
            Collections.sort(sortedExpenses, new Comparator<Expense>() {
                @Override
                public int compare(Expense e1, Expense e2) {
                    try {
                        Date date1 = dateFormat.parse(e1.getDate());
                        Date date2 = dateFormat.parse(e2.getDate());
                        // Sort in descending order (newest first)
                        return date2.compareTo(date1);
                    } catch (ParseException e) {
                        return 0;
                    }
                }
            });

            adapter.setExpenses(sortedExpenses);

            // Show/hide message based on whether there are expenses
            View expenseLogMsg = findViewById(R.id.expenseLog_msg);
            if (sortedExpenses.isEmpty()) {
                expenseLogMsg.setVisibility(View.VISIBLE);
            } else {
                expenseLogMsg.setVisibility(View.GONE);
            }
        });

        // Swipe to delete
        setupSwipeToDelete(recyclerView, adapter);
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

    private void setupSwipeToDelete(RecyclerView recyclerView, ExpenseAdapter adapter) {
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
                String expenseId = adapter.getExpenseAt(viewHolder.getAdapterPosition()).getId();
                expenseViewModel.deleteExpense(expenseId);
            }
        }).attachToRecyclerView(recyclerView);
    }
}