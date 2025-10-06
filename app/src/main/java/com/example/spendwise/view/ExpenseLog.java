package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.ExpenselogBinding;

public class ExpenseLog extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Using data binding to inflate the layout
        ExpenselogBinding binding = ExpenselogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Optional ViewModel setup
        // binding.setVariable(BR.viewModel, viewModel); //Use the right view model
        binding.setLifecycleOwner(this);

        // Use findViewById for buttons
        View dashboardNavigate = findViewById(R.id.dashboard_navigate);
        View expenseLogNavigate = findViewById(R.id.expenseLog_navigate);
        View budgetNavigate = findViewById(R.id.budget_navigate);
        View savingCircleNavigate = findViewById(R.id.savingCircle_navigate);
        View chatbotNavigate = findViewById(R.id.chatbot_navigate);

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

        // Add Expense button - placeholder for future expense entry form
        View addExpenseButton = findViewById(R.id.add_expense_button);
        addExpenseButton.setOnClickListener(v -> {
            // TODO: This will navigate to expense entry form in Sprint 2
            // For now, show placeholder message
            android.widget.Toast.makeText(
                    this,
                    "Expense entry form will be implemented in a later sprint",
                    android.widget.Toast.LENGTH_LONG
            ).show();
        });
    }
}

