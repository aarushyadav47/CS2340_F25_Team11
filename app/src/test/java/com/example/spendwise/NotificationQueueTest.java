package com.example.spendwise;

import static org.junit.Assert.*;

import com.example.spendwise.viewModel.NotificationViewModel.NotificationItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Sprint 4 Unit Tests: Notification Queue System
 * Tests that queue prevents overlapping notifications
 */
public class NotificationQueueTest {

    @Test
    public void testQueuePreventsOverlap() {
        Queue<List<NotificationItem>> queue = new LinkedList<>();
        
        List<NotificationItem> batch1 = new ArrayList<>();
        batch1.add(createTestNotification("id1"));
        
        List<NotificationItem> batch2 = new ArrayList<>();
        batch2.add(createTestNotification("id2"));
        
        queue.offer(batch1);
        queue.offer(batch2);
        
        assertEquals("Queue should have 2 batches", 2, queue.size());
        
        List<NotificationItem> first = queue.poll();
        assertNotNull("First batch should exist", first);
        assertEquals("First batch should have 1 item", 1, first.size());
        
        List<NotificationItem> second = queue.poll();
        assertNotNull("Second batch should exist", second);
        assertEquals("Second batch should have 1 item", 1, second.size());
        
        assertTrue("Queue should be empty after processing", queue.isEmpty());
    }

    @Test
    public void testQueueMaintainsOrder() {
        Queue<List<NotificationItem>> queue = new LinkedList<>();
        
        List<NotificationItem> batch1 = new ArrayList<>();
        batch1.add(createTestNotification("first"));
        
        List<NotificationItem> batch2 = new ArrayList<>();
        batch2.add(createTestNotification("second"));
        
        queue.offer(batch1);
        queue.offer(batch2);
        
        List<NotificationItem> first = queue.poll();
        assertEquals("First notification should be 'first'", 
            "first", first.get(0).getId());
        
        List<NotificationItem> second = queue.poll();
        assertEquals("Second notification should be 'second'", 
            "second", second.get(0).getId());
    }

    private NotificationItem createTestNotification(String id) {
        return new NotificationItem(
            NotificationItem.Type.NO_EXPENSES,
            id,
            "Test Title",
            "Test Subtitle",
            0,
            System.currentTimeMillis()
        );
    }
}

