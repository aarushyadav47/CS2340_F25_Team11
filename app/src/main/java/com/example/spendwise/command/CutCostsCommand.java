package com.example.spendwise.command;

import com.example.spendwise.viewModel.ChatbotViewModel;

/**
 * Command Pattern: Concrete command for cost-cutting suggestions
 */
public class CutCostsCommand implements ChatCommand {
    @Override
    public void execute(ChatbotViewModel viewModel, String userMessage) {
        viewModel.suggestCostCutting(userMessage);
    }

    @Override
    public boolean matches(String message) {
        String lower = message.toLowerCase();
        return lower.contains("cut costs") || lower.contains("reduce spending");
    }
}

