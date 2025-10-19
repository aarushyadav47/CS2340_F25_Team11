package com.example.spendwise.repo;

import androidx.annotation.NonNull;

import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseRepo {

    private static volatile ExpenseRepo instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private ExpenseRepo() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public static ExpenseRepo getInstance() {
        if (instance == null) {
            synchronized (ExpenseRepo.class) {
                if (instance == null) {
                    instance = new ExpenseRepo();
                }
            }
        }
        return instance;
    }

    private CollectionReference userExpensesCollection() {
        String uid = safeUid();
        return db.collection("users").document(uid).collection("expenses");
    }

    private String safeUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
    }

    public void addExpense(Expense expense, @NonNull RepoCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", expense.getId());
        data.put("name", expense.getName());
        data.put("amount", expense.getAmount());
        data.put("category", expense.getCategory().name());
        // Store date as Firestore Timestamp for consistency
        java.util.Date parsedDate;
        try {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            parsedDate = fmt.parse(expense.getDate());
        } catch (Exception e) {
            parsedDate = new java.util.Date();
        }
        data.put("date", new Timestamp(parsedDate));
        data.put("notes", expense.getNotes());

        userExpensesCollection().add(data)
                .addOnSuccessListener(r -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteExpense(String expenseId, @NonNull RepoCallback callback) {
        userExpensesCollection().whereEqualTo("id", expenseId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        callback.onError("Expense not found");
                        return;
                    }
                    snap.getDocuments().get(0).getReference().delete()
                            .addOnSuccessListener(v -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void fetchExpenses(@NonNull ExpensesCallback callback) {
        userExpensesCollection()
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    List<Expense> list = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getString("id");
                        String name = doc.getString("name");
                        // amount can be stored as Long or Double
                        Number amountNum = (Number) doc.get("amount");
                        Double amount = amountNum != null ? amountNum.doubleValue() : null;
                        String categoryStr = doc.getString("category");
                        // date may be Timestamp or String; accept both
                        Object dateObj = doc.get("date");
                        String date;
                        if (dateObj instanceof Timestamp) {
                            java.util.Date d = ((Timestamp) dateObj).toDate();
                            date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(d);
                        } else if (dateObj instanceof String) {
                            date = (String) dateObj;
                        } else {
                            date = null;
                        }
                        String notes = doc.getString("notes");

                        Category category = Category.OTHER;
                        try {
                            if (categoryStr != null)
                                category = Category.valueOf(categoryStr);
                        } catch (IllegalArgumentException ignored) {
                        }

                        if (name != null && amount != null && date != null) {
                            Expense e = new Expense(name, amount, category, date, notes != null ? notes : "");
                            e.setId(id);
                            list.add(e);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void seedIfEmpty(@NonNull RepoCallback callback) {
        userExpensesCollection().limit(1).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        List<Expense> seeds = new ArrayList<>();
                        seeds.add(new Expense("Coffee", 3.75, Category.FOOD, "2025-10-01", "Latte"));
                        seeds.add(new Expense("MARTA", 2.50, Category.TRANSPORT, "2025-10-03", "One-way"));

                        final int total = seeds.size();
                        final int[] done = { 0 };
                        final boolean[] failed = { false };
                        for (Expense e : seeds) {
                            addExpense(e, new RepoCallback() {
                                @Override
                                public void onSuccess() {
                                    if (failed[0])
                                        return;
                                    done[0]++;
                                    if (done[0] == total)
                                        callback.onSuccess();
                                }

                                @Override
                                public void onError(String error) {
                                    if (!failed[0]) {
                                        failed[0] = true;
                                        callback.onError(error);
                                    }
                                }
                            });
                        }
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public interface RepoCallback {
        void onSuccess();

        void onError(String error);
    }

    public interface ExpensesCallback {
        void onSuccess(List<Expense> expenses);

        void onError(String error);
    }
}
