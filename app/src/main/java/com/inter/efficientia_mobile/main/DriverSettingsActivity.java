package com.inter.efficientia_mobile.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.splash.MainSplashActivity;

public class DriverSettingsActivity extends AppCompatActivity {
    private static final String[] LANGUAGE_TAGS = {"pt-BR", "en", "es", "fr", "zh-CN", "ja"};
    private static final int[] LANGUAGE_LABELS = {R.string.settings_portuguese,
            R.string.settings_english, R.string.settings_spanish, R.string.settings_french,
            R.string.settings_chinese, R.string.settings_japanese};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SessionManager.authorizationHeader(this) == null) {
            Intent start = new Intent(this, MainSplashActivity.class);
            start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(start);
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(!DriverPreferences.darkBlue(this));
        setContentView(R.layout.activity_driver_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        findViewById(R.id.btnSettingsTopHome).setOnClickListener(view -> goHome());
        findViewById(R.id.navSettingsHome).setOnClickListener(view -> goHome());
        findViewById(R.id.btnSettingsNotifications).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverNotificationsActivity.class));
        findViewById(R.id.imgSettingsProfile).setOnClickListener(view -> openProfile());
        findViewById(R.id.navSettingsProfile).setOnClickListener(view -> openProfile());
        findViewById(R.id.navSettingsDocuments).setOnClickListener(view ->
                ToolbarNavigation.open(this, DocumentsActivity.class));
        findViewById(R.id.navSettingsAssistant).setOnClickListener(view ->
                ToolbarNavigation.open(this, CinttiaActivity.class));

        bindOption(R.id.settingNotifications, R.string.settings_notifications,
                DriverPreferences.notificationsEnabled(this) ? R.string.settings_enabled : R.string.settings_disabled,
                view -> showNotificationOptions());
        bindOption(R.id.settingLanguage, R.string.settings_language,
                LANGUAGE_LABELS[currentLanguageIndex()],
                view -> showLanguageOptions());
        bindOption(R.id.settingPrivacy, R.string.settings_privacy,
                R.string.settings_permissions, view -> showPrivacyOptions());
        bindOption(R.id.settingAbout, R.string.settings_about,
                R.string.settings_about, view -> showVersion());
        bindOption(R.id.settingAccessibility, R.string.settings_accessibility,
                R.string.settings_accessibility_summary, view -> showAccessibilityOptions());
        updateAboutSubtitle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImageView photo = findViewById(R.id.imgSettingsProfile);
        if (photo != null) ProfilePhotoStore.show(this, photo);
        updateSubtitle(R.id.settingNotifications,
                DriverPreferences.notificationsEnabled(this)
                        ? R.string.settings_enabled : R.string.settings_disabled);
    }

    private void bindOption(int id, int title, int subtitle, View.OnClickListener listener) {
        View option = findViewById(id);
        ((TextView) option.findViewById(R.id.settingsOptionTitle)).setText(title);
        ((TextView) option.findViewById(R.id.settingsOptionSubtitle)).setText(subtitle);
        option.setOnClickListener(listener);
    }

    private void updateSubtitle(int id, int subtitle) {
        View option = findViewById(id);
        ((TextView) option.findViewById(R.id.settingsOptionSubtitle)).setText(subtitle);
    }

    private void showNotificationOptions() {
        boolean enabled = DriverPreferences.notificationsEnabled(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_notifications)
                .setMessage(R.string.settings_notifications_detail)
                .setSingleChoiceItems(new String[]{getString(R.string.settings_enabled),
                        getString(R.string.settings_disabled)}, enabled ? 0 : 1,
                        (dialog, choice) -> {
                            DriverPreferences.setNotificationsEnabled(this, choice == 0);
                            updateSubtitle(R.id.settingNotifications,
                                    choice == 0 ? R.string.settings_enabled : R.string.settings_disabled);
                            dialog.dismiss();
                        })
                .setNegativeButton(R.string.settings_close, null)
                .show();
    }

    private void showLanguageOptions() {
        String[] labels = new String[LANGUAGE_LABELS.length];
        for (int i = 0; i < labels.length; i++) labels[i] = getString(LANGUAGE_LABELS[i]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_language)
                .setSingleChoiceItems(labels, currentLanguageIndex(),
                        (dialog, choice) -> {
                            dialog.dismiss();
                            AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(LANGUAGE_TAGS[choice]));
                        })
                .setNegativeButton(R.string.settings_close, null)
                .show();
    }

    private int currentLanguageIndex() {
        String language = getResources().getConfiguration().getLocales().get(0).getLanguage();
        for (int i = 0; i < LANGUAGE_TAGS.length; i++) {
            if (LANGUAGE_TAGS[i].startsWith(language)) return i;
        }
        return 0;
    }

    private void showPrivacyOptions() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_privacy)
                .setMessage(R.string.settings_permissions_detail)
                .setPositiveButton(R.string.settings_open_permissions, (dialog, which) -> {
                    Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(settings);
                })
                .setNegativeButton(R.string.settings_close, null)
                .show();
    }

    private void showVersion() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_about)
                .setMessage(getString(R.string.settings_version, versionName()))
                .setPositiveButton(R.string.settings_close, null)
                .show();
    }

    private void updateAboutSubtitle() {
        ((TextView) findViewById(R.id.settingAbout).findViewById(R.id.settingsOptionSubtitle))
                .setText(getString(R.string.settings_version, versionName()));
    }

    private String versionName() {
        try {
            String name;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                name = getPackageManager().getPackageInfo(getPackageName(),
                        PackageManager.PackageInfoFlags.of(0)).versionName;
            } else {
                name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            }
            return name == null ? "" : name;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    private void showAccessibilityOptions() {
        boolean[] selected = {DriverPreferences.highContrast(this), DriverPreferences.darkBlue(this),
                DriverPreferences.largeText(this)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_accessibility)
                .setMultiChoiceItems(new String[]{getString(R.string.settings_high_contrast),
                        getString(R.string.settings_dark_blue), getString(R.string.settings_large_text)},
                        selected, (dialog, index, checked) -> selected[index] = checked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    boolean nightChanged = DriverPreferences.darkBlue(this) != selected[1];
                    DriverPreferences.setHighContrast(this, selected[0]);
                    DriverPreferences.setDarkBlue(this, selected[1]);
                    DriverPreferences.setLargeText(this, selected[2]);
                    AppCompatDelegate.setDefaultNightMode(selected[1]
                            ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                    if (!nightChanged) recreate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void goHome() {
        ToolbarNavigation.home(this);
    }

    private void openProfile() {
        ToolbarNavigation.open(this, DriverProfileActivity.class);
    }

    private void showPendingFeature(View ignored) {
        Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show();
    }
}
