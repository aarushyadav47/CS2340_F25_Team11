package com.example.spendwise.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.databinding.RegisterBinding;
import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.example.spendwise.model.Budget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Register extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private RegisterBinding binding;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy",
            Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = RegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        binding.setLifecycleOwner(this);

        // Force status bar color (the purple bar at top)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF6200EE); // Purple
            // Or use: getWindow().setStatusBarColor(Color.parseColor("#6200EE"));
        }

        // Register user
        binding.registerButton.setOnClickListener(v -> {
            String email = binding.emailField.getText().toString().trim();
            String password = binding.passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Register.this,
                        "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(Register.this, "Registration successful!",
                                    Toast.LENGTH_SHORT).show();

                            // Wait for Firebase authentication to complete
                            // then create dummy data
                            android.util.Log.d("Register",
                                    "Waiting for authentication...");
                            new android.os.Handler().postDelayed(() -> {
                                FirebaseUser currentUser = mAuth.getCurrentUser();
                                if (currentUser != null) {
                                    android.util.Log.d("Register",
                                            "User authenticated: "
                                                    + currentUser.getUid());
                                    android.util.Log.d("Register",
                                            "Creating dummy data...");
                                    createDummyData(currentUser.getUid());
                                } else {
                                    android.util.Log.e("Register",
                                            "User still not authenticated!");
                                }

                                // Navigate to login after attempting to create data
                                new android.os.Handler().postDelayed(() -> {
                                    startActivity(new Intent(Register.this,
                                            Login.class));
                                    finish();
                                }, 1000); // Wait another second before navigating
                            }, 2000); // Wait 2 seconds for Firebase to authenticate
                        } else {
                            Toast.makeText(Register.this, "Registration failed: "
                                            + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Open Login screen
        binding.openLogin.setOnClickListener(v -> startActivity(new Intent(Register.this,
                Login.class)));
    }

    // Method to create dummy data directly via Firebase (bypassing ViewModels)
    private void createDummyData(String uid) {
        android.util.Log.d("Register", "createDummyData() called for uid: " + uid);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef = database.getReference("users").child(uid);

        Calendar calendar = Calendar.getInstance();
        String today = dateFormat.format(calendar.getTime());

        // Get dates for the past week
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String yesterday = dateFormat.format(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, -2);
        String threeDaysAgo = dateFormat.format(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, -2);
        String fiveDaysAgo = dateFormat.format(calendar.getTime());

        // Counter to track completion
        AtomicInteger completed = new AtomicInteger(0);
        final int totalItems = 10; // 7 expenses + 3 budgets

        // Create 7 dummy expenses
        addExpenseDirectly(userRef,
                new Expense("Grocery Shopping", 85.50, Category.FOOD,
                        today, "Weekly groceries"),
                completed, totalItems);
        addExpenseDirectly(userRef,
                new Expense("Restaurant Dinner", 45.00, Category.FOOD,
                        yesterday, "Dinner with friends"),
                completed, totalItems);
        addExpenseDirectly(userRef,
                new Expense("Coffee Shop", 12.50, Category.FOOD,
                        threeDaysAgo, "Morning coffee"),
                completed, totalItems);
        addExpenseDirectly(userRef,
                new Expense("Gas Station", 50.00, Category.TRANSPORT,
                        yesterday, "Fuel for car"),
                completed, totalItems);
        addExpenseDirectly(userRef,
                new Expense("Uber Ride", 18.50, Category.TRANSPORT,
                        fiveDaysAgo, "Ride to office"),
                completed, totalItems);
        addExpenseDirectly(userRef,
                new Expense("Movie Tickets", 30.00, Category.ENTERTAINMENT,
                        threeDaysAgo, "Weekend movie"),
                completed, totalItems);
        addExpenseDirectly(userRef,
                new Expense("Concert", 75.00, Category.ENTERTAINMENT,
                        fiveDaysAgo, "Live music event"),
                completed, totalItems);

        // Create 3 dummy budgets
        addBudgetDirectly(userRef,
                new Budget("Food Budget", 500.00, Category.FOOD,
                        today, "Monthly"),
                completed, totalItems);
        addBudgetDirectly(userRef,
                new Budget("Transport Budget", 200.00, Category.TRANSPORT,
                        today, "Monthly"),
                completed, totalItems);
        addBudgetDirectly(userRef,
                new Budget("Entertainment Budget", 150.00,
                        Category.ENTERTAINMENT, today, "Monthly"),
                completed, totalItems);
    }

    private void addExpenseDirectly(DatabaseReference userRef, Expense expense,
                                    AtomicInteger completed, int total) {
        DatabaseReference expenseRef = userRef.child("expenses").push();
        String id = expenseRef.getKey();

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("id", id);
        expenseData.put("name", expense.getName());
        expenseData.put("amount", expense.getAmount());
        expenseData.put("category", expense.getCategory().name());
        expenseData.put("date", expense.getDate());
        expenseData.put("notes", expense.getNotes());

        expenseRef.setValue(expenseData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("Register", "Expense added: " + expense.getName());
                    if (completed.incrementAndGet() == total) {
                        android.util.Log.d("Register",
                                "All dummy data created successfully!");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Register", "Failed to add expense: "
                            + expense.getName(), e);
                });
    }

    private void addBudgetDirectly(DatabaseReference userRef, Budget budget,
                                   AtomicInteger completed, int total) {
        DatabaseReference budgetRef = userRef.child("budgets").push();
        String id = budgetRef.getKey();

        Map<String, Object> budgetData = new HashMap<>();
        budgetData.put("id", id);
        budgetData.put("name", budget.getName());
        budgetData.put("amount", budget.getAmount());
        budgetData.put("category", budget.getCategory().name());
        budgetData.put("date", budget.getDate());
        budgetData.put("freq", budget.getfreq());

        budgetRef.setValue(budgetData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("Register", "Budget added: " + budget.getName());
                    if (completed.incrementAndGet() == total) {
                        android.util.Log.d("Register",
                                "All dummy data created successfully!");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Register", "Failed to add budget: "
                            + budget.getName(), e);
                });
    }
}