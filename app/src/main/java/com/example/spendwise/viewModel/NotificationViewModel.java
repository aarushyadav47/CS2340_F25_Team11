package com.example.spendwise.viewModel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Budget;
import com.example.spendwise.model.MemberCycle;
import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.model.SavingCircleMember;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationViewModel extends ViewModel {

    private static final String TAG = "NotificationViewModel";
    private static final int NO_EXPENSE_DAYS = 3; // Alert if no expenses for 3 days
    private static final double BUDGET_WARNING_THRESHOLD = 0.90; // 90% of budget

    private final MutableLiveData<List<NotificationItem>> pendingNotifications;
    private final MutableLiveData<Boolean> hasNotifications;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private final SimpleDateFormat dateFormat;

    // Track shown notifications for this session
    private final Set<String> shownNotificationIds;
    private final Set<String> dismissedNotificationIds;

    public NotificationViewModel() {
        pendingNotifications = new MutableLiveData<>(new ArrayList<>());
        hasNotifications = new MutableLiveData<>(false);
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        shownNotificationIds = new HashSet<>();
        dismissedNotificationIds = new HashSet<>();
    }

    /**
     * Check for notifications based on dashboard date
     * This is called when user opens the app or changes dashboard date
     */
    public void checkNotificationsForDate(long dashboardTimestamp) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in");
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userRef = database.getReference("users").child(uid);

        // First, load dismissed notifications from Firebase
        loadDismissedNotifications(userRef, () -> {
            // Use a single list to collect all notifications
            final List<NotificationItem> allNotifications = new ArrayList<>();
            final boolean[] checksComplete = {false, false}; // expenses, budget90

            // Check for no expenses in last 3 days
            checkNoExpensesAlert(userRef, dashboardTimestamp, allNotifications, () -> {
                checksComplete[0] = true;
                if (allChecksComplete(checksComplete)) {
                    updateNotifications(allNotifications);
                }
            });

            // Check for budgets at 90%
            checkBudget90PercentAlert(userRef, dashboardTimestamp, allNotifications, () -> {
                checksComplete[1] = true;
                if (allChecksComplete(checksComplete)) {
                    updateNotifications(allNotifications);
                }
            });
        });
    }

    private boolean allChecksComplete(boolean[] checks) {
        for (boolean check : checks) {
            if (!check) return false;
        }
        return true;
    }

    /**
     * Dismiss a specific notification and save to Firebase
     */
    public void dismissNotification(NotificationItem item) {
        dismissedNotificationIds.add(item.getId());

        // Save to Firebase
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            database.getReference("users").child(uid)
                    .child("dismissedNotifications")
                    .child(item.getId())
                    .setValue(System.currentTimeMillis())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Notification dismissed and saved: " + item.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save dismissed notification", e);
                    });
        }

        List<NotificationItem> current = pendingNotifications.getValue();
        if (current != null) {
            List<NotificationItem> updated = new ArrayList<>();
            for (NotificationItem notification : current) {
                if (!notification.getId().equals(item.getId())) {
                    updated.add(notification);
                }
            }
            pendingNotifications.setValue(updated);
            hasNotifications.setValue(!updated.isEmpty());
        }
    }

    /**
     * Clear all notifications for the current session
     */
    public void clearAllNotifications() {
        FirebaseUser currentUser = auth.getCurrentUser();
        List<NotificationItem> current = pendingNotifications.getValue();

        if (current != null && currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference dismissedRef = database.getReference("users")
                    .child(uid)
                    .child("dismissedNotifications");

            for (NotificationItem item : current) {
                dismissedNotificationIds.add(item.getId());
                dismissedRef.child(item.getId()).setValue(System.currentTimeMillis());
            }
        }

        pendingNotifications.setValue(new ArrayList<>());
        hasNotifications.setValue(false);
    }

    /**
     * Load dismissed notifications from Firebase
     */
    private void loadDismissedNotifications(DatabaseReference userRef, Runnable onComplete) {
        userRef.child("dismissedNotifications").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dismissedNotificationIds.clear();

                for (DataSnapshot dismissedSnapshot : snapshot.getChildren()) {
                    String notificationId = dismissedSnapshot.getKey();
                    if (notificationId != null) {
                        dismissedNotificationIds.add(notificationId);
                        Log.d(TAG, "Loaded dismissed notification: " + notificationId);
                    }
                }

                Log.d(TAG, "Loaded " + dismissedNotificationIds.size() + " dismissed notifications");
                onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading dismissed notifications", error.toException());
                onComplete.run();
            }
        });
    }

    /**
     * Reset the session - useful when user logs out or app restarts
     */
    public void resetSession() {
        shownNotificationIds.clear();
        dismissedNotificationIds.clear();
        pendingNotifications.setValue(new ArrayList<>());
        hasNotifications.setValue(false);
    }

    /**
     * Check if user hasn't logged any expenses in the last 3 days
     */
    private void checkNoExpensesAlert(DatabaseReference userRef, long dashboardTimestamp,
                                      List<NotificationItem> notifications, Runnable onComplete) {
        Log.d(TAG, "=== Checking No Expenses Alert ===");

        userRef.child("expenses").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long threeDaysAgo = dashboardTimestamp - (NO_EXPENSE_DAYS * 24 * 60 * 60 * 1000);
                boolean hasRecentExpense = false;
                long mostRecentExpenseDate = 0;

                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    String dateStr = expenseSnapshot.child("date").getValue(String.class);
                    if (dateStr != null) {
                        try {
                            Date expenseDate = dateFormat.parse(dateStr);
                            if (expenseDate != null) {
                                long expenseTime = expenseDate.getTime();

                                // Track most recent expense
                                if (expenseTime > mostRecentExpenseDate) {
                                    mostRecentExpenseDate = expenseTime;
                                }

                                // Check if expense is within last 3 days
                                if (expenseTime >= threeDaysAgo && expenseTime <= dashboardTimestamp) {
                                    hasRecentExpense = true;
                                    break;
                                }
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing expense date", e);
                        }
                    }
                }

                if (!hasRecentExpense && snapshot.getChildrenCount() > 0) {
                    long daysSinceLastExpense = (dashboardTimestamp - mostRecentExpenseDate) / (1000 * 60 * 60 * 24);

                    NotificationItem item = new NotificationItem(
                            NotificationItem.Type.NO_EXPENSES,
                            "no_expenses_" + dashboardTimestamp,
                            "No Recent Expenses",
                            "Last expense was " + daysSinceLastExpense + " days ago",
                            0,
                            dashboardTimestamp
                    );
                    synchronized (notifications) {
                        notifications.add(item);
                    }
                    Log.d(TAG, "✓✓✓ NOTIFICATION ADDED: No expenses in " + daysSinceLastExpense + " days");
                } else {
                    Log.d(TAG, "✗ Recent expenses found or no expenses in database");
                }

                onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking expenses", error.toException());
                onComplete.run();
            }
        });
    }

    /**
     * Check if any budget has reached 90% of its limit
     */
    private void checkBudget90PercentAlert(DatabaseReference userRef, long dashboardTimestamp,
                                           List<NotificationItem> notifications, Runnable onComplete) {
        Log.d(TAG, "=== Checking Budget 90% Alert ===");

        Calendar dashboardCal = Calendar.getInstance();
        dashboardCal.setTimeInMillis(dashboardTimestamp);

        userRef.child("budgets").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long budgetCount = snapshot.getChildrenCount();

                if (budgetCount == 0) {
                    Log.d(TAG, "No budgets found");
                    onComplete.run();
                    return;
                }

                // Counter to track completed budget checks
                final int[] completedChecks = {0};
                final int[] activeBudgets = {0};

                for (DataSnapshot budgetSnapshot : snapshot.getChildren()) {
                    try {
                        String name = budgetSnapshot.child("name").getValue(String.class);
                        String dateStr = budgetSnapshot.child("date").getValue(String.class);
                        Double amount = budgetSnapshot.child("amount").getValue(Double.class);

                        String frequency = budgetSnapshot.child("frequency").getValue(String.class);
                        if (frequency == null) {
                            frequency = budgetSnapshot.child("freq").getValue(String.class);
                        }

                        if (name == null || dateStr == null || frequency == null || amount == null) {
                            continue;
                        }

                        Date startDate = dateFormat.parse(dateStr);
                        if (startDate == null) continue;

                        // Find current cycle
                        Calendar cycleStart = Calendar.getInstance();
                        cycleStart.setTime(startDate);
                        Calendar cycleEnd = (Calendar) cycleStart.clone();

                        while (cycleEnd.getTimeInMillis() <= dashboardTimestamp) {
                            cycleStart = (Calendar) cycleEnd.clone();
                            cycleEnd = (Calendar) cycleStart.clone();

                            if ("Weekly".equalsIgnoreCase(frequency)) {
                                cycleEnd.add(Calendar.DAY_OF_YEAR, 7);
                            } else if ("Monthly".equalsIgnoreCase(frequency)) {
                                cycleEnd.add(Calendar.MONTH, 1);
                            }
                        }

                        // Check if in active cycle
                        if (dashboardTimestamp >= cycleStart.getTimeInMillis() &&
                                dashboardTimestamp <= cycleEnd.getTimeInMillis()) {

                            activeBudgets[0]++;
                            // Calculate total expenses for this budget in current cycle
                            String category = budgetSnapshot.child("category").getValue(String.class);
                            checkExpensesForBudget(userRef, category, cycleStart.getTimeInMillis(),
                                    cycleEnd.getTimeInMillis(), amount, name, notifications, () -> {
                                        completedChecks[0]++;
                                        Log.d(TAG, "Completed budget check " + completedChecks[0] + " of " + activeBudgets[0]);
                                        if (completedChecks[0] >= activeBudgets[0]) {
                                            onComplete.run();
                                        }
                                    });
                        }

                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing budget date", e);
                    }
                }

                // If no active budgets were found, complete immediately
                if (activeBudgets[0] == 0) {
                    Log.d(TAG, "No active budgets in current cycle");
                    onComplete.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking budgets for 90%", error.toException());
                onComplete.run();
            }
        });
    }

    private void checkExpensesForBudget(DatabaseReference userRef, String category,
                                        long cycleStart, long cycleEnd, double budgetAmount,
                                        String budgetName, List<NotificationItem> notifications,
                                        Runnable onComplete) {
        userRef.child("expenses").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalSpent = 0.0;

                for (DataSnapshot expenseSnapshot : snapshot.getChildren()) {
                    String expenseCategory = expenseSnapshot.child("category").getValue(String.class);
                    String dateStr = expenseSnapshot.child("date").getValue(String.class);
                    Double expenseAmount = expenseSnapshot.child("amount").getValue(Double.class);

                    if (category != null && category.equals(expenseCategory) && dateStr != null && expenseAmount != null) {
                        try {
                            Date expenseDate = dateFormat.parse(dateStr);
                            if (expenseDate != null) {
                                long expenseTime = expenseDate.getTime();
                                if (expenseTime >= cycleStart && expenseTime <= cycleEnd) {
                                    totalSpent += expenseAmount;
                                }
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing expense date", e);
                        }
                    }
                }

                double percentageUsed = (totalSpent / budgetAmount) * 100;

                Log.d(TAG, "Budget: " + budgetName + ", Spent: $" + totalSpent + " / $" + budgetAmount + " = " + percentageUsed + "%");

                if (percentageUsed >= (BUDGET_WARNING_THRESHOLD * 100)) {
                    NotificationItem item = new NotificationItem(
                            NotificationItem.Type.BUDGET_90_PERCENT,
                            "budget_90_" + budgetName + "_" + cycleStart,
                            budgetName + " at " + String.format("%.0f", percentageUsed) + "%",
                            String.format("Spent $%.2f of $%.2f", totalSpent, budgetAmount),
                            0,
                            cycleEnd
                    );
                    synchronized (notifications) {
                        notifications.add(item);
                    }
                    Log.d(TAG, "✓✓✓ NOTIFICATION ADDED: " + budgetName + " at " + percentageUsed + "%");
                } else {
                    Log.d(TAG, "✗ Budget under 90% threshold");
                }

                onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking expenses for budget", error.toException());
                onComplete.run();
            }
        });
    }

    private void updateNotifications(List<NotificationItem> notifications) {
        // Filter out already shown or dismissed notifications
        List<NotificationItem> newNotifications = new ArrayList<>();
        for (NotificationItem item : notifications) {
            if (!shownNotificationIds.contains(item.getId()) &&
                    !dismissedNotificationIds.contains(item.getId())) {
                newNotifications.add(item);
                shownNotificationIds.add(item.getId());
            }
        }

        pendingNotifications.setValue(newNotifications);
        hasNotifications.setValue(!newNotifications.isEmpty());
        Log.d(TAG, "Updated notifications: " + newNotifications.size() + " new items (filtered from " + notifications.size() + " total)");
    }

    private String sanitizeEmail(String email) {
        if (email == null) return "";
        return email.replace(".", "_").replace("@", "_at_");
    }

    public LiveData<List<NotificationItem>> getPendingNotifications() {
        return pendingNotifications;
    }

    public LiveData<Boolean> getHasNotifications() {
        return hasNotifications;
    }

    /**
     * Data class to represent a notification item
     */
    public static class NotificationItem {
        public enum Type {
            NO_EXPENSES,
            BUDGET_90_PERCENT
        }

        private String id;
        private Type type;
        private String title;
        private String subtitle;
        private int daysRemaining;
        private long endDate;

        public NotificationItem(Type type, String id, String title, String subtitle,
                                int daysRemaining, long endDate) {
            this.type = type;
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.daysRemaining = daysRemaining;
            this.endDate = endDate;
        }

        public String getId() {
            return id;
        }

        public Type getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public int getDaysRemaining() {
            return daysRemaining;
        }

        public long getEndDate() {
            return endDate;
        }

        public String getTimeMessage() {
            if (type == Type.NO_EXPENSES) {
                return "Track your spending!";
            } else if (type == Type.BUDGET_90_PERCENT) {
                return "Budget limit approaching";
            }
            return "";
        }
    }
}