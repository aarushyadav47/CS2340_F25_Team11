package com.example.spendwise.viewModel;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;

public class LoginViewModel extends ViewModel {

    private final FirebaseAuth mAuth;
    private final MutableLiveData<String> loginResult = new MutableLiveData<>();

    public LoginViewModel() {
        mAuth = FirebaseAuth.getInstance();
    }

    public LiveData<String> getLoginResult() {
        return loginResult;
    }

    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            loginResult.setValue("Please enter email and password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        loginResult.setValue("SUCCESS");
                    } else {
                        loginResult.setValue("Login failed: " + task.getException().getMessage());
                    }
                });
    }
}