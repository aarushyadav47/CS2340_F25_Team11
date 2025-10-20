package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ArrayAdapter;
import androidx.lifecycle.ViewModelProvider;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.ExpenselogBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.app.DatePickerDialog;
import com.example.spendwise.model.Category;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.example.spendwise.adapter.ExpenseAdapter;

import com.example.spendwise.viewModel.ExpenseViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

public class ExpenseLog extends AppCompatActivity {

    private ExpenseViewModel expenseViewModel;
    private ExpenselogBinding binding;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        binding = ExpenselogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel setup
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        binding.setLifecycleOwner(this);

        expenseViewModel.getStatusMessage().observe(this, msg -> {
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

        View expenseLogForm = findViewById(R.id.form_Container);
        View expenseLogMsg = findViewById(R.id.expenseLog_msg);
        View expenseRecycler = findViewById(R.id.expense_recycler_view);

        // Set click listeners using lambdas for the routing
        dashboardNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, Dashboard.class))
        );

        expenseLogNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, ExpenseLog.class))
        );

        budgetNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, Budget.class))
        );

        savingCircleNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, SavingCircle.class))
        );

        chatbotNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, Chatbot.class))
        );

        // Add Expense button
        View addExpenseButton = findViewById(R.id.add_expense_button);
        addExpenseButton.setOnClickListener(v -> {
            clearForm();
            expenseLogForm.setVisibility(View.VISIBLE);
            expenseRecycler.setVisibility(View.GONE);
            expenseLogMsg.setVisibility(View.GONE);
        });

        setupDatePicker();
        setupRecyclerView();

        // Setup category dropdown
        String[] options = {"Food", "Transport", "Entertainment", "Bills", "Health", "Shopping"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item, options);

        AutoCompleteTextView dropdown = findViewById(R.id.categoryInput);
        dropdown.setAdapter(adapter);

        // Create Expense button
        View createExpenseBtn = findViewById(R.id.create_Expense);
        createExpenseBtn.setOnClickListener(v -> {
            saveExpense();
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
        dateInput.setText(dateFormat.format(calendar.getTime())); // Reset to today
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
            adapter.setExpenses(expenses);

            // Show/hide message based on whether there are expenses
            View expenseLogMsg = findViewById(R.id.expenseLog_msg);
            if (expenses.isEmpty()) {
                expenseLogMsg.setVisibility(View.VISIBLE);
            } else {
                expenseLogMsg.setVisibility(View.GONE);
            }
        });

        // Click to edit expense
        adapter.setOnItemClickListener(expense -> {
            // Show the form with existing expense data
            View expenseLogForm = findViewById(R.id.form_Container);
            View expenseRecycler = findViewById(R.id.expense_recycler_view);
            View expenseLogMsg = findViewById(R.id.expenseLog_msg);

            expenseLogForm.setVisibility(View.VISIBLE);
            expenseRecycler.setVisibility(View.GONE);
            expenseLogMsg.setVisibility(View.GONE);

            // Populate form fields with expense data
            TextInputEditText expenseNameInput = findViewById(R.id.expenseNameInput);
            TextInputEditText amountInput = findViewById(R.id.amountInput);
            AutoCompleteTextView categoryInput = findViewById(R.id.categoryInput);
            TextInputEditText dateInput = findViewById(R.id.dateInput);
            TextInputEditText notesInput = findViewById(R.id.notesInput);

            expenseNameInput.setText(expense.getName());
            amountInput.setText(String.valueOf(expense.getAmount()));
            categoryInput.setText(expense.getCategory().getDisplayName(), false);
            dateInput.setText(expense.getDate());
            notesInput.setText(expense.getNotes());

            // Store expense ID in a tag so you can update it later
            expenseNameInput.setTag(expense.getId());

            // TODO: Change the Create button to Update button when editing
            // You can do this by checking if the tag is not null
        });

        // Swipe to delete
        setupSwipeToDelete(recyclerView, adapter);
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