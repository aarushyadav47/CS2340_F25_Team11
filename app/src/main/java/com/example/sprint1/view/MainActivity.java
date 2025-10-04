package com.example.sprint1.view;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.example.sprint1.R;
import com.example.sprint1.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Using data binding to inflate the layout(no explicit mention)
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Binding viewmodel to the layout
        // binding.setVariable(BR.viewModel, viewModel);
        // binding.setLifecycleOwner(this); //the viewmodel is binded by this file - not destroyed by rotations

        Button openBtn = findViewById(R.id.start_button);
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
            }
        });

        Button quit = findViewById(R.id.quit_button);
        quit.setOnClickListener(v -> {
            finishAffinity(); // closesall activities
            System.exit(0);   // app process halted
        });
    }
}