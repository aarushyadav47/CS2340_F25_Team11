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
                TextView cycleDatesText = memberView.findViewById(R.id.member_cycle_dates);
                TextView historyText = memberView.findViewById(R.id.member_historical_contributions);
                ProgressBar memberProgress = memberView.findViewById(R.id.member_progress_bar);

                nameText.setText(member.getEmail());
                joinDateText.setText(
                        String.format(Locale.US, "Joined: %s", DATE_FORMAT.format(member.getJoinedAt()))
                );

                currentAmountText.setText("Loading...");
                cycleDatesText.setText("Loading cycle dates...");
                historyText.setText("Loading...");
                memberProgress.setProgress(0);

                memberContainer.addView(memberView);

                if (member.getJoinedAt() > selectedDateTimestamp) {
                    currentAmountText.setText("Not joined yet");
                    cycleDatesText.setText("Not joined yet");
                    historyText.setText("--");
                    memberProgress.setProgress(0);

                    synchronized (this) {
                        membersProcessed++;
                    }
                    checkIfAllMembersProcessed();
                    continue;
                }

                final double allocation = member.getPersonalAllocation();

                // 🌟 NEW: Trigger the check/completion/rollover logic for the *current* date
                // This ensures any expired cycles are marked complete immediately.
                savingCircleViewModel.checkAndCreateNextCycle(circleId, member.getEmail(), frequency);

                // Get or create the cycle for the selected date
                getOrCreateCycleForDate(circleId, member.getEmail(), member.getJoinedAt(),
                        allocation, currentAmountText, cycleDatesText, memberProgress, historyText);
            }
        });
    }


    /**
     * Get existing cycle or trigger cycle creation logic in the ViewModel
     * to ensure a cycle is active for the selected date.
     */
    private void getOrCreateCycleForDate(String circleId, String memberEmail, long joinDate,
                                         double allocation, TextView currentAmountText,
                                         TextView cycleDatesText, ProgressBar memberProgress,
                                         TextView historyText) {

        // Call the new high-level command in the ViewModel
        savingCircleViewModel.ensureCycleIsActiveForDate(
                circleId, memberEmail, selectedDateTimestamp, frequency, allocation,
                new SavingCircleViewModel.OnCycleLoadedListener() {

                    @Override
                    public void onCycleLoaded(MemberCycle cycle) {
                        // Cycle exists or was just created/updated, display it
                        displayCycleData(cycle, allocation, currentAmountText, cycleDatesText, memberProgress);
                        loadMemberHistoricalContributions(memberEmail, historyText);
                    }

                    @Override
                    public void onCycleNotFound() {
                        // This path indicates a failure in the ViewModel logic or data setup.
                        Log.e(TAG, "Cycle logic failed to ensure an active cycle.");
                        currentAmountText.setText("Cycle Error");
                        cycleDatesText.setText("Cycle Error");
                        historyText.setText("Error");

                        synchronized (SavingCircleDetailActivity.this) {
                            membersProcessed++;
                        }
                        checkIfAllMembersProcessed();
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error in cycle logic: " + message);
                        currentAmountText.setText("Error: " + message);
                        cycleDatesText.setText("Error");
                        historyText.setText("Error");

                        synchronized (SavingCircleDetailActivity.this) {
                            membersProcessed++;
                        }
                        checkIfAllMembersProcessed();
                    }
                });
    }

    /**
     * Display cycle data in the UI
     */
    private void displayCycleData(MemberCycle cycle, double allocation,
                                  TextView currentAmountText, TextView cycleDatesText,
                                  ProgressBar memberProgress) {
        double leftover = cycle.getEndAmount();

        currentAmountText.setText(
                String.format(Locale.US, "$%s / $%s",
                        CURRENCY_FORMAT.format(leftover),
                        CURRENCY_FORMAT.format(allocation))
        );

        // Display cycle start/end dates
        String cycleDates = String.format(Locale.US, "Cycle: %s - %s",
                DATE_FORMAT.format(cycle.getStartDate()),
                DATE_FORMAT.format(cycle.getEndDate()));
        cycleDatesText.setText(cycleDates);
        Log.d(TAG, "Setting cycle dates for " + cycleDates);

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
                            String.format(Locale.US, "Contribution to Shared Goal: $%s (%d cycles)",
                                    CURRENCY_FORMAT.format(historicalContribution),
                                    completedCount)
                    );
                } else {
                    historyText.setText("Contribution to Shared Goal: $0.00");
                }

                checkIfAllMembersProcessed();
            } else {
                historyText.setText("Contribution to Shared Goal: $0.00");

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