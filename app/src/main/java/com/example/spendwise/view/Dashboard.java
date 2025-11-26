package com.example.spendwise.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.adapter.BudgetAdapter;
import com.example.spendwise.databinding.DashboardBinding;
import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Expense;
import com.example.spendwise.util.ThemeHelper;
import com.example.spendwise.viewModel.BudgetViewModel;
import com.example.spendwise.viewModel.DashboardAnalyticsViewModel;
import com.example.spendwise.viewModel.ExpenseViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.example.spendwise.viewModel.NotificationViewModel;
import com.example.spendwise.adapter.NotificationAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import com.example.spendwise.logic.BudgetAlertLogic;

public class Dashboard extends AppCompatActivity {

    private DashboardBinding binding;
    private ExpenseViewModel expenseViewModel;
    private BudgetViewModel budgetViewModel;
    private DashboardAnalyticsViewModel dashboardAnalyticsViewModel;
    private FirebaseAuth auth;
    private BudgetAdapter remainingBudgetsAdapter;
    private PieChart pieChart;
    private BarChart budgetBarChart;
    private final List<String> budgetLabels = new ArrayList<>();

    private Calendar currentSimulatedDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    private final SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
    private SharedPreferences preferences;
    private Queue<String> alertQueue = new LinkedList<>();
    private Set<String> alertedBudgets = new HashSet<>();
    private BudgetAlertLogic budgetAlertLogic = new BudgetAlertLogic();

    private static final String PREFS_NAME = "SpendWisePrefs";
    private static final String KEY_SIMULATED_DATE = "simulated_date";
    private boolean hasCheckedMissedExpenses = false;

    private NotificationViewModel notificationViewModel;
    private NotificationAdapter notificationAdapter;
    private long currentDashboardTimestamp;

    private boolean isNotificationDialogShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        dashboardAnalyticsViewModel = new ViewModelProvider(this).get(DashboardAnalyticsViewModel.class);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        binding.setLifecycleOwner(this);

        pieChart = findViewById(R.id.spending_pie_chart);
        budgetBarChart = findViewById(R.id.budget_usage_bar_chart);

        setupPieChart();
        setupBudgetBarChart();
        observeAnalyticsData();

        loadSimulatedDate();
        updateDateDisplay();

        setupCalendarSelector();
        setupLogoutButton();
        setupProfileButton();
        setupNavigation();
        setupQuickActions();
        setupBudgetCards();
        setupRemainingBudgetsButton();
        setupNotifications();

        loadDashboardData();
        setupThemeToggle();

