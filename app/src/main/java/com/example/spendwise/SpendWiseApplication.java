
package com.example.spendwise;

import android.app.Application;
import com.example.spendwise.util.ThemeHelper;

public class SpendWiseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply the saved theme when app starts
        ThemeHelper.applyTheme(this);
    }
}