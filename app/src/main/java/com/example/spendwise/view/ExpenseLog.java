package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.ExpenselogBinding;

import com.example.spendwise.viewModel.ExpenseViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.AutoCompleteTextView;

public class ExpenseLog extends AppCompatActivity {

    private ExpenseViewModel expenseViewModel;
    private ExpenselogBinding binding;

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
        View expenseLogForm = findViewById(R.id.form_Container);
        View expenseLogMsg = findViewById(R.id.expenseLog_msg);

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
            expenseLogForm.setVisibility(View.VISIBLE);
            expenseLogMsg.setVisibility(View.GONE);
        });

        View createExpenseBtn = findViewById(R.id.create_Expense);
        createExpenseBtn.setOnClickListener(v->{
            // Get all input fields
            TextInputEditText expenseNameInput = findViewById(R.id.expenseNameInput);
            TextInputEditText amountInput = findViewById(R.id.amountInput);
            AutoCompleteTextView categoryInput = findViewById(R.id.categoryInput);
            TextInputEditText dateInput = findViewById(R.id.dateInput);
            TextInputEditText notesInput = findViewById(R.id.notesInput);

            // Get the text from each field
            String name = expenseNameInput.getText().toString();
            String amount = amountInput.getText().toString();
            String category = categoryInput.getText().toString();
            String date = dateInput.getText().toString();
            String notes = notesInput.getText().toString();
            //Firebase addition

            //Show the different expenses in the expense Log

        });
    }
}

