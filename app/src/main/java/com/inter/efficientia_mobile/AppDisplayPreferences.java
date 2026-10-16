package com.inter.efficientia_mobile;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.view.WindowCompat;

import com.inter.efficientia_mobile.main.DriverPreferences;

import java.util.WeakHashMap;

/** Applies device accessibility preferences to every app screen when it becomes visible. */
public final class AppDisplayPreferences {
    private static final int NAVY = 0xFF00183F;
    private static final int DARK_SURFACE = 0xFF082856;
    private static final WeakHashMap<TextView, Float> ORIGINAL_SIZES = new WeakHashMap<>();
    private static final WeakHashMap<TextView, ColorStateList> ORIGINAL_COLORS = new WeakHashMap<>();
    private static final WeakHashMap<View, Drawable> ORIGINAL_BACKGROUNDS = new WeakHashMap<>();

    private AppDisplayPreferences() { }

    public static void apply(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        boolean dark = DriverPreferences.darkBlue(activity);
        boolean highContrast = DriverPreferences.highContrast(activity);
        String screen = activity.getClass().getName();
        boolean blueScreen = screen.contains(".route.") || screen.contains(".auth.")
                || screen.contains(".signature.") || screen.contains(".splash.");
        if (dark || highContrast) {
            boolean lightBars = highContrast && !dark && !blueScreen;
            WindowCompat.getInsetsController(activity.getWindow(), decor)
                    .setAppearanceLightStatusBars(lightBars);
            WindowCompat.getInsetsController(activity.getWindow(), decor)
                    .setAppearanceLightNavigationBars(lightBars);
        }

        ViewGroup content = activity.findViewById(android.R.id.content);
        View screenRoot = content != null && content.getChildCount() > 0
                ? content.getChildAt(0) : null;
        if (!highContrast && screenRoot != null) restoreBackground(screenRoot);
        applyTree(decor, DriverPreferences.largeText(activity), highContrast, dark, blueScreen);
        // Apply last: the tree pass restores original drawables before recoloring child cards.
        // Doing this first would restore the root gradient and hide high-contrast mode.
        if (highContrast && screenRoot != null) {
            rememberBackground(screenRoot);
            screenRoot.setBackgroundColor(dark || blueScreen ? NAVY : Color.WHITE);
        }
    }

    private static void applyTree(View view, boolean largeText, boolean highContrast,
                                  boolean darkBlue, boolean blueScreen) {
        applySurface(view, highContrast, darkBlue);
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            float originalSize = ORIGINAL_SIZES.computeIfAbsent(text, TextView::getTextSize);
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSize * (largeText ? 1.16f : 1f));
            ColorStateList original = ORIGINAL_COLORS.computeIfAbsent(text, TextView::getTextColors);
            text.setTextColor(original);
            int originalColor = original.getDefaultColor();
            if (Color.alpha(originalColor) >= 16 && !(text instanceof Button)) {
                if (highContrast) {
                    if (darkBlue || (blueScreen && !hasLightSurface(text))) {
                        text.setTextColor(Color.WHITE);
                    } else if (brightness(originalColor) < 215 || hasLightSurface(text)) {
                        text.setTextColor(NAVY);
                    }
                } else if (darkBlue && brightness(originalColor) < 185) {
                    text.setTextColor(Color.WHITE);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTree(group.getChildAt(i), largeText, highContrast, darkBlue, blueScreen);
            }
        }
    }

    private static void applySurface(View view, boolean highContrast, boolean darkBlue) {
        if (view instanceof Button) return;
        Drawable original = ORIGINAL_BACKGROUNDS.get(view);
        if (original != null && view.getBackground() != original) view.setBackground(original);
        if (!(view.getBackground() instanceof GradientDrawable)) return;
        GradientDrawable drawable = (GradientDrawable) view.getBackground();
        ColorStateList fill = drawable.getColor();
        if (fill == null || brightness(fill.getDefaultColor()) <= 210) return;
        if (!highContrast && !darkBlue) return;
        rememberBackground(view);
        Drawable.ConstantState state = drawable.getConstantState();
        if (state == null) return;
        GradientDrawable adjusted = (GradientDrawable) state.newDrawable().mutate();
        if (highContrast) {
            adjusted.setColor(darkBlue ? DARK_SURFACE : Color.WHITE);
            adjusted.setStroke(dp(view, 2), darkBlue ? Color.WHITE : NAVY);
        } else {
            adjusted.setColor(0xFF153B73);
        }
        view.setBackground(adjusted);
    }

    private static boolean hasLightSurface(View view) {
        View parent = view;
        while (parent != null) {
            Drawable background = parent.getBackground();
            if (background instanceof GradientDrawable) {
                ColorStateList color = ((GradientDrawable) background).getColor();
                if (color != null) return brightness(color.getDefaultColor()) > 175;
            }
            if (!(parent.getParent() instanceof View)) break;
            parent = (View) parent.getParent();
        }
        return false;
    }

    private static void rememberBackground(View view) {
        ORIGINAL_BACKGROUNDS.putIfAbsent(view, view.getBackground());
    }

    private static void restoreBackground(View view) {
        Drawable original = ORIGINAL_BACKGROUNDS.get(view);
        if (original != null) view.setBackground(original);
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static int brightness(int color) {
        return (int) (Color.red(color) * 0.2126 + Color.green(color) * 0.7152
                + Color.blue(color) * 0.0722);
    }
}
