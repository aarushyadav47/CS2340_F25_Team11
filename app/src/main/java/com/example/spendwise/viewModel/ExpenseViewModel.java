package com.example.spendwise.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;

public class ExpenseViewModel {
    private final MutableLiveData<String> logResult = new MutableLiveData<>();

    public ExpenseViewModel() {}

    public LiveData<String> getLoginResult() {
        return logResult;
    }

    public void addExpense(String name, String amount, String category, String date) {
        if (name.isEmpty() || amount == null ||date.isEmpty()) {
            logResult.setValue("Please enter valid data in the input fields");
            return;
        }

    }
}
