package com.inter.efficientia_mobile;

import android.app.Activity;
import android.app.AlertDialog;

/** Keeps diagnostics available without exposing them in the main error message. */
public final class TechnicalDetails {
    private TechnicalDetails() { }

    public static void show(Activity activity, String details) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.technical_details)
                .setMessage(details)
                .setPositiveButton(R.string.dialog_understood, null)
                .show();
    }

    public static void showError(Activity activity, String message, String details) {
        new AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_understood, null)
                .setNeutralButton(R.string.technical_details,
                        (dialog, which) -> show(activity, details))
                .show();
    }
}
