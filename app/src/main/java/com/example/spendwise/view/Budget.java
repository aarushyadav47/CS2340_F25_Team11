package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.BudgetBinding;
import com.example.spendwise.adapter.BudgetAdapter;
import com.example.spendwise.viewModel.BudgetViewModel;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        BudgetViewModel vm = new ViewModelProvider(this).get(BudgetViewModel.class);
        vm.refreshBudgets(true);

        RecyclerView recyclerView = findViewById(R.id.budget_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        BudgetAdapter adapter = new BudgetAdapter();
        recyclerView.setAdapter(adapter);
        vm.getBudgets().observe(this, budgets -> {
            adapter.setBudgets(budgets);
        });

        adapter.setOnItemClickListener(b -> {
            Intent i = new Intent(this, BudgetDetails.class);
            i.putExtra("name", b.getName());
            i.putExtra("amount", b.getAmount());
            i.putExtra("category", b.getCategory());
            i.putExtra("frequency", b.getFrequency());
            i.putExtra("startDate", b.getStartDate());
            startActivity(i);
        });

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
        addBudgetButton.setOnClickListener(v -> startActivity(new Intent(this, FormBudget.class)));
    }
}

