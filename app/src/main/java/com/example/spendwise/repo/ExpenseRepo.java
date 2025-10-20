package com.example.spendwise.repo;

import androidx.annotation.NonNull;

import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseRepo {

	private static volatile ExpenseRepo instance;
	private final FirebaseDatabase db;
	private final FirebaseAuth auth;

	private ExpenseRepo() {
		db = FirebaseDatabase.getInstance();
		auth = FirebaseAuth.getInstance();
	}

	public static ExpenseRepo getInstance() {
		if (instance == null) {
			synchronized (ExpenseRepo.class) {
				if (instance == null) instance = new ExpenseRepo();
			}
		}
		return instance;
	}

	private DatabaseReference userExpensesRef() {
		return db.getReference("users").child(safeUid()).child("expenses");
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
		data.put("date", expense.getDate()); // store as string (yyyy-MM-dd preferred)
		data.put("notes", expense.getNotes());

		userExpensesRef().push().setValue(data, (error, ref) -> {
			if (error != null) callback.onError(error.getMessage()); else callback.onSuccess();
		});
	}

	public void deleteExpense(String expenseId, @NonNull RepoCallback callback) {
		userExpensesRef().orderByChild("id").equalTo(expenseId)
				.addListenerForSingleValueEvent(new ValueEventListener() {
					@Override public void onDataChange(@NonNull DataSnapshot snapshot) {
						if (!snapshot.hasChildren()) { callback.onError("Expense not found"); return; }
						for (DataSnapshot child : snapshot.getChildren()) {
							child.getRef().removeValue((error, ref) -> {
								if (error != null) callback.onError(error.getMessage()); else callback.onSuccess();
							});
							break;
						}
					}
					@Override public void onCancelled(@NonNull DatabaseError error) { callback.onError(error.getMessage()); }
				});
	}

	public void fetchExpenses(@NonNull ExpensesCallback callback) {
		userExpensesRef().addListenerForSingleValueEvent(new ValueEventListener() {
			@Override public void onDataChange(@NonNull DataSnapshot snapshot) {
				List<Expense> list = new ArrayList<>();
				for (DataSnapshot child : snapshot.getChildren()) {
					String id = asString(child.child("id").getValue());
					String name = asString(child.child("name").getValue());
					Double amount = asNumber(child.child("amount").getValue());
					String categoryStr = asString(child.child("category").getValue());
					String date = asString(child.child("date").getValue());
					String notes = asString(child.child("notes").getValue());

					Category category = Category.OTHER;
					try { if (categoryStr != null) category = Category.valueOf(categoryStr); } catch (IllegalArgumentException ignored) {}

					if (name != null && amount != null && date != null) {
						Expense e = new Expense(name, amount, category, date, notes != null ? notes : "");
						e.setId(id);
						list.add(e);
					}
				}
				// Sort by date desc; support yyyy-MM-dd and MM/dd/yyyy
				Collections.sort(list, new Comparator<Expense>() {
					final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
					final SimpleDateFormat us = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
					private long toMillis(String d) {
						try { return iso.parse(d).getTime(); } catch (ParseException e) {
							try { return us.parse(d).getTime(); } catch (ParseException ex) { return 0L; }
						}
					}
					@Override public int compare(Expense a, Expense b) { return Long.compare(toMillis(b.getDate()), toMillis(a.getDate())); }
				});
				callback.onSuccess(list);
			}
			@Override public void onCancelled(@NonNull DatabaseError error) { callback.onError(error.getMessage()); }
		});
	}

	public void seedIfEmpty(@NonNull RepoCallback callback) {
		userExpensesRef().limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.hasChildren()) { callback.onSuccess(); return; }
				List<Expense> seeds = new ArrayList<>();
				seeds.add(new Expense("Coffee", 3.75, Category.FOOD, "2025-10-01", "Latte"));
				seeds.add(new Expense("MARTA", 2.50, Category.TRANSPORT, "2025-10-03", "One-way"));
				final int total = seeds.size();
				final int[] done = {0};
				final boolean[] failed = {false};
				for (Expense e : seeds) {
					addExpense(e, new RepoCallback() {
						@Override public void onSuccess() { if (failed[0]) return; if (++done[0] == total) callback.onSuccess(); }
						@Override public void onError(String error) { if (!failed[0]) { failed[0] = true; callback.onError(error); } }
					});
				}
			}
			@Override public void onCancelled(@NonNull DatabaseError error) { callback.onError(error.getMessage()); }
		});
	}

	private static String asString(Object v) { return v == null ? null : String.valueOf(v); }
	private static Double asNumber(Object v) { return v instanceof Number ? ((Number)v).doubleValue() : null; }

	public interface RepoCallback { void onSuccess(); void onError(String error); }
	public interface ExpensesCallback { void onSuccess(List<Expense> expenses); void onError(String error); }
}
