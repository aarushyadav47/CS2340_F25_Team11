package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.spendwise.R;
import com.example.spendwise.databinding.SavingcircleDetailBinding;
import com.example.spendwise.model.SavingCircle;
import com.example.spendwise.model.SavingCircleMember;
import com.example.spendwise.model.MemberCycle;
import com.example.spendwise.viewModel.SavingCircleViewModel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SavingCircleDetailActivity extends AppCompatActivity {

    private static final String TAG = "CircleDetailActivity";
    private SavingcircleDetailBinding binding;
    private SavingCircleViewModel savingCircleViewModel;
    private String circleId;
    private long selectedDateTimestamp;
    private String frequency;

    // Helper for formatting currency
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    // Track total progress for overall calculation
    private double totalProgressAmount = 0;
    private int membersProcessed = 0;
    private int totalMembers = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = SavingcircleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        savingCircleViewModel = new ViewModelProvider(this).get(SavingCircleViewModel.class);
        binding.setLifecycleOwner(this);

        Intent intent = getIntent();
        circleId = intent.getStringExtra("CIRCLE_ID");
        selectedDateTimestamp = intent.getLongExtra("SELECTED_DATE", System.currentTimeMillis());

        Log.d(TAG, "Circle ID: " + circleId);
        Log.d(TAG, "Selected Date: " + DATE_FORMAT.format(selectedDateTimestamp));

        if (circleId == null || circleId.isEmpty()) {
            Log.e(TAG, "Error: No CIRCLE_ID provided.");
            finish();
            return;
        }

        observeSavingCircleData();
        observeSavingCircleMembers();
    }

    private void observeSavingCircleData() {
        savingCircleViewModel.getSavingCircleById(circleId).observe(this, savingCircle -> {
            if (savingCircle != null) {
                binding.challengeTitle.setText(savingCircle.getChallengeTitle());
                binding.frequency.setText(savingCircle.getFrequency());
                frequency = savingCircle.getFrequency();

                binding.goalAmount.setText(
                        String.format(Locale.US, "Goal: $%s", CURRENCY_FORMAT.format(savingCircle.getGoalAmount()))
                );

                binding.overallProgressText.setText("Overall Progress: Calculating...");
                binding.overallProgressBar.setProgress(0);
            }
        });
    }

    private void observeSavingCircleMembers() {
        savingCircleViewModel.getSavingCircleMembers(circleId).observe(this, members -> {
            LinearLayout memberContainer = binding.memberContainer;
            memberContainer.removeAllViews();

            if (members == null || members.isEmpty()) {
                TextView noMembers = new TextView(this);
                noMembers.setText("No members in this circle yet.");
                noMembers.setPadding(16, 16, 16, 16);
                memberContainer.addView(noMembers);
                updateOverallProgress(0, 0);
                return;
            }

            totalProgressAmount = 0;
            membersProcessed = 0;
            totalMembers = members.size();

            for (SavingCircleMember member : members) {
                View memberView = getLayoutInflater().inflate(R.layout.item_member_detail, memberContainer, false);

                TextView nameText = memberView.findViewById(R.id.member_name);
                TextView currentAmountText = memberView.findViewById(R.id.member_current_amount);
                TextView joinDateText = memberView.findViewById(R.id.member_join_date);
                TextView historyText = memberView.findViewById(R.id.member_historical_contributions);
                ProgressBar memberProgress = memberView.findViewById(R.id.member_progress_bar);

                nameText.setText(member.getEmail());
                joinDateText.setText(
                        String.format(Locale.US, "Joined: %s", DATE_FORMAT.format(member.getJoinedAt()))
                );

                currentAmountText.setText("Loading...");
                historyText.setText("Loading...");
                memberProgress.setProgress(0);

                memberContainer.addView(memberView);

                if (member.getJoinedAt() > selectedDateTimestamp) {
                    currentAmountText.setText("Not joined yet");
                    historyText.setText("--");
                    memberProgress.setProgress(0);

                    synchronized (this) {
                        membersProcessed++;
                    }
                    checkIfAllMembersProcessed();
                    continue;
                }

                final double allocation = member.getPersonalAllocation();

                // Get or create the cycle for the selected date
                getOrCreateCycleForDate(circleId, member.getEmail(), member.getJoinedAt(),
                        allocation, currentAmountText, memberProgress, historyText);
            }
        });
    }

    /**
     * Get existing cycle or create missing cycles up to the selected date
     */
    private void getOrCreateCycleForDate(String circleId, String memberEmail, long joinDate,
                                         double allocation, TextView currentAmountText,
                                         ProgressBar memberProgress, TextView historyText) {
        savingCircleViewModel.getCycleAtDate(circleId, memberEmail, selectedDateTimestamp,
                new SavingCircleViewModel.OnCycleLoadedListener() {

                    @Override
                    public void onCycleLoaded(MemberCycle cycle) {
                        // Cycle exists, display it
                        displayCycleData(cycle, allocation, currentAmountText, memberProgress);
                        loadMemberHistoricalContributions(memberEmail, historyText);
                    }

                    @Override
                    public void onCycleNotFound() {
                        // No cycle found - need to create missing cycles
                        createMissingCycles(circleId, memberEmail, joinDate, allocation,
                                currentAmountText, memberProgress, historyText);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error: " + message);
                        currentAmountText.setText("Error");
                        historyText.setText("Error");

                        synchronized (SavingCircleDetailActivity.this) {
                            membersProcessed++;
                        }
                        checkIfAllMembersProcessed();
                    }
                });
    }

    /**
     * Create all missing cycles from the last existing cycle to the selected date
     */
    private void createMissingCycles(String circleId, String memberEmail, long joinDate,
                                     double allocation, TextView currentAmountText,
                                     ProgressBar memberProgress, TextView historyText) {
        savingCircleViewModel.getMemberCycleHistory(circleId, memberEmail).observe(this, cycles -> {
            if (cycles == null || cycles.isEmpty()) {
                // No cycles at all
                currentAmountText.setText("No cycles available");
                historyText.setText("--");

                synchronized (SavingCircleDetailActivity.this) {
                    membersProcessed++;
                }
                checkIfAllMembersProcessed();
                return;
            }

            // Find the last cycle
            MemberCycle lastCycle = null;
            for (MemberCycle cycle : cycles) {
                if (lastCycle == null || cycle.getEndDate() > lastCycle.getEndDate()) {
                    lastCycle = cycle;
                }
            }

            if (lastCycle == null || selectedDateTimestamp < lastCycle.getStartDate()) {
                // Selected date is before any cycles
                currentAmountText.setText("No cycle for this date");
                historyText.setText("--");

                synchronized (SavingCircleDetailActivity.this) {
                    membersProcessed++;
                }
                checkIfAllMembersProcessed();
                return;
            }

            // If selected date is within existing cycles, we already checked - shouldn't be here
            if (selectedDateTimestamp < lastCycle.getEndDate()) {
                displayCycleData(lastCycle, allocation, currentAmountText, memberProgress);
                loadMemberHistoricalContributions(memberEmail, historyText);
                return;
            }

            // Create cycles from lastCycle to selectedDate
            createCyclesUpToDate(circleId, memberEmail, lastCycle, allocation,
                    currentAmountText, memberProgress, historyText);
        });
    }

    /**
     * Create cycles sequentially from the last cycle to cover the selected date
     * Each new cycle starts with FRESH allocation (budget resets)
     * IMPORTANT: Auto-created cycles should have $0 contribution (user wasn't active)
     */
    private void createCyclesUpToDate(String circleId, String memberEmail, MemberCycle lastCycle,
                                      double allocation, TextView currentAmountText,
                                      ProgressBar memberProgress, TextView historyText) {

        // First, complete the last cycle if it should be complete
        if (!lastCycle.isComplete() && lastCycle.getEndDate() <= selectedDateTimestamp) {
            Log.d(TAG, "Completing cycle: " + lastCycle.getCycleId());
            completeCycleInDatabase(circleId, memberEmail, lastCycle);
        }

        // Calculate next cycle dates
        long nextStartDate = lastCycle.getEndDate();
        long nextEndDate = calculateNextEndDate(nextStartDate, frequency);

        // Create next cycle - but we need to determine if it's auto-created or real
        MemberCycle nextCycle = new MemberCycle(nextStartDate, nextEndDate, allocation);

        // Check if this new cycle should already be complete (if it's in the past)
        boolean shouldBeComplete = nextCycle.getEndDate() <= selectedDateTimestamp;
        if (shouldBeComplete) {
            nextCycle.setComplete(true);
            // CRITICAL FIX: Auto-created cycles have $0 contribution
            // They represent periods where the user wasn't actively managing the circle
            nextCycle.setStartAmount(0);
            nextCycle.setEndAmount(0);
            nextCycle.setSpent(0);
            Log.d(TAG, "Creating auto-completed cycle (no contribution): " + nextCycle.getCycleId());
        } else {
            // This is the current/future cycle - starts with full allocation
            nextCycle.setStartAmount(allocation);
            nextCycle.setEndAmount(allocation);
            Log.d(TAG, "Creating active cycle: " + nextCycle.getCycleId());
        }

        // Check if we need to create this cycle
        if (selectedDateTimestamp >= nextCycle.getStartDate()) {
            savingCircleViewModel.createCycle(circleId, memberEmail, nextCycle,
                    new SavingCircleViewModel.OnCycleCreatedListener() {

                        @Override
                        public void onCycleCreated(MemberCycle cycle) {
                            Log.d(TAG, "Cycle created: " + cycle.getCycleId() +
                                    " (complete: " + cycle.isComplete() +
                                    ", endAmount: " + cycle.getEndAmount() + ")");

                            // Check if we need more cycles
                            if (selectedDateTimestamp >= cycle.getEndDate()) {
                                // Need to create more cycles
                                createCyclesUpToDate(circleId, memberEmail, cycle, allocation,
                                        currentAmountText, memberProgress, historyText);
                            } else {
                                // This cycle covers the selected date - display it
                                displayCycleData(cycle, allocation, currentAmountText, memberProgress);
                                loadMemberHistoricalContributions(memberEmail, historyText);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "Error creating cycle: " + message);
                            currentAmountText.setText("Error creating cycle");

                            synchronized (SavingCircleDetailActivity.this) {
                                membersProcessed++;
                            }
                            checkIfAllMembersProcessed();
                        }
                    });
        } else {
            // Last cycle covers the selected date
            displayCycleData(lastCycle, allocation, currentAmountText, memberProgress);
            loadMemberHistoricalContributions(memberEmail, historyText);
        }
    }

    /**
     * Mark a cycle as complete in the database
     */
    private void completeCycleInDatabase(String circleId, String memberEmail, MemberCycle cycle) {
        cycle.setComplete(true);
        savingCircleViewModel.createCycle(circleId, memberEmail, cycle,
                new SavingCircleViewModel.OnCycleCreatedListener() {
                    @Override
                    public void onCycleCreated(MemberCycle updatedCycle) {
                        Log.d(TAG, "Cycle marked complete: " + updatedCycle.getCycleId());
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error completing cycle: " + message);
                    }
                });
    }

    /**
     * Calculate the end date for the next cycle based on frequency
     */
    private long calculateNextEndDate(long startDate, String frequency) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startDate);

        if ("Weekly".equals(frequency)) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        } else { // Monthly
            calendar.add(Calendar.MONTH, 1);
        }

        return calendar.getTimeInMillis();
    }

    /**
     * Display cycle data in the UI
     */
    private void displayCycleData(MemberCycle cycle, double allocation,
                                  TextView currentAmountText, ProgressBar memberProgress) {
        double leftover = cycle.getEndAmount();

        currentAmountText.setText(
                String.format(Locale.US, "$%s / $%s",
                        CURRENCY_FORMAT.format(leftover),
                        CURRENCY_FORMAT.format(allocation))
        );

        int progress = allocation > 0 ? (int) ((leftover / allocation) * 100) : 0;
        memberProgress.setProgress(Math.min(progress, 100));
    }

    /**
     * Load historical contributions from completed cycles
     */
    private void loadMemberHistoricalContributions(String memberEmail, TextView historyText) {
        savingCircleViewModel.getMemberCycleHistory(circleId, memberEmail).observe(this, cycles -> {
            if (cycles != null && !cycles.isEmpty()) {
                double historicalContribution = 0;
                int completedCount = 0;

                for (MemberCycle cycle : cycles) {
                    // Only count COMPLETED cycles that ended on or before selected date
                    if (cycle.isComplete() && cycle.getEndDate() <= selectedDateTimestamp) {
                        historicalContribution += cycle.getEndAmount();
                        completedCount++;
                    }
                }

                synchronized (this) {
                    totalProgressAmount += historicalContribution;
                    membersProcessed++;
                }

                if (completedCount > 0) {
                    historyText.setText(
                            String.format(Locale.US, "Contributed: $%s (%d cycles)",
                                    CURRENCY_FORMAT.format(historicalContribution),
                                    completedCount)
                    );
                } else {
                    historyText.setText("No completed cycles yet");
                }

                checkIfAllMembersProcessed();
            } else {
                historyText.setText("No history");

                synchronized (this) {
                    membersProcessed++;
                }
                checkIfAllMembersProcessed();
            }
        });
    }

    private void checkIfAllMembersProcessed() {
        if (membersProcessed >= totalMembers) {
            savingCircleViewModel.getSavingCircleById(circleId).observe(this, circle -> {
                if (circle != null) {
                    updateOverallProgress(totalProgressAmount, circle.getGoalAmount());
                }
            });
        }
    }

    private void updateOverallProgress(double totalContributions, double goalAmount) {
        int progressPercentage = goalAmount > 0 ?
                (int) ((totalContributions / goalAmount) * 100) : 0;

        binding.overallProgressText.setText(
                String.format(Locale.US, "Overall Progress: $%s / $%s (%d%%)",
                        CURRENCY_FORMAT.format(totalContributions),
                        CURRENCY_FORMAT.format(goalAmount),
                        progressPercentage)
        );
        binding.overallProgressBar.setProgress(Math.min(progressPercentage, 100));

        Log.d(TAG, "Total Progress: $" + totalContributions + " / $" + goalAmount);
    }
}