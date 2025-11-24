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
import java.util.List;
import java.util.Locale;

public class NotificationViewModel extends ViewModel {

    private static final String TAG = "NotificationViewModel";
    private static final int WARNING_DAYS = 30; // Show notifications 2 days before

    private final MutableLiveData<List<NotificationItem>> pendingNotifications;
    private final MutableLiveData<Boolean> hasNotifications;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private final SimpleDateFormat dateFormat;

    public NotificationViewModel() {
        pendingNotifications = new MutableLiveData<>(new ArrayList<>());
        hasNotifications = new MutableLiveData<>(false);
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
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

        // Use a single list to collect all notifications
        final List<NotificationItem> allNotifications = new ArrayList<>();
        final boolean[] budgetsChecked = {false};
        final boolean[] circlesChecked = {false};

        // Check budgets
        checkBudgetsForDate(userRef, dashboardTimestamp, allNotifications, () -> {
            budgetsChecked[0] = true;
            if (circlesChecked[0]) {
                updateNotifications(allNotifications);
            }
        });

        // Check saving circles
        checkSavingCirclesForDate(userRef, dashboardTimestamp, allNotifications, () -> {
            circlesChecked[0] = true;
            if (budgetsChecked[0]) {
                updateNotifications(allNotifications);
            }
        });
    }

