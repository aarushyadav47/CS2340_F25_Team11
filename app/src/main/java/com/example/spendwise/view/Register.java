package com.example.spendwise.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendwise.R;
import com.example.spendwise.databinding.RegisterBinding;

public class Register extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Using data binding to inflate the layout(no explicit mention)
        RegisterBinding binding = RegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Binding viewmodel to the layout
        // binding.setVariable(BR.viewModel, viewModel); Needed though once the right viewmodel is created
        binding.setLifecycleOwner(this); //the viewmodel is binded by this file - not destroyed by rotations

        Button register = findViewById(R.id.register_button);
        register.setOnClickListener(v -> {
            //Firebase Auth and stuff
        });

        Button openLogin = findViewById(R.id.open_login);
        openLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                Intent intent = new Intent(Register.this, Login.class);
                startActivity(intent);
            }
        });
    }
}