package com.example.spendwise.command;

import com.example.spendwise.viewModel.ChatbotViewModel;

/**
 * Command Pattern: Concrete command for weekly spending summary
 */
public class WeeklySpendingCommand implements ChatCommand {
    @Override
    public void execute(ChatbotViewModel viewModel, String userMessage) {
        viewModel.computeWeeklySpending(userMessage);
    }

    @Override
    public boolean matches(String message) {
        String lower = message.toLowerCase();
        return (lower.contains("spending this week") || 
                lower.contains("spent this week") ||
                (lower.contains("how much") && lower.contains("spend") && lower.contains("week")) ||
                lower.contains("my spending") || 
                lower.contains("my expenses"));
    }
}

