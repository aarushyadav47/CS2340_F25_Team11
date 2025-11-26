package com.example.spendwise.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "theme_mode";

    // Theme modes
    public static final int LIGHT_MODE = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int DARK_MODE = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int SYSTEM_MODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    /**
     * Apply the saved theme
     */
    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int themeMode = prefs.getInt(KEY_THEME, SYSTEM_MODE);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    /**
     * Save and apply a new theme
     */
    public static void setTheme(Context context, int themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, themeMode).apply();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    /**
     * Get the current theme mode
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
}