package com.example.spendwise.command;

import com.example.spendwise.viewModel.ChatbotViewModel;

/**
 * Command Pattern: Encapsulates chatbot commands as objects
 * This allows commands to be executed, queued, and extended easily
 * New design pattern for Sprint 4
 */
public interface ChatCommand {
    /**
     * Execute the command
     * @param viewModel The ChatbotViewModel to execute the command on
     * @param userMessage The original user message
     */
    void execute(ChatbotViewModel viewModel, String userMessage);
    
    /**
     * Check if this command matches the user message
     * @param message The user message to check
     * @return true if this command should handle the message
     */
    boolean matches(String message);
}