    private void checkBudgetsForDate(DatabaseReference userRef, long dashboardTimestamp,
                                     List<NotificationItem> notifications, Runnable onComplete) {
        Log.d(TAG, "=== Checking Budgets ===");
        Calendar dashboardCal = Calendar.getInstance();
        dashboardCal.setTimeInMillis(dashboardTimestamp);
        Log.d(TAG, "Dashboard date: " + dateFormat.format(dashboardCal.getTime()));

        userRef.child("budgets").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Found " + snapshot.getChildrenCount() + " budgets");

                for (DataSnapshot budgetSnapshot : snapshot.getChildren()) {
                    try {
                        // Log all fields to see what's actually in the database
                        Log.d(TAG, "Budget fields:");
                        for (DataSnapshot field : budgetSnapshot.getChildren()) {
                            Log.d(TAG, "  " + field.getKey() + " = " + field.getValue());
                        }

                        String name = budgetSnapshot.child("name").getValue(String.class);
                        String dateStr = budgetSnapshot.child("date").getValue(String.class);

                        // TRY BOTH "frequency" and "freq" field names
                        String frequency = budgetSnapshot.child("frequency").getValue(String.class);
                        if (frequency == null) {
                            frequency = budgetSnapshot.child("freq").getValue(String.class);
                        }

                        String categoryName = budgetSnapshot.child("category").getValue(String.class);

                        Log.d(TAG, "Checking budget: " + name + " [" + frequency + "] started " + dateStr);

                        if (name == null || dateStr == null || frequency == null) {
                            Log.w(TAG, "  Skipping - missing fields (name=" + name + ", date=" + dateStr + ", frequency=" + frequency + ")");
                            continue;
                        }

                        Date startDate = dateFormat.parse(dateStr);
                        if (startDate == null) {
                            Log.w(TAG, "  Could not parse date: " + dateStr);
                            continue;
                        }

                        // Find which cycle the dashboard date falls into
                        Calendar cycleStart = Calendar.getInstance();
                        cycleStart.setTime(startDate);

                        Calendar cycleEnd = (Calendar) cycleStart.clone();

                        // Find the active cycle
                        boolean foundActiveCycle = false;
                        int cycleNumber = 0;

                        while (cycleEnd.getTimeInMillis() <= dashboardTimestamp) {
                            cycleStart = (Calendar) cycleEnd.clone();
                            cycleEnd = (Calendar) cycleStart.clone();

                            if ("Weekly".equalsIgnoreCase(frequency)) {
                                cycleEnd.add(Calendar.DAY_OF_YEAR, 7);
                            } else if ("Monthly".equalsIgnoreCase(frequency)) {
                                cycleEnd.add(Calendar.MONTH, 1);
                            } else {
                                Log.w(TAG, "  Unknown frequency: " + frequency);
                                break;
                            }
                            cycleNumber++;

                            if (cycleNumber > 1000) { // Safety check
                                Log.w(TAG, "  Too many cycles, breaking");
                                break;
                            }
                        }

                        // Check if dashboard date is within this cycle
                        if (dashboardTimestamp >= cycleStart.getTimeInMillis() &&
                                dashboardTimestamp <= cycleEnd.getTimeInMillis()) {
                            foundActiveCycle = true;

                            long endTime = cycleEnd.getTimeInMillis();
                            long daysUntilEnd = (endTime - dashboardTimestamp) / (1000 * 60 * 60 * 24);

                            Log.d(TAG, "  Active cycle found:");
                            Log.d(TAG, "    Cycle start: " + dateFormat.format(cycleStart.getTime()));
                            Log.d(TAG, "    Cycle end: " + dateFormat.format(cycleEnd.getTime()));
                            Log.d(TAG, "    Days until end: " + daysUntilEnd);

                            // Add notification if ending within WARNING_DAYS
                            if (daysUntilEnd >= 0 && daysUntilEnd <= WARNING_DAYS) {
                                NotificationItem item = new NotificationItem(
                                        NotificationItem.Type.BUDGET,
                                        name,
                                        categoryName != null ? categoryName : "Budget",
                                        (int) daysUntilEnd,
                                        endTime
                                );
                                synchronized (notifications) {
                                    notifications.add(item);
                                }
                                Log.d(TAG, "  ✓✓✓ NOTIFICATION ADDED: " + name + " ends in " + daysUntilEnd + " days");
                            } else {
                                Log.d(TAG, "  ✗ Not within warning window (daysUntilEnd=" + daysUntilEnd + ", WARNING_DAYS=" + WARNING_DAYS + ")");
                            }
                        } else {
                            Log.d(TAG, "  ✗ Dashboard date not in active cycle");
                        }

                    } catch (ParseException e) {
                        Log.e(TAG, "  Error parsing date", e);
                    }
                }

                Log.d(TAG, "=== Budget check complete. Notifications found: " + notifications.size() + " ===");
                onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking budgets", error.toException());
                onComplete.run();
            }
        });
    }

    private void checkSavingCirclesForDate(DatabaseReference userRef, long dashboardTimestamp,
                                           List<NotificationItem> notifications, Runnable onComplete) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            onComplete.run();
            return;
        }

        String userEmail = currentUser.getEmail();

        Log.d(TAG, "=== Checking Saving Circles ===");
        Log.d(TAG, "Dashboard timestamp: " + dashboardTimestamp + " (" + new Date(dashboardTimestamp) + ")");

        userRef.child("savingCircles").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Found " + snapshot.getChildrenCount() + " saving circles");

                for (DataSnapshot circleSnapshot : snapshot.getChildren()) {
                    try {
                        String groupName = circleSnapshot.child("groupName").getValue(String.class);
                        String frequency = circleSnapshot.child("frequency").getValue(String.class);

                        Log.d(TAG, "Checking circle: " + groupName + " [" + frequency + "]");

                        if (groupName == null || frequency == null) {
                            Log.w(TAG, "  Skipping - missing fields");
                            continue;
                        }

                        // Check if current user is a member
                        DataSnapshot membersSnapshot = circleSnapshot.child("members");
                        String sanitizedEmail = sanitizeEmail(userEmail);

                        if (!membersSnapshot.hasChild(sanitizedEmail)) {
                            Log.d(TAG, "  User is not a member");
                            continue;
                        }

                        DataSnapshot memberSnapshot = membersSnapshot.child(sanitizedEmail);
                        Double allocation = memberSnapshot.child("personalAllocation").getValue(Double.class);

                        if (allocation == null) {
                            Log.w(TAG, "  No allocation found");
                            continue;
                        }

                        Log.d(TAG, "  User allocation: $" + allocation);

                        // Get current cycle relative to dashboard date
                        DataSnapshot cyclesSnapshot = memberSnapshot.child("cycles");
                        Log.d(TAG, "  Found " + cyclesSnapshot.getChildrenCount() + " cycles");

                        for (DataSnapshot cycleSnapshot : cyclesSnapshot.getChildren()) {
                            Long startDate = cycleSnapshot.child("startDate").getValue(Long.class);
                            Long endDate = cycleSnapshot.child("endDate").getValue(Long.class);
                            Double endAmount = cycleSnapshot.child("endAmount").getValue(Double.class);
                            Boolean isComplete = cycleSnapshot.child("complete").getValue(Boolean.class);

                            if (startDate == null || endDate == null || endAmount == null) {
                                Log.w(TAG, "    Cycle missing required fields");
                                continue;
                            }

                            if (isComplete != null && isComplete) {
                                Log.d(TAG, "    Cycle already complete");
                                continue;
                            }

                            Log.d(TAG, "    Cycle: " + new Date(startDate) + " to " + new Date(endDate));

                            // Check if this cycle is active on the dashboard date
                            if (dashboardTimestamp >= startDate && dashboardTimestamp <= endDate) {
                                long daysUntilEnd = (endDate - dashboardTimestamp) / (1000 * 60 * 60 * 24);

                                Log.d(TAG, "    Active cycle found! Days until end: " + daysUntilEnd);

                                // Add notification if ending within WARNING_DAYS
                                if (daysUntilEnd >= 0 && daysUntilEnd <= WARNING_DAYS) {
                                    double remaining = allocation - endAmount;
                                    NotificationItem item = new NotificationItem(
                                            NotificationItem.Type.SAVING_CIRCLE,
                                            groupName,
                                            "Remaining: $" + String.format("%.2f", remaining),
                                            (int) daysUntilEnd,
                                            endDate
                                    );
                                    synchronized (notifications) {
                                        notifications.add(item);
                                    }
                                    Log.d(TAG, "    ✓✓✓ NOTIFICATION ADDED: " + groupName + " ends in " + daysUntilEnd + " days");
                                } else {
                                    Log.d(TAG, "    ✗ Not within warning window (daysUntilEnd=" + daysUntilEnd + ", WARNING_DAYS=" + WARNING_DAYS + ")");
                                }
                                break; // Found active cycle
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error checking saving circle", e);
                    }
                }

                Log.d(TAG, "=== Saving circles check complete. Notifications found: " + notifications.size() + " ===");
                onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking saving circles", error.toException());
                onComplete.run();
            }
        });
    }

    private void updateNotifications(List<NotificationItem> notifications) {
        pendingNotifications.setValue(notifications);
        hasNotifications.setValue(!notifications.isEmpty());
        Log.d(TAG, "Updated notifications: " + notifications.size() + " items");
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
            BUDGET,
            SAVING_CIRCLE
        }

        private Type type;
        private String title;
        private String subtitle;
        private int daysRemaining;
        private long endDate;

        public NotificationItem(Type type, String title, String subtitle,
                                int daysRemaining, long endDate) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.daysRemaining = daysRemaining;
            this.endDate = endDate;
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
            if (daysRemaining == 0) {
                return "Ends today!";
            } else if (daysRemaining == 1) {
                return "Ends tomorrow";
            } else {
                return "Ends in " + daysRemaining + " days";
            }
        }
    }
}