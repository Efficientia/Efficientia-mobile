package com.inter.efficientia_mobile;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.style.ScaleXSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.view.WindowCompat;

import com.inter.efficientia_mobile.main.DriverPreferences;

import java.util.WeakHashMap;

/** Applies device accessibility preferences to every app screen when it becomes visible. */
public final class AppDisplayPreferences {
    private static final int NAVY = 0xFF00183F;
    private static final int DARK_SURFACE = 0xFF082856;
    private static final int TEXT_SOFT = 0xFF30343B;
    private static final int[] READING_BACKGROUNDS = {0xFFFFF8E9, 0xFFF0F2F4, 0xFFFFF7D9};
    private static final WeakHashMap<TextView, Float> ORIGINAL_SIZES = new WeakHashMap<>();
    private static final WeakHashMap<TextView, ColorStateList> ORIGINAL_COLORS = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Typeface> ORIGINAL_TYPEFACES = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Float> ORIGINAL_LETTER_SPACING = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Float> ORIGINAL_LINE_MULTIPLIER = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Float> ORIGINAL_LINE_EXTRA = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Integer> ORIGINAL_MAX_WIDTH = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Integer> ORIGINAL_ALIGNMENT = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Boolean> WIDTH_MODIFIED = new WeakHashMap<>();
    private static final WeakHashMap<TextView, CharSequence> ORIGINAL_WORDS = new WeakHashMap<>();
    private static final WeakHashMap<TextView, CharSequence> SPACED_WORDS = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Boolean> SPEECH_ATTACHED = new WeakHashMap<>();
    private static final WeakHashMap<View, Drawable> ORIGINAL_BACKGROUNDS = new WeakHashMap<>();

    private AppDisplayPreferences() { }

    public static void apply(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        boolean dark = DriverPreferences.darkBlue(activity);
        boolean highContrast = DriverPreferences.highContrast(activity);
        boolean dyslexiaReading = DriverPreferences.dyslexiaReading(activity);
        int readingBackground = READING_BACKGROUNDS[Math.max(0,
                Math.min(READING_BACKGROUNDS.length - 1,
                        DriverPreferences.dyslexiaBackground(activity)))];
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
        applyTree(decor, DriverPreferences.largeText(activity),
                dyslexiaReading, DriverPreferences.readAloud(activity), highContrast,
                dark, blueScreen, readingBackground, activity);
        // Apply last: the tree pass restores original drawables before recoloring child cards.
        // Doing this first would restore the root gradient and hide high-contrast mode.
        if (highContrast && screenRoot != null) {
            rememberBackground(screenRoot);
            screenRoot.setBackgroundColor(dark || blueScreen ? NAVY : Color.WHITE);
        } else if (dyslexiaReading && !dark && !blueScreen && screenRoot != null) {
            rememberBackground(screenRoot);
            screenRoot.setBackgroundColor(readingBackground);
        }
    }

