package com.example.spendwise.command;

import com.example.spendwise.viewModel.ChatbotViewModel;

/**
 * Command Pattern: Concrete command for expense queries
 */
public class ExpenseQueryCommand implements ChatCommand {
    @Override
    public void execute(ChatbotViewModel viewModel, String userMessage) {
        viewModel.fetchExpensesWithContext(userMessage);
    }

    @Override
    public boolean matches(String message) {
        String lower = message.toLowerCase();
        return lower.contains("expense") || lower.contains("expenses");
    }
}

