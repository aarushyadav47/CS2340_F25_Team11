package com.example.spendwise;

import static org.junit.Assert.*;

import com.example.spendwise.command.ChatCommand;
import com.example.spendwise.command.WeeklySpendingCommand;
import com.example.spendwise.command.CutCostsCommand;
import com.example.spendwise.command.MonthlyComparisonCommand;

import org.junit.Test;

/**
 * Sprint 4 Unit Tests: Command Pattern Implementation
 * Tests that commands correctly match and can be executed
 */
public class ChatbotCommandTest {

    @Test
    public void testWeeklySpendingCommandMatches() {
        ChatCommand command = new WeeklySpendingCommand();
        assertTrue("Should match 'spending this week'", 
            command.matches("How much am I spending this week?"));
        assertTrue("Should match 'my spending'", 
            command.matches("Show my spending"));
        assertFalse("Should not match unrelated message", 
            command.matches("What's the weather?"));
    }

    @Test
    public void testCutCostsCommandMatches() {
        ChatCommand command = new CutCostsCommand();
        assertTrue("Should match 'cut costs'", 
            command.matches("How can I cut costs?"));
        assertTrue("Should match 'reduce spending'", 
            command.matches("Help me reduce spending"));
        assertFalse("Should not match unrelated message", 
            command.matches("What's for dinner?"));
    }

    @Test
    public void testMonthlyComparisonCommandMatches() {
        ChatCommand command = new MonthlyComparisonCommand();
        assertTrue("Should match 'compared to last month'", 
            command.matches("How did I do compared to last month?"));
        assertTrue("Should match 'vs last month'", 
            command.matches("Spending vs last month"));
        assertFalse("Should not match unrelated message", 
            command.matches("Tell me a joke"));
    }

    @Test
    public void testCommandPatternExtensibility() {
        // Verify commands can be extended easily
        ChatCommand command1 = new WeeklySpendingCommand();
        ChatCommand command2 = new CutCostsCommand();
        ChatCommand command3 = new MonthlyComparisonCommand();
        
        assertNotEquals("Commands should be different instances", 
            command1.getClass(), command2.getClass());
        assertNotEquals("Commands should be different instances", 
            command2.getClass(), command3.getClass());
    }
}