        ThemeHelper.loadThemeFromFirebase(this);
    }

    private void setupNotifications() {
        // Observe notifications and show dialog when they exist
        notificationViewModel.getPendingNotifications().observe(this, notifications -> {
            if (notifications != null && !notifications.isEmpty()) {
                showNotificationDialog(notifications);
            }
        });
    }

    private void showNotificationDialog(List<NotificationViewModel.NotificationItem> notifications) {
        Log.d("Dashboard", "showNotificationDialog called with " + notifications.size() + " notifications");

        // Prevent showing multiple dialogs
        if (isNotificationDialogShowing) {
            Log.d("Dashboard", "Dialog already showing");
            return;
        }

        isNotificationDialogShowing = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_reminder, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Setup RecyclerView with click listener
        RecyclerView notificationsRecycler = dialogView.findViewById(R.id.notifications_recycler);
        notificationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        NotificationAdapter adapter = new NotificationAdapter();

        // Set item click listener to handle individual notification clicks
        adapter.setOnItemClickListener(item -> {
            isNotificationDialogShowing = false;
            dialog.dismiss();

            // Navigate based on notification type
            if (item.getType() == NotificationViewModel.NotificationItem.Type.NO_EXPENSES) {
                // Take user to expense log page
                Intent intent = new Intent(Dashboard.this, ExpenseLog.class);
                intent.putExtra("selected_date", shortDateFormat.format(currentSimulatedDate.getTime()));
                startActivity(intent);
            } else if (item.getType() == NotificationViewModel.NotificationItem.Type.BUDGET_90_PERCENT) {
                // Take user to budget page
                Intent intent = new Intent(Dashboard.this, Budgetlog.class);
                intent.putExtra("selected_date", shortDateFormat.format(currentSimulatedDate.getTime()));
                startActivity(intent);
            }
        });

        adapter.setNotifications(notifications);
        notificationsRecycler.setAdapter(adapter);

        // Setup buttons
        View btnDismiss = dialogView.findViewById(R.id.btn_dismiss);
        View btnViewBudgets = dialogView.findViewById(R.id.btn_view_budgets);

        btnDismiss.setOnClickListener(v -> {
            // Dismiss all notifications
            notificationViewModel.clearAllNotifications();
            isNotificationDialogShowing = false;
            dialog.dismiss();
        });

        btnViewBudgets.setOnClickListener(v -> {
            isNotificationDialogShowing = false;
            dialog.dismiss();
            Intent intent = new Intent(Dashboard.this, Budgetlog.class);
            intent.putExtra("selected_date", shortDateFormat.format(currentSimulatedDate.getTime()));
            startActivity(intent);
        });

        dialog.setOnDismissListener(d -> {
            isNotificationDialogShowing = false;
        });

        dialog.show();
        Log.d("Dashboard", "Dialog shown successfully");
        setupThemeToggle();
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setEntryLabelColor(android.graphics.Color.BLACK);
        pieChart.setEntryLabelTextSize(11f);
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    private void setupBudgetBarChart() {
        budgetBarChart.setDrawGridBackground(false);
        budgetBarChart.setDrawBarShadow(false);
        budgetBarChart.setHighlightFullBarEnabled(false);
        budgetBarChart.getDescription().setEnabled(false);

        XAxis xAxis = budgetBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setDrawGridLines(false);

        budgetBarChart.getAxisLeft().setAxisMinimum(0f);
        budgetBarChart.getAxisRight().setAxisMinimum(0f);
        budgetBarChart.getAxisRight().setDrawLabels(false);

        Legend legend = budgetBarChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
    }

    private void observeAnalyticsData() {
        dashboardAnalyticsViewModel.getSpendingByCategoryData().observe(this, pieData -> {
            if (pieData == null) {
                return;
            }
            pieData.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));
            pieChart.setData(pieData);
            pieChart.highlightValues(null);
            pieChart.invalidate();
        });

        dashboardAnalyticsViewModel.getBudgetUsageLabels().observe(this, labels -> {
            budgetLabels.clear();
            if (labels != null) {
                budgetLabels.addAll(labels);
            }
            XAxis xAxis = budgetBarChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(budgetLabels));
            xAxis.setLabelCount(Math.max(budgetLabels.size(), 1));
        });

        dashboardAnalyticsViewModel.getBudgetUsageData().observe(this, barData -> {
            if (barData == null) {
                return;
            }

            float groupSpace = 0.2f;
            float barSpace = 0.05f;
            float barWidth = 0.35f;

            barData.setBarWidth(barWidth);
            budgetBarChart.setData(barData);

            int groupCount = budgetLabels.size();
            if (groupCount == 0 && barData.getDataSetCount() > 0) {
                groupCount = barData.getDataSetByIndex(0).getEntryCount();
            }
            if (groupCount == 0) {
                groupCount = 1;
            }
            budgetBarChart.getXAxis().setAxisMinimum(0f);
            budgetBarChart.getXAxis().setAxisMaximum(0f + budgetBarChart.getBarData().getGroupWidth(groupSpace, barSpace) * groupCount);

            if (barData.getDataSetCount() > 1) {
                budgetBarChart.groupBars(0f, groupSpace, barSpace);
            }

            budgetBarChart.invalidate();
        });
    }

    private void loadSimulatedDate() {
        long savedDate = preferences.getLong(KEY_SIMULATED_DATE, -1);
        currentSimulatedDate = Calendar.getInstance();

        if (savedDate != -1) {
            currentSimulatedDate.setTimeInMillis(savedDate);
        }
    }

    private void saveSimulatedDate() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_SIMULATED_DATE, currentSimulatedDate.getTimeInMillis());
        editor.apply();
    }

    public static Date getSimulatedDate(SharedPreferences prefs) {
        long savedDate = prefs.getLong(KEY_SIMULATED_DATE, -1);
        if (savedDate != -1) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(savedDate);
            return cal.getTime();
        }
        return new Date();
    }

    private void updateDateDisplay() {
        TextView dateText = findViewById(R.id.selected_date_text);
        dateText.setText(dateFormat.format(currentSimulatedDate.getTime()));

        TextView periodInfo = findViewById(R.id.period_info);
        Calendar start = (Calendar) currentSimulatedDate.clone();
        start.set(Calendar.DAY_OF_MONTH, 1);
        Calendar end = (Calendar) currentSimulatedDate.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));

        SimpleDateFormat periodFormat = new SimpleDateFormat("MMM d", Locale.US);
        String periodText = String.format("Current Period: %s - %s",
                periodFormat.format(start.getTime()),
                periodFormat.format(end.getTime()));
        periodInfo.setText(periodText);
    }

    private void setupCalendarSelector() {
        View calendarSelector = findViewById(R.id.calendar_selector);
        calendarSelector.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    currentSimulatedDate.set(year, month, dayOfMonth);
                    saveSimulatedDate();

                    // Clear the dismiss preference for the new date
                    String newDate = shortDateFormat.format(currentSimulatedDate.getTime());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.remove("notifications_dismissed_" + newDate);
                    editor.apply();

                    updateDateDisplay();
                    loadDashboardData();
                    Toast.makeText(this,
                            "Date updated! All time-based features will use this date.",
                            Toast.LENGTH_LONG).show();
                },
                currentSimulatedDate.get(Calendar.YEAR),
                currentSimulatedDate.get(Calendar.MONTH),
                currentSimulatedDate.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.setTitle("Select Testing Date");
        datePickerDialog.show();
    }

    private void setupLogoutButton() {
        View logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        // Note: We don't clear theme preference anymore
        // It will remain in Firebase for when user logs back in

        // Only clear other preferences
        SharedPreferences.Editor editor = preferences.edit();
        String themeMode = String.valueOf(ThemeHelper.getThemeMode(this));
        editor.clear();
        // Restore theme preference
        editor.putInt("theme_mode", Integer.parseInt(themeMode));
        editor.apply();

        auth.signOut();

        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void setupProfileButton() {
        View profileButton = findViewById(R.id.profile_button);
        if (profileButton != null) {
            profileButton.setOnClickListener(v -> {
                startActivity(new Intent(Dashboard.this, ProfileActivity.class));
            });
        }
    }

    private void setupNavigation() {
        View dashboardNavigate = findViewById(R.id.dashboard_navigate);
        View expenseLogNavigate = findViewById(R.id.expenseLog_navigate);
        View budgetNavigate = findViewById(R.id.budget_navigate);
        View savingCircleNavigate = findViewById(R.id.savingCircle_navigate);
        View chatbotNavigate = findViewById(R.id.chatbot_navigate);

        dashboardNavigate.setOnClickListener(v -> {
            // Already on dashboard
        });

        expenseLogNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExpenseLog.class);
            intent.putExtra("selected_date",
                    shortDateFormat.format(currentSimulatedDate.getTime()));
            startActivity(intent);
        });
        budgetNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, Budgetlog.class);
            intent.putExtra("selected_date",
                    shortDateFormat.format(currentSimulatedDate.getTime()));
            startActivity(intent);
        });

        savingCircleNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, SavingCircleLog.class);
            intent.putExtra("selected_date",
                    shortDateFormat.format(currentSimulatedDate.getTime()));
            startActivity(intent);
        });

        chatbotNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, Chatbot.class);
            intent.putExtra("selected_date",
                    shortDateFormat.format(currentSimulatedDate.getTime()));
            startActivity(intent);
        });
    }

    private void setupQuickActions() {
        MaterialCardView addExpenseCard = findViewById(R.id.add_expense_card);
        MaterialCardView addBudgetCard = findViewById(R.id.add_budget_card);

        addExpenseCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ExpenseLog.class);
            intent.putExtra("selected_date",
                    shortDateFormat.format(currentSimulatedDate.getTime()));
            startActivity(intent);
        });

        addBudgetCard.setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, Budgetlog.class);
            intent.putExtra("selected_date",
                    shortDateFormat.format(currentSimulatedDate.getTime()));
            startActivity(intent);
        });
    }

    private void setupBudgetCards() {
        MaterialCardView weeklyCard = findViewById(R.id.weekly_budget_card);
        MaterialCardView monthlyCard = findViewById(R.id.monthly_budget_card);

        weeklyCard.setClickable(false);
        monthlyCard.setClickable(false);

        weeklyCard.setFocusable(false);
        monthlyCard.setFocusable(false);
    }

    private void showFilteredBudgets(String frequency) {
        RecyclerView recyclerView = findViewById(R.id.remaining_budgets_recycler);
        recyclerView.setVisibility(View.VISIBLE);

        budgetViewModel.getBudgets().observe(this, budgets -> {
            List<Budget> filteredBudgets = new ArrayList<>();

            for (Budget budget : budgets) {
                if (frequency.equalsIgnoreCase(budget.getfreq())) {
                    try {
                        Date budgetDate = shortDateFormat.parse(budget.getDate());
                        if (budgetDate != null) {
                            boolean isInPeriod = false;

                            if ("Weekly".equalsIgnoreCase(frequency)) {
                                isInPeriod = isInCurrentWeek(budgetDate);
                            } else if ("Monthly".equalsIgnoreCase(frequency)) {
                                isInPeriod = isInCurrentMonth(budgetDate);
                            }

                            if (isInPeriod) {
                                filteredBudgets.add(budget);
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (filteredBudgets.isEmpty()) {
                Toast.makeText(this,
                        "No " + frequency.toLowerCase()
                                + " budgets found for this period",
                        Toast.LENGTH_SHORT).show();
                recyclerView.setVisibility(View.GONE);
            } else {
                remainingBudgetsAdapter.setBudgets(filteredBudgets);
                Toast.makeText(this, "Showing " + filteredBudgets.size() + " "
                                + frequency.toLowerCase() + " budget(s)",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllRemainingBudgets() {
        budgetViewModel.getBudgets().observe(this, budgets -> {
            expenseViewModel.getExpenses().observe(this, expenses -> {

                List<Budget> remainingBudgets = new ArrayList<>();

                for (Budget budget : budgets) {
                    try {
                        String freq = budget.getfreq();
                        boolean isActive = false;

                        if ("Weekly".equalsIgnoreCase(freq)
                                || "Monthly".equalsIgnoreCase(freq)) {
                            isActive = true;
                        } else if ("Yearly".equalsIgnoreCase(freq)) {
                            Date budgetDate = shortDateFormat.parse(budget.getDate());
                            if (budgetDate != null) {
                                isActive = isInCurrentYear(budgetDate);
                            }
                        } else if ("Daily".equalsIgnoreCase(freq)) {
                            Date budgetDate = shortDateFormat.parse(budget.getDate());
                            if (budgetDate != null) {
                                isActive = isToday(budgetDate);
                            }
                        }

                        if (!isActive) {
                            continue;
                        }

                        double totalSpent = 0.0;
                        for (Expense expense : expenses) {
                            if (expense.getCategory().getDisplayName()
                                    .equalsIgnoreCase(budget.getCategory()
                                            .getDisplayName())) {
                                Date expenseDate = shortDateFormat.parse(expense.getDate());
                                if (expenseDate != null) {
                                    boolean inSamePeriod = false;
                                    if ("Weekly".equalsIgnoreCase(freq)) {
                                        inSamePeriod = isInCurrentWeek(expenseDate);
                                    } else if ("Monthly".equalsIgnoreCase(freq)) {
                                        inSamePeriod = isInCurrentMonth(expenseDate);
                                    } else if ("Yearly".equalsIgnoreCase(freq)) {
                                        inSamePeriod = isInCurrentYear(expenseDate);
                                    } else if ("Daily".equalsIgnoreCase(freq)) {
                                        inSamePeriod = isToday(expenseDate);
                                    }
                                    if (inSamePeriod) {
                                        totalSpent += expense.getAmount();
                                    }
                                }
                            }
                        }

                        Budget remainingBudget = new Budget(
                                budget.getName(),
                                budget.getAmount() - totalSpent,
                                budget.getAmount(),
                                budget.getCategory(),
                                budget.getDate(),
                                budget.getfreq()
                        );

                        remainingBudgets.add(remainingBudget);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if (remainingBudgets.isEmpty()) {
                    Toast.makeText(this, "No active budgets for this period",
                            Toast.LENGTH_SHORT).show();
                } else {
                    remainingBudgetsAdapter.setBudgets(remainingBudgets);
                    Toast.makeText(this,
                            "Showing remaining budgets for current period",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupRemainingBudgetsButton() {
        View remainingBudgetsButton = findViewById(R.id.remaining_budgets_button);
        RecyclerView recyclerView = findViewById(R.id.remaining_budgets_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        remainingBudgetsAdapter = new BudgetAdapter();
        recyclerView.setAdapter(remainingBudgetsAdapter);

        remainingBudgetsButton.setOnClickListener(v -> {
            if (recyclerView.getVisibility() == View.VISIBLE) {
                recyclerView.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                loadAllRemainingBudgets();
            }
        });
    }


    private void loadDashboardData() {
        Calendar weekStart = (Calendar) currentSimulatedDate.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_WEEK, 6);

        Calendar monthStart = (Calendar) currentSimulatedDate.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);

        Calendar monthEnd = (Calendar) currentSimulatedDate.clone();
        monthEnd.set(Calendar.DAY_OF_MONTH,
                monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));

        dashboardAnalyticsViewModel.updateWindow(monthStart.getTime(), monthEnd.getTime());

        // Store current timestamp for notifications
        currentDashboardTimestamp = currentSimulatedDate.getTimeInMillis();

        // Check for notifications based on the dashboard date
        notificationViewModel.checkNotificationsForDate(currentDashboardTimestamp);

        expenseViewModel.getExpenses().observe(this, expenses -> {
            if (expenses != null) {
                calculateAndDisplayTotals(expenses, weekStart, weekEnd,
                        monthStart, monthEnd);
            }
        });

        budgetViewModel.getBudgets().observe(this, budgets -> {
            if (budgets != null) {
                displayBudgetSummary(budgets);
            }
        });
    }

    private void calculateAndDisplayTotals(List<Expense> expenses,
                                           Calendar weekStart, Calendar weekEnd,
                                           Calendar monthStart,
                                           Calendar monthEnd) {
        double totalSpent = 0.0;

        for (Expense expense : expenses) {
            try {
                Date expenseDate = shortDateFormat.parse(expense.getDate());
                if (expenseDate != null) {
                    if (isDateInRange(expenseDate, monthStart.getTime(),
                            monthEnd.getTime())) {
                        totalSpent += expense.getAmount();
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        TextView totalSpentText = findViewById(R.id.total_spent_amount);
        totalSpentText.setText(String.format(Locale.US, "$%.2f", totalSpent));

        updateBudgetDisplay();
    }

    private void updateBudgetDisplay() {
        budgetViewModel.getBudgets().observe(this, budgets -> {
            expenseViewModel.getExpenses().observe(this, expenses -> {

                Map<String, Double> weeklyBudgets = new HashMap<>();
                Map<String, Double> monthlyBudgets = new HashMap<>();

                for (Budget budget : budgets) {
                    try {
                        Date budgetDate = shortDateFormat.parse(budget.getDate());
                        if (budgetDate == null) {
                            continue;
                        }

                        String categoryName = budget.getCategory().getDisplayName();
                        String freq = budget.getfreq();

                        if ("Weekly".equalsIgnoreCase(freq)
                                && isInCurrentWeek(budgetDate)) {
                            weeklyBudgets.put(categoryName,
                                    weeklyBudgets.getOrDefault(categoryName, 0.0)
                                            + budget.getAmount());
                        } else if ("Monthly".equalsIgnoreCase(freq)
                                && isInCurrentMonth(budgetDate)) {
                            monthlyBudgets.put(categoryName,
                                    monthlyBudgets.getOrDefault(categoryName, 0.0)
                                            + budget.getAmount());
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                double totalWeeklySpent = 0.0;
                double totalMonthlySpent = 0.0;

                for (Expense expense : expenses) {
                    try {
                        Date expenseDate = shortDateFormat.parse(expense.getDate());
                        if (expenseDate == null) {
                            continue;
                        }

                        String categoryName = expense.getCategory().getDisplayName();

                        if (isInCurrentWeek(expenseDate)
                                && weeklyBudgets.containsKey(categoryName)) {
                            totalWeeklySpent += expense.getAmount();
                        }

                        if (isInCurrentMonth(expenseDate)
                                && monthlyBudgets.containsKey(categoryName)) {
                            totalMonthlySpent += expense.getAmount();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                double totalWeeklyBudget = 0.0;
                double totalMonthlyBudget = 0.0;

                for (double amount : weeklyBudgets.values()) {
                    totalWeeklyBudget += amount;
                }
                for (double amount : monthlyBudgets.values()) {
                    totalMonthlyBudget += amount;
                }

                double weeklyRemaining = totalWeeklyBudget - totalWeeklySpent;
                double monthlyRemaining = totalMonthlyBudget - totalMonthlySpent;

                TextView weeklyBudgetText = findViewById(R.id.weekly_budget_amount);
                weeklyBudgetText.setText(String.format(Locale.US, "$%.2f",
                        Math.max(0, weeklyRemaining)));
                if (weeklyRemaining < 0) {
                    weeklyBudgetText.setTextColor(getResources()
                            .getColor(android.R.color.holo_red_dark));
                } else {
                    weeklyBudgetText.setTextColor(getResources()
                            .getColor(android.R.color.black));
                }

                TextView monthlyBudgetText = findViewById(R.id.monthly_budget_amount);
                monthlyBudgetText.setText(String.format(Locale.US, "$%.2f",
                        Math.max(0, monthlyRemaining)));
                if (monthlyRemaining < 0) {
                    monthlyBudgetText.setTextColor(getResources()
                            .getColor(android.R.color.holo_red_dark));
                } else {
                    monthlyBudgetText.setTextColor(getResources()
                            .getColor(android.R.color.black));
                }

                // Prepare maps for alert checking
                Map<String, Double> weeklySpentMap = new HashMap<>();
                Map<String, Double> monthlySpentMap = new HashMap<>();

                 // Re-calculating per-category spent for alerts
                 for (Expense expense : expenses) {
                    try {
                        Date expenseDate = shortDateFormat.parse(expense.getDate());
                        if (expenseDate == null) continue;
                        String categoryName = expense.getCategory().getDisplayName();

                        if (isInCurrentWeek(expenseDate)) {
                            weeklySpentMap.put(categoryName, weeklySpentMap.getOrDefault(categoryName, 0.0) + expense.getAmount());
                        }
                        if (isInCurrentMonth(expenseDate)) {
                            monthlySpentMap.put(categoryName, monthlySpentMap.getOrDefault(categoryName, 0.0) + expense.getAmount());
                        }
                    } catch (ParseException e) { e.printStackTrace(); }
                }

                checkBudgetAlerts(weeklyBudgets, monthlyBudgets, weeklySpentMap, monthlySpentMap);
            });
        });
    }

    private void checkBudgetAlerts(Map<String, Double> weeklyBudgets, Map<String, Double> monthlyBudgets,
                                   Map<String, Double> weeklySpent, Map<String, Double> monthlySpent) {
        
        // Check Weekly Budgets
        for (Map.Entry<String, Double> entry : weeklyBudgets.entrySet()) {
            String category = entry.getKey();
            double limit = entry.getValue();
            double spent = weeklySpent.getOrDefault(category, 0.0);
            String budgetKey = "Weekly-" + category;

            if (!alertedBudgets.contains(budgetKey)) {
                if (budgetAlertLogic.isExceeded(spent, limit)) {
                    alertQueue.add(budgetAlertLogic.getExceededMessage(category + " (Weekly)", spent, limit));
                    alertedBudgets.add(budgetKey);
                } else if (budgetAlertLogic.isNearLimit(spent, limit, 0.85)) { // 85% threshold
                    alertQueue.add(budgetAlertLogic.getNearLimitMessage(category + " (Weekly)", spent, limit));
                    alertedBudgets.add(budgetKey);
                }
            }
        }

        // Check Monthly Budgets
        for (Map.Entry<String, Double> entry : monthlyBudgets.entrySet()) {
            String category = entry.getKey();
            double limit = entry.getValue();
            double spent = monthlySpent.getOrDefault(category, 0.0);
            String budgetKey = "Monthly-" + category;

            if (!alertedBudgets.contains(budgetKey)) {
                if (budgetAlertLogic.isExceeded(spent, limit)) {
                    alertQueue.add(budgetAlertLogic.getExceededMessage(category + " (Monthly)", spent, limit));
                    alertedBudgets.add(budgetKey);
                } else if (budgetAlertLogic.isNearLimit(spent, limit, 0.85)) {
                    alertQueue.add(budgetAlertLogic.getNearLimitMessage(category + " (Monthly)", spent, limit));
                    alertedBudgets.add(budgetKey);
                }
            }
        }

        processAlertQueue();
    }

    private void processAlertQueue() {
        if (alertQueue.isEmpty()) return;

        String message = alertQueue.poll();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Budget Alert")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> processAlertQueue())
                .setCancelable(false)
                .show();
    }

    private void displayBudgetSummary(List<Budget> budgets) {
        Map<String, Double> categoryBudgets = new HashMap<>();
        Map<String, Double> categorySpent = new HashMap<>();

        for (Budget budget : budgets) {
            String freq = budget.getfreq();
            boolean isInPeriod = false;

            try {
                Date budgetDate = shortDateFormat.parse(budget.getDate());
                if (budgetDate != null) {
                    if ("Weekly".equalsIgnoreCase(freq)) {
                        isInPeriod = isInCurrentWeek(budgetDate);
                    } else if ("Monthly".equalsIgnoreCase(freq)) {
                        isInPeriod = isInCurrentMonth(budgetDate);
                    } else if ("Yearly".equalsIgnoreCase(freq)) {
                        isInPeriod = isInCurrentYear(budgetDate);
                    } else if ("Daily".equalsIgnoreCase(freq)) {
                        isInPeriod = isToday(budgetDate);
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (isInPeriod) {
                String category = budget.getCategory().getDisplayName();
                categoryBudgets.put(category,
                        categoryBudgets.getOrDefault(category, 0.0)
                                + budget.getAmount());
            }
        }

        expenseViewModel.getExpenses().observe(this, expenses -> {
            if (expenses != null) {
                for (Expense expense : expenses) {
                    String category = expense.getCategory().getDisplayName();
                    categorySpent.put(category,
                            categorySpent.getOrDefault(category, 0.0)
                                    + expense.getAmount());
                }
            }
        });
    }

    private boolean isDateInRange(Date date, Date start, Date end) {
        return !date.before(start) && !date.after(end);
    }

    private boolean isInCurrentWeek(Date date) {
        Calendar weekStart = (Calendar) currentSimulatedDate.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_WEEK, 6);

        return isDateInRange(date, weekStart.getTime(), weekEnd.getTime());
    }

    private boolean isInCurrentMonth(Date date) {
        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(date);

        return dateCalendar.get(Calendar.YEAR) == currentSimulatedDate.get(Calendar.YEAR)
                && dateCalendar.get(Calendar.MONTH)
                == currentSimulatedDate.get(Calendar.MONTH);
    }

    private boolean isInCurrentYear(Date date) {
        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(date);

        return dateCalendar.get(Calendar.YEAR) == currentSimulatedDate.get(Calendar.YEAR);
    }

    private boolean isToday(Date date) {
        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(date);

        return dateCalendar.get(Calendar.YEAR) == currentSimulatedDate.get(Calendar.YEAR)
                && dateCalendar.get(Calendar.MONTH)
                == currentSimulatedDate.get(Calendar.MONTH)
                && dateCalendar.get(Calendar.DAY_OF_MONTH)
                == currentSimulatedDate.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void setupThemeToggle() {
        SwitchMaterial themeSwitch = findViewById(R.id.theme_switch);
        themeSwitch.setChecked(ThemeHelper.isDarkMode(this));

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ThemeHelper.setTheme(this, ThemeHelper.DARK_MODE);
            } else {
                ThemeHelper.setTheme(this, ThemeHelper.LIGHT_MODE);
            }
            recreate(); // Restart activity to apply theme
        });

        // Optional: Sync theme changes from Firebase in real-time
        ThemeHelper.syncThemeFromFirebase(this, newThemeMode -> {
            // Theme changed from another device, recreate activity
            runOnUiThread(() -> {
                themeSwitch.setChecked(newThemeMode == ThemeHelper.DARK_MODE);
                recreate();
            });
        });
    }
}
