package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.BudgetBinding;

public class Budget extends AppCompatActivity {

    private String category;
    private String frequency;
    private String startDate;

    public String getCategory() {
        return category;
    }

    public String getStartDate() {
        return startDate;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BudgetBinding binding = BudgetBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.setLifecycleOwner(this);

        View dashboardNavigate = findViewById(R.id.dashboard_navigate);
        View expenseLogNavigate = findViewById(R.id.expenseLog_navigate);
        View budgetNavigate = findViewById(R.id.budget_navigate);
        View savingCircleNavigate = findViewById(R.id.savingCircle_navigate);
        View chatbotNavigate = findViewById(R.id.chatbot_navigate);

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

        View addBudgetButton = findViewById(R.id.add_budget_button);
        addBudgetButton.setOnClickListener(v -> {
            // TODO: This will navigate to budget creation form in Sprint 2

            android.widget.Toast.makeText(
                    this,
                    "Budget creation form will be implemented in a later sprint",
                    android.widget.Toast.LENGTH_LONG
            ).show();
        });
    }
}

