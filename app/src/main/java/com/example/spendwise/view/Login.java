package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.spendwise.databinding.LoginBinding;
import com.example.spendwise.R;

import com.example.spendwise.viewModel.LoginViewModel;

public class Login extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private LoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = LoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding.setLifecycleOwner(this);


        loginViewModel.getLoginResult().observe(this, result -> {
            if ("SUCCESS".equals(result)) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Login.this, Dashboard.class));
                finish();
            } else {
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            }
        });

        // Login button click interaction with the user
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailField.getText().toString().trim();
            String password = binding.passwordField.getText().toString().trim();
            loginViewModel.login(email, password);
        });

        // Opens the Register screen
        binding.openRegister.setOnClickListener(v -> startActivity(new Intent(Login.this, Register.class)));
    }
}
