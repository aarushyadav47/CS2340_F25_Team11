package com.example.spendwise.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.spendwise.R;
import com.example.spendwise.util.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

/**
 * Profile Activity for managing user info, photo, and theme settings
 * Extra credit: View/manage info + photo + friend system integration
 */
public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private EditText nameEditText;
    private EditText emailEditText;
    private ImageView profileImageView;
    private RadioGroup themeRadioGroup;
    private RadioButton lightRadio;
    private RadioButton darkRadio;
    private RadioButton systemRadio;
    private Button saveButton;
    private Button friendsButton;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load theme before setting content
        ThemeManager.loadTheme(this);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeViews();
        loadUserProfile();
        setupThemeControls();
        setupClickListeners();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.profile_name_edit);
        emailEditText = findViewById(R.id.profile_email_edit);
        profileImageView = findViewById(R.id.profile_image_view);
        themeRadioGroup = findViewById(R.id.theme_radio_group);
        lightRadio = findViewById(R.id.theme_light_radio);
        darkRadio = findViewById(R.id.theme_dark_radio);
        systemRadio = findViewById(R.id.theme_system_radio);
        saveButton = findViewById(R.id.profile_save_button);
        friendsButton = findViewById(R.id.profile_friends_button);
    }

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load basic info
        if (user.getDisplayName() != null) {
            nameEditText.setText(user.getDisplayName());
        }
        emailEditText.setText(user.getEmail());

        // Load profile photo from Firebase Storage
        loadProfilePhoto(user.getUid());

        // Load user settings from Firebase
        DatabaseReference userRef = database.getReference("users")
                .child(user.getUid());
        
        userRef.child("name").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String name = task.getResult().getValue(String.class);
                if (name != null && !name.isEmpty()) {
                    nameEditText.setText(name);
                }
            }
        });

        userRef.child("settings").child("themeMode").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String themeMode = task.getResult().getValue(String.class);
                if (themeMode != null) {
                    selectThemeRadio(ThemeManager.ThemeMode.fromString(themeMode));
                } else {
                    selectThemeRadio(ThemeManager.getTheme(this));
                }
            } else {
                selectThemeRadio(ThemeManager.getTheme(this));
            }
        });
    }

    private void loadProfilePhoto(String uid) {
        StorageReference photoRef = storage.getReference()
                .child("profile_photos")
                .child(uid + ".jpg");

        photoRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_add)
                    .into(profileImageView);
        }).addOnFailureListener(e -> {
            Log.d(TAG, "No profile photo found, using default");
            profileImageView.setImageResource(R.drawable.ic_person_add);
        });
    }

    private void setupThemeControls() {
        ThemeManager.ThemeMode currentTheme = ThemeManager.getTheme(this);
        selectThemeRadio(currentTheme);

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            ThemeManager.ThemeMode selectedTheme;
            if (checkedId == R.id.theme_light_radio) {
                selectedTheme = ThemeManager.ThemeMode.LIGHT;
            } else if (checkedId == R.id.theme_dark_radio) {
                selectedTheme = ThemeManager.ThemeMode.DARK;
            } else {
                selectedTheme = ThemeManager.ThemeMode.SYSTEM;
            }
            ThemeManager.setTheme(this, selectedTheme);
        });
    }

    private void selectThemeRadio(ThemeManager.ThemeMode theme) {
        switch (theme) {
            case LIGHT:
                lightRadio.setChecked(true);
                break;
            case DARK:
                darkRadio.setChecked(true);
                break;
            case SYSTEM:
            default:
                systemRadio.setChecked(true);
                break;
        }
    }

    private void setupClickListeners() {
        profileImageView.setOnClickListener(v -> openImagePicker());
        
        saveButton.setOnClickListener(v -> saveProfile());
        
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, FriendsActivity.class);
            startActivity(intent);
        });

        // Back button
        findViewById(R.id.profile_back_button).setOnClickListener(v -> finish());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .circleCrop()
                        .into(profileImageView);
            }
        }
    }

    private void saveProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }

        String name = nameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name cannot be empty");
            return;
        }

        String uid = user.getUid();
        DatabaseReference userRef = database.getReference("users").child(uid);
        
        // Save name
        userRef.child("name").setValue(name);

        // Upload profile photo if selected
        if (selectedImageUri != null) {
            uploadProfilePhoto(uid, selectedImageUri);
        }

        Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void uploadProfilePhoto(String uid, Uri imageUri) {
        StorageReference photoRef = storage.getReference()
                .child("profile_photos")
                .child(uid + ".jpg");

        photoRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Profile photo uploaded successfully");
                    loadProfilePhoto(uid); // Reload to show new photo
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload profile photo", e);
                    Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show();
                });
    }
}

