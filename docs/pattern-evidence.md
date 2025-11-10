# Design Pattern Evidence (Factory and Strategy)

This page documents the Factory and Strategy patterns implemented for Sprint 3, with code excerpts and a brief rationale for each pattern.

---

## Factory Pattern: ChartFactory

- Location: `app/src/main/java/com/example/spendwise/factory/ChartFactory.java`

### Intent
Centralize creation of MPAndroidChart data objects so Views/ViewModels ask for a chart by intent (e.g., “budget usage”, “category breakdown”) without knowing the concrete construction details. This keeps chart-building logic consistent, testable, and swappable.

### Participants
- Creator: `ChartFactory`
- Products: `PieData`, `BarData` (from MPAndroidChart)
- Clients: `DashboardAnalyticsViewModel` (and any future analytics ViewModels)

### Why this improves the design
- Removes chart-construction details from ViewModels
- Supports easy extension (e.g., swap aggregation rules or color palettes)
- Keeps MPAndroidChart API usage isolated in one place

### Excerpt
```java
// ChartFactory.java (excerpt)
public final class ChartFactory {
    public static PieData buildSpendingByCategoryPie(Map<String, Double> categoryTotals) {
        // ... constructs PieEntries, PieDataSet, and returns PieData
    }

    public static BarData buildBudgetUsageBar(List<BudgetUsageSummary> summaries) {
        // ... constructs two BarDataSets (Spent, Remaining) and returns BarData
    }
}
```

### Example usage
```java
// DashboardAnalyticsViewModel.java (excerpt)
Map<String, Double> categoryTotals = analyticsRepository.calculateCategoryTotals(cachedExpenses, windowStart, windowEnd);
PieData pie = ChartFactory.buildSpendingByCategoryPie(categoryTotals);
spendingByCategoryData.postValue(pie);

List<BudgetUsageSummary> summaries = analyticsRepository.calculateBudgetUsage(cachedBudgets, cachedExpenses, windowStart, windowEnd);
BarData bar = ChartFactory.buildBudgetUsageBar(summaries);
budgetUsageData.postValue(bar);
```

---

## Strategy Pattern: ExpenseSortStrategy

- Location: `app/src/main/java/com/example/spendwise/strategy/`
  - `ExpenseSortStrategy.java`
  - `SortByDateStrategy.java`
  - `SortByAmountStrategy.java`
  - `SortByCategoryStrategy.java`
- Usage: `app/src/main/java/com/example/spendwise/viewModel/ExpenseViewModel.java`

### Intent
Allow the ViewModel to select different sorting behaviors at runtime (by date, amount, category) without changing the code that uses the strategy.

### Participants
- Strategy Interface: `ExpenseSortStrategy`
- Concrete Strategies: `SortByDateStrategy`, `SortByAmountStrategy`, `SortByCategoryStrategy`
- Context: `ExpenseViewModel` (holds a reference and applies it)

### Why this improves the design
- Pluggable policies: runtime selection of sorting without `if/else` chains
- Open/Closed: new sort behaviors do not modify existing code
- Testable: each strategy can be unit tested in isolation

### Excerpt
```java
// ExpenseSortStrategy.java
public interface ExpenseSortStrategy {
    void sort(List<Expense> expenses);
}

// ExpenseViewModel.java (excerpt)
private ExpenseSortStrategy sortStrategy = new SortByDateStrategy();

public void setSortStrategy(ExpenseSortStrategy strategy) {
    this.sortStrategy = strategy;
    applySorting();
}

private void applySorting() {
    List<Expense> current = getExpenses().getValue();
    if (current != null && !current.isEmpty()) {
        List<Expense> sorted = new ArrayList<>(current);
        sortStrategy.sort(sorted);
        // Updating the source (or adapter) with 'sorted' list occurs at the observer/adapter level
    }
}
```

---

## How to Capture Screenshots for the PDF
1. Open each file in Android Studio and capture the relevant portions:
   - `ChartFactory.buildSpendingByCategoryPie(...)` and `ChartFactory.buildBudgetUsageBar(...)`
   - `ExpenseSortStrategy` interface and one of the `SortBy*Strategy` classes
   - `ExpenseViewModel.setSortStrategy(...)` and `applySorting()` usage
2. Paste the screenshots into your Sprint 3 PDF under “Design Pattern Evidence” with 3–4 sentence summaries. This page’s content can be copied for the textual summaries.

---

## Quick Links (relative)
- Factory: `app/src/main/java/com/example/spendwise/factory/ChartFactory.java`
- Strategy: `app/src/main/java/com/example/spendwise/strategy/ExpenseSortStrategy.java`
- Strategy impls: `app/src/main/java/com/example/spendwise/strategy/SortBy*.java`
- Usage (Factory and Strategy): `app/src/main/java/com/example/spendwise/viewModel/`


