package com.example.spendwise.view;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.example.spendwise.databinding.LoginBinding;
import com.example.spendwise.R;

public class Login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Using data binding to inflate the layout(no explicit mention)
        LoginBinding binding = LoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Binding viewmodel to the layout
        // binding.setVariable(BR.viewModel, viewModel); Needed though
        binding.setLifecycleOwner(this); //the viewmodel is binded by this file - not destroyed by rotations

        Button login = findViewById(R.id.login_button);
        login.setOnClickListener(v -> {
            //Firebase Auth and stuff
        });

        Button openRegister = findViewById(R.id.open_register);
        openRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                Intent intent = new Intent(Login.this, Register.class);
                startActivity(intent);
            }
        });
    }
}
