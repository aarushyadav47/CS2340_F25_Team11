package com.example.spendwise.command;

import com.example.spendwise.viewModel.ChatbotViewModel;

/**
 * Command Pattern: Concrete command for budget queries
 */
public class BudgetQueryCommand implements ChatCommand {
    @Override
    public void execute(ChatbotViewModel viewModel, String userMessage) {
        viewModel.fetchBudgetsWithContext(userMessage);
    }

    @Override
    public boolean matches(String message) {
        String lower = message.toLowerCase();
        return lower.contains("my budget") || lower.contains("budget");
    }
}

