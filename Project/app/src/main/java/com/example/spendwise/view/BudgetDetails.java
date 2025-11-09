package com.example.spendwise.view;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;

public class BudgetDetails extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget_details);

        String name = getIntent().getStringExtra("name");
        double amount = getIntent().getDoubleExtra("amount", 0);
        String category = getIntent().getStringExtra("category");
        String frequency = getIntent().getStringExtra("frequency");
        String startDate = getIntent().getStringExtra("startDate");

        ((TextView) findViewById(R.id.detail_name)).setText(name);
        ((TextView) findViewById(R.id.detail_amount)).setText(String.format("$%.2f", amount));
        ((TextView) findViewById(R.id.detail_category)).setText(category);
        ((TextView) findViewById(R.id.detail_frequency)).setText(frequency);
        ((TextView) findViewById(R.id.detail_start_date)).setText(startDate);
    }
}


