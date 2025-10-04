package com.example.sprint1.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.sprint1.databinding.DashboardBinding;

public class Dashboard extends AppCompatActivity {

    private DashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.setLifecycleOwner(this);

        // Setup bottom nav buttons
        setupNavClick((ViewGroup) binding.dashboardNavigate, Dashboard.class);
        setupNavClick((ViewGroup) binding.expenseLogNavigate, ExpenseLog.class);
        setupNavClick((ViewGroup) binding.budgetNavigate, Budget.class);
        setupNavClick((ViewGroup) binding.savingCircleNavigate, SavingCircle.class);
        setupNavClick((ViewGroup) binding.chatbotNavigate, Chatbot.class);

        // Highlight current page
        highlightActiveButton((ViewGroup) binding.dashboardNavigate);
    }

    private void setupNavClick(ViewGroup button, Class<?> targetActivity) {
        button.setOnClickListener(v -> {
            if (!targetActivity.equals(Dashboard.class)) {
                startActivity(new Intent(Dashboard.this, targetActivity));
            }
        });
    }

    private void highlightActiveButton(ViewGroup activeButton) {
        resetNavIcons();

        ImageView icon = (ImageView) activeButton.getChildAt(0);
        View indicator = (View) activeButton.getChildAt(1);

        icon.setColorFilter(Color.BLACK);
        indicator.setVisibility(View.VISIBLE);
        indicator.setBackgroundColor(Color.BLACK);
    }

    private void resetNavIcons() {
        ViewGroup[] buttons = {
                (ViewGroup) binding.dashboardNavigate,
                (ViewGroup) binding.expenseLogNavigate,
                (ViewGroup) binding.budgetNavigate,
                (ViewGroup) binding.savingCircleNavigate,
                (ViewGroup) binding.chatbotNavigate
        };

        for (ViewGroup button : buttons) {
            ImageView icon = (ImageView) button.getChildAt(0);
            View indicator = (View) button.getChildAt(1);

            icon.setColorFilter(Color.parseColor("#888888"));
            indicator.setVisibility(View.INVISIBLE);
        }
    }
}
