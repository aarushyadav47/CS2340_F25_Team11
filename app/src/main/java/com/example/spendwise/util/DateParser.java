package com.example.spendwise.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for date parsing
 * Fixes code smell: Duplicate Code
 */
public class DateParser {
    private static final String TAG = "DateParser";
    private static final String DATE_PATTERN = "MM/dd/yyyy";
    private static final SimpleDateFormat dateFormat = 
        new SimpleDateFormat(DATE_PATTERN, Locale.US);

    private DateParser() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parse date string to timestamp, handling nulls gracefully
     */
    public static Long parseToTimestamp(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            Date date = dateFormat.parse(dateStr);
            return date != null ? date.getTime() : null;
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + dateStr, e);
            return null;
        }
    }

    /**
     * Get the date format instance
     */
    public static SimpleDateFormat getDateFormat() {
        return (SimpleDateFormat) dateFormat.clone();
    }
}

