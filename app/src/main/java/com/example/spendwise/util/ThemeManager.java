package com.example.spendwise.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Manages app theme (Light/Dark mode) with SharedPreferences and Firebase persistence
 * Extra credit feature: Light/Dark mode with SharedPreferences + DB persistence
 */
public class ThemeManager {
    private static final String TAG = "ThemeManager";
    private static final String PREFS_NAME = "SpendWiseThemePrefs";
    private static final String KEY_THEME_MODE = "theme_mode";
    
    public enum ThemeMode {
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES),
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        private final int modeValue;
        
        ThemeMode(int modeValue) {
            this.modeValue = modeValue;
        }
        
        public int getModeValue() {
            return modeValue;
        }
        
        public static ThemeMode fromString(String value) {
            try {
                return valueOf(value);
            } catch (Exception e) {
                return SYSTEM;
            }
        }
    }

    /**
     * Load theme from SharedPreferences and apply it
     */
    public static void loadTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String themeString = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name());
        ThemeMode theme = ThemeMode.fromString(themeString);
        applyTheme(theme);
        
        // Also load from Firebase if user is logged in
        loadThemeFromFirebase(context);
    }

    /**
     * Set theme mode and persist to SharedPreferences and Firebase
     */
    public static void setTheme(Context context, ThemeMode theme) {
        // Save to SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME_MODE, theme.name()).apply();
        
        // Apply theme immediately
        applyTheme(theme);
        
        // Save to Firebase for persistence across devices
        saveThemeToFirebase(context, theme);
    }

    /**
     * Get current theme mode from SharedPreferences
     */
    public static ThemeMode getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String themeString = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name());
        return ThemeMode.fromString(themeString);
    }

    /**
     * Apply theme mode
     */
    private static void applyTheme(ThemeMode theme) {
        AppCompatDelegate.setDefaultNightMode(theme.getModeValue());
        Log.d(TAG, "Applied theme: " + theme.name());
    }

    /**
     * Save theme to Firebase database
     */
    private static void saveThemeToFirebase(Context context, ThemeMode theme) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("settings");
        
        userRef.child("themeMode").setValue(theme.name())
                .addOnSuccessListener(aVoid -> 
                    Log.d(TAG, "Theme saved to Firebase: " + theme.name()))
                .addOnFailureListener(e -> 
                    Log.e(TAG, "Failed to save theme to Firebase", e));
    }

    /**
     * Load theme from Firebase and apply if different
     */
    private static void loadThemeFromFirebase(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("settings")
                .child("themeMode");
        
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getValue() != null) {
                String themeString = task.getResult().getValue().toString();
                ThemeMode firebaseTheme = ThemeMode.fromString(themeString);
                
                // Only update if different from current
                ThemeMode currentTheme = getTheme(context);
                if (firebaseTheme != currentTheme) {
                    setTheme(context, firebaseTheme);
                    Log.d(TAG, "Theme loaded from Firebase: " + firebaseTheme.name());
                }
            }
        });
    }
}

