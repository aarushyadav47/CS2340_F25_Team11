package com.example.spendwise.command;

import com.example.spendwise.viewModel.ChatbotViewModel;

/**
 * Command Pattern: Concrete command for monthly comparison
 */
public class MonthlyComparisonCommand implements ChatCommand {
    @Override
    public void execute(ChatbotViewModel viewModel, String userMessage) {
        viewModel.compareToLastMonth(userMessage);
    }

    @Override
    public boolean matches(String message) {
        String lower = message.toLowerCase();
        return (lower.contains("compared to last month") || 
                lower.contains("vs last month") ||
                (lower.contains("how much") && lower.contains("spend") && lower.contains("month")));
    }
}

