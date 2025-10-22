package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.spendwise.model.Category;

import com.example.spendwise.model.Expense;
import com.example.spendwise.model.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import android.util.Log;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ExpenseViewModel extends ViewModel {
    private static final String TAG = "ExpenseViewModel";

    private MutableLiveData<String> statusMessage;
    private MutableLiveData<List<Expense>> expenses;
    private FirebaseDatabase database;
    private DatabaseReference expensesRef; //references to the expenses collection
    //Firebase is a json so points to that node
    private FirebaseAuth auth;

    public ExpenseViewModel() {
        expenses = new MutableLiveData<>(new ArrayList<>());
        statusMessage = new MutableLiveData<>();

        // Initialize Firebase
        database = Firebase.getDatabase(); //gets it from package.json
        auth = FirebaseAuth.getInstance(); //gets user info

        // Setups user specific path for the proper structure in database tree,
        // and correct retrieval later
        setupUserExpensesReference();
        // Load expenses from Firebase when ViewModel is created (a function)
        loadExpensesFromFirebase();
    }

    // Setup reference based on current user
    private void setupUserExpensesReference() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            // Path: users/{uid}/expenses
            expensesRef = database.getReference("users").child(uid)
                    .child("expenses");
            Log.d(TAG, "Expenses reference set for user: " + uid);
        } else {
            Log.e(TAG, "No user logged in!");
            statusMessage.setValue("Please log in to manage expenses");
        }
    }

    public LiveData<List<Expense>> getExpenses() {
        return expenses;
    }

    /*public void addExpense(Expense expense) {
        expenses.getValue().add(expense);
        // Add directly to the list by unpacking the mutable live data box
        expenses.setValue(expenses.getValue());
        // Tell LiveData to update UI and stuff (opens box and alerts everyone using it)
    }*/

    // Add new expense to Firebase
    public void addExpense(String name, double amount, Category category,
                           String date, String notes) {
        if (expensesRef == null) {
            Log.e(TAG, "expensesRef is null! Cannot add expense.");
            statusMessage.setValue("Error: User not logged in");
            return;
        }

        Expense expense = new Expense(name, amount, category, date, notes);
        // Push to Firebase (auto-generates ID)
        // creates a new child location in the database tree with unique id
        DatabaseReference newExpenseRef = expensesRef.push();
        // gets the key associated with the pointer ie /expenses/mkdfjos
        String firebaseId = newExpenseRef.getKey();
        // ensures the key in database and of object is aligned
        expense.setId(firebaseId);

        Log.d(TAG, "Adding expense to Firebase: " + expense);

        newExpenseRef.setValue(expense)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Expense added successfully: " + expense);
                    statusMessage.setValue("Expense added!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding expense", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
        // setsValue in that pointer location that holds no data
        // with serialized (json formatted) data
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    // Update existing expense in Firebase
    public void updateExpense(String id, String name, double amount,
                              Category category, String date, String notes) {
        Expense expense = new Expense(name, amount, category, date, notes);
        expense.setId(id);

        expensesRef.child(id).setValue(expense)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Expense updated successfully");
                    statusMessage.setValue("Expense updated!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating expense", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
    }

    // Load expenses from Firebase
    private void loadExpensesFromFirebase() {
        expensesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Expense> expenseList = new ArrayList<>();

                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    try {
                        // Get the expense data and parse it correctly
                        String id = expenseSnapshot.getKey();
                        String name = expenseSnapshot.child("name")
                                .getValue(String.class);
                        Double amount = expenseSnapshot.child("amount")
                                .getValue(Double.class);
                        String categoryStr = expenseSnapshot.child("category")
                                .getValue(String.class);
                        String date = expenseSnapshot.child("date")
                                .getValue(String.class);
                        String notes = expenseSnapshot.child("notes")
                                .getValue(String.class);

                        // Create expense object
                        if (name != null && amount != null && categoryStr != null) {
                            Category category = Category.valueOf(categoryStr);
                            Expense expense = new Expense(name, amount, category,
                                    date, notes != null ? notes : "");
                            expense.setId(id);
                            expenseList.add(expense);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing expense", e);
                    }
                }

                expenses.setValue(expenseList);
                Log.d(TAG, "Loaded " + expenseList.size()
                        + " expenses from Firebase");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
                statusMessage.setValue("Error loading expenses: "
                        + error.getMessage());
            }
        });
    }

    // Delete expense from Firebase
    public void deleteExpense(String id) {
        if (expensesRef == null) {
            statusMessage.setValue("User not authenticated");
            return;
        }

        expensesRef.child(id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Expense deleted successfully");
                    statusMessage.setValue("Expense deleted!");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting expense", e);
                    statusMessage.setValue("Error: " + e.getMessage());
                });
    }

    /* public void addExpense(String name, String amount, String category, String date) {
        if (name.isEmpty() || amount == null ||date.isEmpty()) {
            logResult.setValue("Please enter valid data in the input fields");
            return;
        }
    }
    */
}