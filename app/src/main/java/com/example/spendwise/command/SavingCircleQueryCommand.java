package com.example.spendwise.command;

import com.example.spendwise.viewModel.ChatbotViewModel;

/**
 * Command Pattern: Concrete command for saving circle queries
 */
public class SavingCircleQueryCommand implements ChatCommand {
    @Override
    public void execute(ChatbotViewModel viewModel, String userMessage) {
        viewModel.fetchSavingCircles();
    }

    @Override
    public boolean matches(String message) {
        String lower = message.toLowerCase();
        return lower.contains("saving");
    }
}

