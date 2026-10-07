package com.inter.efficientia_mobile;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Keeps the focused form field visible above the software keyboard. */
public final class FormKeyboardInsets {
    private FormKeyboardInsets() { }

    public static void install(Activity activity, View root, boolean edgeToEdge) {
        Runnable revealFocusedField = () -> {
            View focused = activity.getCurrentFocus();
            if (focused == null || !isDescendantOf(focused, root)) return;
            Rect field = new Rect();
            focused.getDrawingRect(field);
            field.bottom += (int) (24 * root.getResources().getDisplayMetrics().density);
            focused.requestRectangleOnScreen(field, false);
        };

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            if (edgeToEdge) {
                Insets occupied = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                view.setPadding(occupied.left, occupied.top, occupied.right, occupied.bottom);
            }
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                view.removeCallbacks(revealFocusedField);
                view.post(revealFocusedField);
            }
            return insets;
        });

        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
            if (newFocus != null && insets != null
                    && insets.isVisible(WindowInsetsCompat.Type.ime())) {
                root.postDelayed(revealFocusedField, 100);
            }
        });
    }

    private static boolean isDescendantOf(View view, View root) {
        if (view == root) return true;
        ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent == root) return true;
            parent = parent.getParent();
        }
        return false;
    }
}
