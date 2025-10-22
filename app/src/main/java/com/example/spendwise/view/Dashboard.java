package com.example.spendwise.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendwise.R;
import com.example.spendwise.databinding.DashboardBinding;
import com.example.spendwise.model.Budget;
import com.example.spendwise.model.Expense;
import com.example.spendwise.viewModel.BudgetViewModel;
import com.example.spendwise.viewModel.ExpenseViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.example.spendwise.adapter.BudgetAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

public class Dashboard extends AppCompatActivity {

    private DashboardBinding binding;
    private ExpenseViewModel expenseViewModel;
    private BudgetViewModel budgetViewModel;
    private FirebaseAuth auth;
    private BudgetAdapter remainingBudgetsAdapter;


    private Calendar currentSimulatedDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy",
            Locale.US);
    private SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy",
            Locale.US);
    private SharedPreferences preferences;

    private static final String PREFS_NAME = "SpendWisePrefs";
    private static final String KEY_SIMULATED_DATE = "simulated_date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize ViewModels
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);
        binding.setLifecycleOwner(this);

        // Load or initialize simulated date
        loadSimulatedDate();
        updateDateDisplay();

        // Setup UI components
        setupCalendarSelector();
        setupLogoutButton();
        setupNavigation();
        setupQuickActions();
        setupBudgetCards();
        setupRemainingBudgetsButton();

        // Load dashboard data
        loadDashboardData();
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

        // Update period info
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
                    updateDateDisplay();
                    loadDashboardData(); // Refresh data with new date
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
        // Clear simulated date
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        // Sign out from Firebase
        auth.signOut();

        // Clear activity stack and go to login
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
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

        savingCircleNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, SavingCircle.class)));

        chatbotNavigate.setOnClickListener(v ->
                startActivity(new Intent(this, Chatbot.class)));
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

        // Disable clicks
        weeklyCard.setClickable(false);
        monthlyCard.setClickable(false);

        // Optionally make the visual feedback consistent
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

                        // Weekly/Monthly budgets are always active; others check period
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

                        // Calculate total spent in the current period for this budget
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

                        // Create remaining budget object
                        Budget remainingBudget = new Budget(
                                budget.getName(),
                                budget.getAmount() - totalSpent, // remaining
                                budget.getAmount(), // original amount
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

        // Setup the RecyclerView
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
        // Calculate date ranges
        Calendar weekStart = (Calendar) currentSimulatedDate.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_WEEK, 6);

        Calendar monthStart = (Calendar) currentSimulatedDate.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);

        Calendar monthEnd = (Calendar) currentSimulatedDate.clone();
        monthEnd.set(Calendar.DAY_OF_MONTH,
                monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));

        // Load expenses and calculate totals
        expenseViewModel.getExpenses().observe(this, expenses -> {
            if (expenses != null) {
                calculateAndDisplayTotals(expenses, weekStart, weekEnd,
                        monthStart, monthEnd);
            }
        });

        // Load budgets and display remaining amounts
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
                    // Total spent in current month (for the main spending card)
                    if (isDateInRange(expenseDate, monthStart.getTime(),
                            monthEnd.getTime())) {
                        totalSpent += expense.getAmount();
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Update total spent UI (main card at top)
        TextView totalSpentText = findViewById(R.id.total_spent_amount);
        totalSpentText.setText(String.format(Locale.US, "$%.2f", totalSpent));

        // Calculate weekly and monthly budgets (now with category filtering)
        updateBudgetDisplay();
    }

    private void updateBudgetDisplay() {
        budgetViewModel.getBudgets().observe(this, budgets -> {
            expenseViewModel.getExpenses().observe(this, expenses -> {

                // Track which categories have active budgets
                Map<String, Double> weeklyBudgets = new HashMap<>();
                Map<String, Double> monthlyBudgets = new HashMap<>();

                // Collect all active budgets per frequency
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

                // Calculate spent amounts only for categories with budgets
                double totalWeeklySpent = 0.0;
                double totalMonthlySpent = 0.0;

                for (Expense expense : expenses) {
                    try {
                        Date expenseDate = shortDateFormat.parse(expense.getDate());
                        if (expenseDate == null) {
                            continue;
                        }

                        String categoryName = expense.getCategory().getDisplayName();

                        // Only count weekly expenses if category has a weekly budget
                        if (isInCurrentWeek(expenseDate)
                                && weeklyBudgets.containsKey(categoryName)) {
                            totalWeeklySpent += expense.getAmount();
                        }

                        // Only count monthly expenses if category has a monthly budget
                        if (isInCurrentMonth(expenseDate)
                                && monthlyBudgets.containsKey(categoryName)) {
                            totalMonthlySpent += expense.getAmount();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                // Calculate total budgets
                double totalWeeklyBudget = 0.0;
                double totalMonthlyBudget = 0.0;

                for (double amount : weeklyBudgets.values()) {
                    totalWeeklyBudget += amount;
                }
                for (double amount : monthlyBudgets.values()) {
                    totalMonthlyBudget += amount;
                }

                // Calculate remaining amounts
                double weeklyRemaining = totalWeeklyBudget - totalWeeklySpent;
                double monthlyRemaining = totalMonthlyBudget - totalMonthlySpent;

                // Update weekly budget card
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

                // Update monthly budget card
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
            });
        });
    }

    private void displayBudgetSummary(List<Budget> budgets) {
        // Filter budgets based on current simulated date period
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

        // Calculate spent per category
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
        // Reload data when returning to dashboard
        loadDashboardData();
    }
}