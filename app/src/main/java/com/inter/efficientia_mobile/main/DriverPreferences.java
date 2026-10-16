package com.inter.efficientia_mobile.main;

import android.content.Context;
import android.content.SharedPreferences;

/** Device preferences for the signed-in driver. */
public final class DriverPreferences {
    private static final String FILE = "driver_preferences";
    private static final String NOTIFICATIONS = "notifications_enabled";
    private static final String HIGH_CONTRAST = "high_contrast";
    private static final String DARK_BLUE = "dark_blue";
    private static final String LARGE_TEXT = "large_text";

    private DriverPreferences() { }

    public static boolean notificationsEnabled(Context context) {
        return preferences(context).getBoolean(NOTIFICATIONS, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(NOTIFICATIONS, enabled).apply();
    }

    public static boolean highContrast(Context context) {
        return preferences(context).getBoolean(HIGH_CONTRAST, false);
    }

    public static void setHighContrast(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(HIGH_CONTRAST, enabled).apply();
    }

    public static boolean darkBlue(Context context) {
        return preferences(context).getBoolean(DARK_BLUE, false);
    }

    public static void setDarkBlue(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(DARK_BLUE, enabled).apply();
    }

    public static boolean largeText(Context context) {
        return preferences(context).getBoolean(LARGE_TEXT, false);
    }

    public static void setLargeText(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(LARGE_TEXT, enabled).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
