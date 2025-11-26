package com.example.spendwise.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class ThemeHelper {
    private static final String TAG = "ThemeHelper";
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String FIREBASE_THEME_PATH = "users/%s/preferences/theme";

    // Theme modes
    public static final int LIGHT_MODE = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int DARK_MODE = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int SYSTEM_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    /**
     * Apply the saved theme from local SharedPreferences
     */
    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(KEY_THEME, SYSTEM_MODE);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    /**
     * Save and apply a new theme (saves both locally and to Firebase)
     */
    public static void setTheme(Context context, int themeMode) {
        // Save locally first for immediate application
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, themeMode).apply();
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Save to Firebase
        saveThemeToFirebase(themeMode);
    }

    /**
     * Save theme preference to Firebase Realtime Database
     */
    private static void saveThemeToFirebase(int themeMode) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference themeRef = FirebaseDatabase.getInstance()
                    .getReference(String.format(FIREBASE_THEME_PATH, userId));

            themeRef.setValue(themeMode)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Theme preference saved to Firebase: " + themeMode))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to save theme to Firebase", e));
        } else {
            Log.w(TAG, "No user logged in, theme not saved to Firebase");
        }
    }

    /**
     * Load theme preference from Firebase and apply it
     * Call this when user logs in or app starts
     */
    public static void loadThemeFromFirebase(Context context) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference themeRef = FirebaseDatabase.getInstance()
                    .getReference(String.format(FIREBASE_THEME_PATH, userId));

            themeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Integer themeMode = snapshot.getValue(Integer.class);
                        if (themeMode != null) {
                            // Save to local SharedPreferences
                            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            prefs.edit().putInt(KEY_THEME, themeMode).apply();

                            // Apply the theme
                            AppCompatDelegate.setDefaultNightMode(themeMode);
                            Log.d(TAG, "Theme loaded from Firebase: " + themeMode);
                        }
                    } else {
                        Log.d(TAG, "No theme preference found in Firebase, using default");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load theme from Firebase", error.toException());
                }
            });
        } else {
            Log.w(TAG, "No user logged in, cannot load theme from Firebase");
        }
    }

    /**
     * Sync theme preference from Firebase in real-time
     * Useful if user changes theme on another device
     */
    public static void syncThemeFromFirebase(Context context, ThemeSyncListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference themeRef = FirebaseDatabase.getInstance()
                    .getReference(String.format(FIREBASE_THEME_PATH, userId));

            themeRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Integer themeMode = snapshot.getValue(Integer.class);
                        if (themeMode != null) {
                            // Save to local SharedPreferences
                            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            int currentTheme = prefs.getInt(KEY_THEME, SYSTEM_MODE);

                            // Only update if different
                            if (currentTheme != themeMode) {
                                prefs.edit().putInt(KEY_THEME, themeMode).apply();
                                AppCompatDelegate.setDefaultNightMode(themeMode);

                                if (listener != null) {
                                    listener.onThemeChanged(themeMode);
                                }
                                Log.d(TAG, "Theme synced from Firebase: " + themeMode);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to sync theme from Firebase", error.toException());
                }
            });
        }
    }

    /**
     * Get the current theme mode from local storage
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, SYSTEM_MODE);
    }

    /**
     * Check if dark mode is currently active
     */
    public static boolean isDarkMode(Context context) {
        int mode = getThemeMode(context);
        if (mode == DARK_MODE) {
            return true;
        } else if (mode == LIGHT_MODE) {
            return false;
        } else {
            // Check system setting
            int nightMode = context.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
    }

    /**
     * Toggle between light and dark mode
     */
    public static void toggleTheme(Context context) {
        int currentMode = getThemeMode(context);
        int newMode = (currentMode == DARK_MODE) ? LIGHT_MODE : DARK_MODE;
        setTheme(context, newMode);
    }

    /**
     * Clear theme preference from Firebase (useful when user logs out)
     */
    public static void clearThemeFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference themeRef = FirebaseDatabase.getInstance()
                    .getReference(String.format(FIREBASE_THEME_PATH, userId));

            // Note: We don't actually delete it, just log that logout happened
            Log.d(TAG, "User logged out, theme preference remains in Firebase");
        }
    }

    /**
     * Interface for listening to theme changes from Firebase
     */
    public interface ThemeSyncListener {
        void onThemeChanged(int newThemeMode);
    }
}