package com.example.spendwise.view;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.model.Budget;
import com.example.spendwise.repo.BudgetRepo;

import java.util.Calendar;

public class FormBudget extends AppCompatActivity {

    private EditText nameInput, amountInput, startDateInput;
    private Spinner categorySpinner, frequencySpinner;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget_form);

        nameInput = findViewById(R.id.budget_name);
        amountInput = findViewById(R.id.budget_limit);
        startDateInput = findViewById(R.id.budget_start_date);
        categorySpinner = findViewById(R.id.budget_category);
        frequencySpinner = findViewById(R.id.budget_frequency);
        submitButton = findViewById(R.id.save_budget_button);

        String[] categories = {"Food", "Transport", "Entertainment", "Bills", "Shopping", "Health", "Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        String[] frequencies = {"Weekly", "Monthly"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, frequencies);
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(freqAdapter);

        startDateInput.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(FormBudget.this,
                    (view, year1, month1, dayOfMonth) -> startDateInput.setText(String.format("%04d-%02d-%02d", year1, month1+1, dayOfMonth)),
                    year, month, day);
            datePickerDialog.show();
        });

        submitButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            String category = categorySpinner.getSelectedItem().toString();
            String frequency = frequencySpinner.getSelectedItem().toString();
            String startDate = startDateInput.getText().toString().trim();

            if (name.isEmpty() || amountStr.isEmpty() || startDate.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount < 0) {
                    Toast.makeText(this, "Amount must be non-negative", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            Budget newBudget = new Budget(name, amount, category, frequency, startDate);
            BudgetRepo repo = new BudgetRepo();

            repo.addBudget(newBudget, new BudgetRepo.OnBudgetAddedListener() {
                @Override
                public void onSuccess(String message) {
                    Toast.makeText(FormBudget.this, "Budget created!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(FormBudget.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

    }
}
