package com.example.spendwise.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.spendwise.databinding.LoginBinding;
import com.example.spendwise.util.ThemeHelper;
import com.example.spendwise.viewModel.LoginViewModel;

public class Login extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private LoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setContentView
        ThemeHelper.applyTheme(this);

        super.onCreate(savedInstanceState);

        binding = LoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding.setLifecycleOwner(this);

        // Force status bar color (the purple bar at top)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF6200EE); // Purple
            // Or use: getWindow().setStatusBarColor(Color.parseColor("#6200EE"));
        }

        loginViewModel.getLoginResult().observe(this, result -> {
            if ("SUCCESS".equals(result)) {
                // Load theme preference from Firebase after successful login
                ThemeHelper.loadThemeFromFirebase(this);

                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                // Small delay to allow theme to load from Firebase
                binding.getRoot().postDelayed(() -> {
                    Intent intent = new Intent(Login.this, Dashboard.class);
                    intent.putExtra("from_login", true);
                    startActivity(intent);
                    finish();
                }, 300); // 300ms delay

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
        binding.openRegister.setOnClickListener(v -> startActivity(
                new Intent(Login.this, Register.class)));
    }
}