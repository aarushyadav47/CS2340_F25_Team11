package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spendwise.model.Category;
import com.example.spendwise.model.Expense;
import com.example.spendwise.repo.ExpenseRepo;

import java.util.List;

public class ExpenseViewModel extends ViewModel {
    private final ExpenseRepo repo = ExpenseRepo.getInstance();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> expenses = new MutableLiveData<>();

    public ExpenseViewModel() {
        refreshExpenses(true);
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<List<Expense>> getExpenses() {
        return expenses;
    }

    public void addExpense(String name, double amount, Category category, String date, String notes) {
        Expense expense = new Expense(name, amount, category, date, notes);
        repo.addExpense(expense, new ExpenseRepo.RepoCallback() {
            @Override
            public void onSuccess() {
                statusMessage.postValue("Expense saved");
                refreshExpenses(false);
            }

            @Override
            public void onError(String error) {
                statusMessage.postValue(error);
            }
        });
    }

    public void deleteExpense(String expenseId) {
        repo.deleteExpense(expenseId, new ExpenseRepo.RepoCallback() {
            @Override
            public void onSuccess() {
                statusMessage.postValue("Expense deleted");
                refreshExpenses(false);
            }

            @Override
            public void onError(String error) {
                statusMessage.postValue(error);
            }
        });
    }

    public void refreshExpenses(boolean withSeed) {
        if (withSeed) {
            repo.seedIfEmpty(new ExpenseRepo.RepoCallback() {
                @Override
                public void onSuccess() {
                    loadExpenses();
                }

                @Override
                public void onError(String error) {
                    loadExpenses();
                }
            });
        } else {
            loadExpenses();
        }
    }

    private void loadExpenses() {
        repo.fetchExpenses(new ExpenseRepo.ExpensesCallback() {
            @Override
            public void onSuccess(List<Expense> list) {
                expenses.postValue(list);
            }

            @Override
            public void onError(String error) {
                statusMessage.postValue(error);
            }
        });
    }
}
