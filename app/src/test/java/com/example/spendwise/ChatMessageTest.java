package com.example.spendwise;

import static org.junit.Assert.*;
import com.example.spendwise.model.ChatMessage;
import org.junit.Test;

public class ChatMessageTest {

    @Test
    public void testChatMessageCreation() {
        String role = "user";
        String content = "What is my budget for food?";

        ChatMessage message = new ChatMessage(role, content);

        assertEquals("Role should match", role, message.getRole());
        assertEquals("Content should match", content, message.getContent());
        assertTrue("Timestamp should be set", message.getTimestamp() > 0);
    }

    @Test
    public void testAiMessageCreation() {
        String role = "ai";
        String content = "Your food budget is $200 for this month.";

        ChatMessage aiMessage = new ChatMessage(role, content);

        assertEquals("AI role should match", "ai", aiMessage.getRole());
        assertEquals("AI content should match", content, aiMessage.getContent());
        assertNotNull("Content should not be null", aiMessage.getContent());
    }

    @Test
    public void testSettersAndGetters() {
        ChatMessage message = new ChatMessage();
        
        String testRole = "user";
        String testContent = "Test message";
        long testTimestamp = System.currentTimeMillis();

        message.setRole(testRole);
        message.setContent(testContent);
        message.setTimestamp(testTimestamp);

        assertEquals("Role should be set correctly", testRole, message.getRole());
        assertEquals("Content should be set correctly", testContent, message.getContent());
        assertEquals("Timestamp should be set correctly", testTimestamp, message.getTimestamp());
    }
}

