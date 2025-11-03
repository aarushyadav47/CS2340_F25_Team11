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
import com.example.spendwise.viewModel.SavingCircleViewModel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class SavingCircleDetailActivity extends AppCompatActivity {

    private static final String TAG = "CircleDetailActivity";
    private SavingcircleDetailBinding binding;
    private SavingCircleViewModel savingCircleViewModel;
    private String circleId;

    // Helper for formatting currency
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy", Locale.US);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Setup Binding and ViewModel
        binding = SavingcircleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        savingCircleViewModel = new ViewModelProvider(this).get(SavingCircleViewModel.class);
        binding.setLifecycleOwner(this);

        // 2. Get Circle ID from Intent
        Intent intent = getIntent();
        circleId = intent.getStringExtra("CIRCLE_ID");

        if (circleId == null || circleId.isEmpty()) {
            Log.e(TAG, "Error: No CIRCLE_ID provided.");
            finish(); // Close activity if we don't know what to display
            return;
        }

        // 3. Load and Display Data
        observeSavingCircleData();
        observeSavingCircleMembers();
    }

    private void observeSavingCircleData() {
        // Assume you add a method in ViewModel to fetch a single circle by ID
        savingCircleViewModel.getSavingCircleById(circleId).observe(this, savingCircle -> {
            if (savingCircle != null) {
                // Update overall goal details
                binding.challengeTitle.setText(savingCircle.getChallengeTitle());
                binding.goalAmount.setText(
                        String.format(Locale.US, "Goal: $%s", CURRENCY_FORMAT.format(savingCircle.getGoalAmount()))
                );
                binding.frequency.setText(savingCircle.getFrequency());

                // Placeholder for Overall Progress (Requires calculating total member contributions vs goal)
                // For simplicity here, we'll assume 0% progress initially.
                binding.overallProgressText.setText("Overall Progress: 0%");
                binding.overallProgressBar.setProgress(0);
            } else {
                Log.w(TAG, "Saving Circle not found for ID: " + circleId);
            }
        });
    }

    private void observeSavingCircleMembers() {
        // Assume you add a method in ViewModel to fetch members for a circle
        savingCircleViewModel.getSavingCircleMembers(circleId).observe(this, members -> {
            LinearLayout memberContainer = binding.memberContainer;
            memberContainer.removeAllViews(); // Clear previous views

            if (members != null && !members.isEmpty()) {
                for (int i = 0; i < members.size(); i++) {
                    // This is complex, so we'll just display basic info first
                    // The member cycle data should ideally be fetched/calculated within the ViewModel
                    // or fetched per member here. For now, display allocation and start date.

                    View memberView = getLayoutInflater().inflate(R.layout.item_member_detail, memberContainer, false);

                    TextView nameText = memberView.findViewById(R.id.member_name);
                    TextView currentAmountText = memberView.findViewById(R.id.member_current_amount);
                    TextView joinDateText = memberView.findViewById(R.id.member_join_date);
                    TextView historyText = memberView.findViewById(R.id.member_historical_contributions);
                    ProgressBar memberProgress = memberView.findViewById(R.id.member_progress_bar);

                    // 1. Member Info
                    nameText.setText(members.get(i).getEmail()); // Use email as name
                    joinDateText.setText(
                            String.format(Locale.US, "Joined: %s", DATE_FORMAT.format(members.get(i).getJoinedAt()))
                    );

                    // 2. Current Amount / Allocation
                    double currentAmount = members.get(i).getCurrentAmount();
                    double allocation = members.get(i).getPersonalAllocation();

                    currentAmountText.setText(
                            String.format(Locale.US, "$%s / $%s",
                                    CURRENCY_FORMAT.format(currentAmount),
                                    CURRENCY_FORMAT.format(allocation))
                    );

                    // 3. Progress Bar
                    int progress = allocation > 0 ? (int) ((currentAmount / allocation) * 100) : 0;
                    memberProgress.setProgress(progress);

                    // 4. Historical Contributions (Placeholder)
                    // The calculation for "historical contributions (excluding this month's)"
                    // requires complex logic involving MemberCycle history, which belongs in the ViewModel.
                    // For now, we'll display a placeholder value.
                    historyText.setText("History: (Click for cycles)");

                    memberContainer.addView(memberView);
                }
            } else {
                // Display message if no members
                TextView noMembers = new TextView(this);
                noMembers.setText("No members in this circle yet.");
                memberContainer.addView(noMembers);
            }
        });
    }
}