package com.example.spendwise.view;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.example.spendwise.R;
import com.example.spendwise.databinding.ActivityMainBinding;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Using data binding to inflate the layout(no explicit mention)
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        FirebaseApp.initializeApp(this);

        //Binding viewmodel to the layout
        // binding.setVariable(BR.viewModel, viewModel);
        // binding.setLifecycleOwner(this);
        // the viewmodel is binded by this file - not destroyed by rotations

        // Force status bar color (the purple bar at top)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF6200EE); // Purple
            // Or use: getWindow().setStatusBarColor(Color.parseColor("#6200EE"));
        }

        Button openBtn = findViewById(R.id.start_button);
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
            }
        });

        Button quit = findViewById(R.id.quit_button);
        quit.setOnClickListener(v -> {
            finishAffinity(); // closesall activities
            System.exit(0);   // app process halted
        });

        Button dashboard = findViewById(R.id.dashboard_link_button);
        dashboard.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, Dashboard.class)));

    }
}