    private static void applyTree(View view, boolean largeText, boolean dyslexiaReading,
                                  boolean readAloud, boolean highContrast,
                                  boolean darkBlue, boolean blueScreen, int readingBackground,
                                  Activity activity) {
        applySurface(view, dyslexiaReading, highContrast, darkBlue, readingBackground);
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            float originalSize = ORIGINAL_SIZES.computeIfAbsent(text, TextView::getTextSize);
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSize * (largeText ? 1.16f : 1f));
            Typeface originalTypeface = ORIGINAL_TYPEFACES.computeIfAbsent(text,
                    key -> key.getTypeface() == null ? Typeface.DEFAULT : key.getTypeface());
            float originalLetterSpacing = ORIGINAL_LETTER_SPACING.computeIfAbsent(text,
                    TextView::getLetterSpacing);
            float originalLineMultiplier = ORIGINAL_LINE_MULTIPLIER.computeIfAbsent(text,
                    TextView::getLineSpacingMultiplier);
            float originalLineExtra = ORIGINAL_LINE_EXTRA.computeIfAbsent(text,
                    TextView::getLineSpacingExtra);
            if (dyslexiaReading) {
                text.setTypeface(Typeface.create("sans-serif", originalTypeface.getStyle()));
                text.setLetterSpacing(originalLetterSpacing + 0.06f);
                text.setLineSpacing(originalLineExtra, Math.max(1.5f, originalLineMultiplier));
            } else {
                text.setTypeface(originalTypeface);
                text.setLetterSpacing(originalLetterSpacing);
                text.setLineSpacing(originalLineExtra, originalLineMultiplier);
            }
            applyReadingLayout(text, dyslexiaReading);
            applySpeech(text, readAloud, activity);
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
                } else if (dyslexiaReading && !darkBlue && !highContrast
                        && brightness(originalColor) < 150 && !hasDarkSurface(text)) {
                    text.setTextColor(TEXT_SOFT);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTree(group.getChildAt(i), largeText, dyslexiaReading, readAloud,
                        highContrast, darkBlue, blueScreen, readingBackground, activity);
            }
        }
    }

    private static void applySurface(View view, boolean dyslexiaReading, boolean highContrast,
                                     boolean darkBlue, int readingBackground) {
        if (view instanceof Button) return;
        Drawable original = ORIGINAL_BACKGROUNDS.get(view);
        if (original != null && view.getBackground() != original) view.setBackground(original);
        if (!(view.getBackground() instanceof GradientDrawable)) return;
        GradientDrawable drawable = (GradientDrawable) view.getBackground();
        ColorStateList fill = drawable.getColor();
        if (fill == null || brightness(fill.getDefaultColor()) <= 210) return;
        if (!highContrast && !darkBlue && !dyslexiaReading) return;
        rememberBackground(view);
        Drawable.ConstantState state = drawable.getConstantState();
        if (state == null) return;
        GradientDrawable adjusted = (GradientDrawable) state.newDrawable().mutate();
        if (highContrast) {
            adjusted.setColor(darkBlue ? DARK_SURFACE : Color.WHITE);
            adjusted.setStroke(dp(view, 2), darkBlue ? Color.WHITE : NAVY);
        } else if (darkBlue) {
            adjusted.setColor(0xFF153B73);
        } else {
            adjusted.setColor(readingBackground);
            adjusted.setStroke(dp(view, 1), 0xFFD7D1C3);
        }
        view.setBackground(adjusted);
    }

    private static void applyReadingLayout(TextView text, boolean enabled) {
        if (text instanceof Button || text instanceof EditText || text.getMovementMethod() != null) return;
        CharSequence current = text.getText();
        CharSequence lastSpaced = SPACED_WORDS.get(text);
        boolean stillSpaced = lastSpaced != null
                && current.toString().contentEquals(lastSpaced);
        if (!stillSpaced) ORIGINAL_WORDS.put(text, current);
        CharSequence original = ORIGINAL_WORDS.get(text);
        if (enabled && original != null && original.length() > 0) {
            SpannableStringBuilder spaced = new SpannableStringBuilder(original);
            for (int index = 0; index < spaced.length(); index++) {
                if (spaced.charAt(index) == ' ')
                    spaced.setSpan(new ScaleXSpan(1.45f), index, index + 1, 0);
            }
            SPACED_WORDS.put(text, spaced);
            text.setText(spaced);
        } else if (!enabled && stillSpaced && original != null) {
            text.setText(original);
            SPACED_WORDS.remove(text);
        }
        if (enabled && original != null && original.length() > 70) {
            int originalWidth = ORIGINAL_MAX_WIDTH.computeIfAbsent(text, TextView::getMaxWidth);
            ORIGINAL_ALIGNMENT.putIfAbsent(text, text.getTextAlignment());
            text.setMaxWidth(Math.min(originalWidth, Math.round(text.getTextSize() * 36f)));
            text.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            WIDTH_MODIFIED.put(text, true);
        } else if (Boolean.TRUE.equals(WIDTH_MODIFIED.get(text))) {
            text.setMaxWidth(ORIGINAL_MAX_WIDTH.get(text));
            text.setTextAlignment(ORIGINAL_ALIGNMENT.get(text));
            WIDTH_MODIFIED.remove(text);
        }
    }

    private static void applySpeech(TextView text, boolean enabled, Activity activity) {
        if (text instanceof Button || text instanceof EditText || text.isClickable()) return;
        if (enabled && !Boolean.TRUE.equals(SPEECH_ATTACHED.get(text)) && !text.isLongClickable()) {
            text.setOnLongClickListener(view -> {
                ((EfficientiaApplication) activity.getApplication()).speak(
                        text.getText().toString(),
                        activity.getResources().getConfiguration().getLocales().get(0));
                return true;
            });
            SPEECH_ATTACHED.put(text, true);
        } else if (!enabled && Boolean.TRUE.equals(SPEECH_ATTACHED.get(text))) {
            text.setOnLongClickListener(null);
            text.setLongClickable(false);
            SPEECH_ATTACHED.remove(text);
        }
    }

    private static boolean hasDarkSurface(View view) {
        View parent = view;
        while (parent != null) {
            Drawable background = parent.getBackground();
            if (background instanceof GradientDrawable) {
                ColorStateList color = ((GradientDrawable) background).getColor();
                if (color != null) return brightness(color.getDefaultColor()) < 130;
            }
            if (!(parent.getParent() instanceof View)) break;
            parent = (View) parent.getParent();
        }
        return false;
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
