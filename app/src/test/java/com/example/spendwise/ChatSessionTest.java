package com.example.spendwise;

import static org.junit.Assert.*;
import com.example.spendwise.model.ChatMessage;
import com.example.spendwise.model.ChatSession;
import org.junit.Test;

public class ChatSessionTest {

    @Test
    public void testChatSessionCreation() {
        String id = "session123";
        String title = "Budget Discussion";
        String summary = "Talked about monthly budget";
        long timestamp = System.currentTimeMillis();

        ChatSession session = new ChatSession(id, title, summary, timestamp);

        assertEquals("ID should match", id, session.getId());
        assertEquals("Title should match", title, session.getTitle());
        assertEquals("Summary should match", summary, session.getSummary());
        assertEquals("Timestamp should match", timestamp, session.getTimestamp());
        assertNotNull("Messages list should be initialized", session.getMessages());
        assertTrue("Messages list should be empty initially", session.getMessages().isEmpty());
    }

    @Test
    public void testAddMessage() {
        ChatSession session = new ChatSession();
        ChatMessage message1 = new ChatMessage("user", "Hello");
        ChatMessage message2 = new ChatMessage("ai", "Hi there!");

        session.addMessage(message1);
        session.addMessage(message2);

        assertEquals("Should have 2 messages", 2, session.getMessages().size());
        assertEquals("First message should match", message1, session.getMessages().get(0));
        assertEquals("Second message should match", message2, session.getMessages().get(1));
    }

    @Test
    public void testAddMessageToNullList() {
        ChatSession session = new ChatSession();
        session.setMessages(null);

        ChatMessage message = new ChatMessage("user", "Test message");
        session.addMessage(message);

        assertNotNull("Messages list should be re-initialized", session.getMessages());
        assertEquals("Should have 1 message", 1, session.getMessages().size());
    }
}

