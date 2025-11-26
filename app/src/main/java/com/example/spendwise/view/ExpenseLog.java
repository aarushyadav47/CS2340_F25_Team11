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
import com.example.spendwise.model.SavingCircle;

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
import com.example.spendwise.viewModel.SavingCircleViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExpenseLog extends AppCompatActivity {

    private ExpenseViewModel expenseViewModel;
    private SavingCircleViewModel savingCircleViewModel;
    private ExpenselogBinding binding;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    private List<String> categoryOptions = new ArrayList<>();
    private Map<String, String> savingCircleMap = new HashMap<>(); // Maps display name to circle ID

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        binding = ExpenselogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ViewModel setup
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
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

        // Setup category dropdown with Savings Circles
        setupCategoryDropdown();

        // Create Expense button
        View createExpenseBtn = findViewById(R.id.create_Expense);
        createExpenseBtn.setOnClickListener(v -> saveExpense());
    }

    private void setupCategoryDropdown() {
        AutoCompleteTextView dropdown = findViewById(R.id.categoryInput);
        
        // Start with regular categories
        categoryOptions.clear();
        categoryOptions.add("Food");
        categoryOptions.add("Transport");
        categoryOptions.add("Entertainment");
        categoryOptions.add("Bills");
        categoryOptions.add("Health");
        categoryOptions.add("Shopping");
        categoryOptions.add("Other");

        // Set initial adapter with just regular categories
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, categoryOptions);
        dropdown.setAdapter(adapter);

        // Load savings circles and add them to the dropdown
        savingCircleViewModel.getSavingCircles().observe(this, circles -> {
            // Always start fresh with base categories
            categoryOptions.clear();
            categoryOptions.add("Food");
            categoryOptions.add("Transport");
            categoryOptions.add("Entertainment");
            categoryOptions.add("Bills");
            categoryOptions.add("Health");
            categoryOptions.add("Shopping");
            categoryOptions.add("Other");
            savingCircleMap.clear();

            // Add savings circles if available
            if (circles != null && !circles.isEmpty()) {
                for (SavingCircle circle : circles) {
                    String displayName = "💰 " + circle.getChallengeTitle() + " (" + circle.getGroupName() + ")";
                    categoryOptions.add(displayName);
                    savingCircleMap.put(displayName, circle.getId());
                }
                Log.d("ExpenseLog", "Added " + circles.size() + " savings circles to dropdown");
            }

            // Update the adapter with the complete list
            ArrayAdapter<String> updatedAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, categoryOptions);
            dropdown.setAdapter(updatedAdapter);
            Log.d("ExpenseLog", "Category dropdown updated with " + categoryOptions.size() + " options");
        });
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

        // Check if it's a savings circle (starts with 💰)
        String savingCircleId = null;
        Category category = null;
        
        if (categoryName.startsWith("💰 ")) {
            // It's a savings circle
            savingCircleId = savingCircleMap.get(categoryName);
            if (savingCircleId == null) {
                categoryInput.setError("Invalid savings circle selected");
                categoryInput.requestFocus();
                return;
            }
            // Use "OTHER" category for savings circle expenses
            category = Category.OTHER;
        } else {
            // Regular category
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
        }

        // Parse expense date to timestamp
        long expenseTimestamp;
        try {
            Date expenseDate = dateFormat.parse(date);
            expenseTimestamp = expenseDate != null ? expenseDate.getTime() : System.currentTimeMillis();
        } catch (ParseException e) {
            expenseTimestamp = System.currentTimeMillis();
        }

        // Save to Firebase through ViewModel
        expenseViewModel.addExpense(name, amount, category, date, notes, savingCircleId, 
                savingCircleId != null ? expenseTimestamp : -1);

        // If linked to a savings circle, deduct from the circle
        if (savingCircleId != null && !savingCircleId.isEmpty()) {
            FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                savingCircleViewModel.deductExpenseFromMemberAtDate(
                        savingCircleId, 
                        currentUser.getEmail(), 
                        amount, 
                        expenseTimestamp
                );
            }
        }

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

        findViewById(R.id.chatbot_navigate).setOnClickListener(v -> {
            Intent chatbotIntent = new Intent(this, Chatbot.class);
            chatbotIntent.putExtra("selected_date", dashboardDate);
            startActivity(chatbotIntent);
        });
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
                Expense expense = adapter.getExpenseAt(viewHolder.getAdapterPosition());
                String expenseId = expense.getId();
                
                // Check if expense is linked to a savings circle
                if (expense.isLinkedToSavingCircle()) {
                    String savingCircleId = expense.getSavingCircleId();
                    double amount = expense.getAmount();
                    String date = expense.getDate();
                    
                    // Parse expense date to timestamp
                    long expenseTimestamp;
                    try {
                        Date expenseDate = dateFormat.parse(date);
                        expenseTimestamp = expenseDate != null ? expenseDate.getTime() : System.currentTimeMillis();
                    } catch (ParseException e) {
                        expenseTimestamp = System.currentTimeMillis();
                    }
                    
                    // Get current user email
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null && currentUser.getEmail() != null) {
                        // Restore the amount to the savings circle
                        savingCircleViewModel.addBackExpenseToMemberAtDate(
                                savingCircleId,
                                currentUser.getEmail(),
                                amount,
                                expenseTimestamp
                        );
                    }
                }
                
                // Delete the expense
                expenseViewModel.deleteExpense(expenseId);
            }
        }).attachToRecyclerView(recyclerView);
    }
}