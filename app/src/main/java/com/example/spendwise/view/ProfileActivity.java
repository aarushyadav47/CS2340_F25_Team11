package com.example.spendwise.view;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spendwise.R;
import com.example.spendwise.adapter.FriendAdapter;
import com.example.spendwise.viewModel.ProfileViewModel;

public class ProfileActivity extends AppCompatActivity {

    private ProfileViewModel viewModel;
    private ImageView profileImage;
    private TextView userNameText;
    private TextView userEmailText;
    private TextView totalExpensesText;
    private TextView totalBudgetsText;
    private EditText friendEmailInput;
    private FriendAdapter friendAdapter;

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    viewModel.uploadProfileImage(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.example.spendwise.util.ThemeHelper.loadThemeFromFirebase(this);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initViews();
        setupObservers();
        setupListeners();

        viewModel.loadUserProfile();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        userNameText = findViewById(R.id.user_name);
        userEmailText = findViewById(R.id.user_email);
        totalExpensesText = findViewById(R.id.total_expenses_text);
        totalBudgetsText = findViewById(R.id.total_budgets_text);
        friendEmailInput = findViewById(R.id.friend_email_input);
        
        RecyclerView friendsRecyclerView = findViewById(R.id.friends_recycler_view);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendAdapter = new FriendAdapter();
        friendsRecyclerView.setAdapter(friendAdapter);
    }

    private void setupObservers() {
        viewModel.getUserProfile().observe(this, user -> {
            if (user != null) {
                userNameText.setText(user.getName());
                userEmailText.setText(user.getEmail());
                
                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                    Glide.with(this)
                            .load(user.getProfileImageUrl())
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .into(profileImage);
                }
            }
        });

        viewModel.getStatusMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getExpensesCount().observe(this, count -> 
            totalExpensesText.setText("Expenses: " + count));

        viewModel.getBudgetsCount().observe(this, count -> 
            totalBudgetsText.setText("Budgets: " + count));

        viewModel.getFriendEmails().observe(this, emails -> 
            friendAdapter.setFriendEmails(emails));
    }

    private void setupListeners() {
        findViewById(R.id.upload_image_button).setOnClickListener(v -> 
            pickImage.launch("image/*"));

        findViewById(R.id.add_friend_button).setOnClickListener(v -> {
            String email = friendEmailInput.getText().toString().trim();
            if (!email.isEmpty()) {
                viewModel.addFriend(email);
                friendEmailInput.setText("");
            }
        });

        friendAdapter.setOnRemoveClickListener(email -> viewModel.removeFriend(email));
    }
}
