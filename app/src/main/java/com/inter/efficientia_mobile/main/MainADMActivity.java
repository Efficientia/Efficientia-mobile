package com.inter.efficientia_mobile.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.LoginGeralActivity;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Administrator-only shell. It never exposes the driver's editing or route-creation actions. */
public class MainADMActivity extends AppCompatActivity {
    private static final int HOME = 0, PROFILE = 1, NOTIFICATIONS = 2, SETTINGS = 3;
    private final List<JsonObject> submittedReports = new ArrayList<>();
    private Call<JsonObject> listCall;
    private int section = HOME;
    private int requestGeneration;
    private boolean loaded;
    private String loadError = "";
    private Uri pendingCameraUri;
    private File pendingCameraFile;
    private AlertDialog photoDialog;
    private ImageView dialogPhoto;
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), saved -> {
                if (saved && pendingCameraUri != null) saveAdminPhoto(pendingCameraUri);
                if (pendingCameraFile != null) pendingCameraFile.delete();
                pendingCameraFile = null;
                pendingCameraUri = null;
            });
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), selected -> {
                if (selected != null) saveAdminPhoto(selected);
            });
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshReports = new Runnable() {
        @Override public void run() {
            if (section == HOME || section == NOTIFICATIONS) loadReports();
            refreshHandler.postDelayed(this, 60_000L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            String uri = state.getString("admin_pending_camera_uri");
            String path = state.getString("admin_pending_camera_path");
            if (uri != null) pendingCameraUri = Uri.parse(uri);
            if (path != null) pendingCameraFile = new File(path);
        }
        if (!validAdminSession()) {
            returnToLogin();
            return;
        }
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_admin_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.adminHome).setOnClickListener(v -> showSection(HOME));
        findViewById(R.id.adminBell).setOnClickListener(v -> showSection(NOTIFICATIONS));
        findViewById(R.id.adminAvatar).setOnClickListener(v -> showSection(PROFILE));
        findViewById(R.id.adminNavHome).setOnClickListener(v -> showSection(HOME));
        findViewById(R.id.adminNavProfile).setOnClickListener(v -> showSection(PROFILE));
        findViewById(R.id.adminNavNotifications).setOnClickListener(v -> showSection(NOTIFICATIONS));
        findViewById(R.id.adminNavSettings).setOnClickListener(v -> showSection(SETTINGS));
        showSection(state == null ? HOME : state.getInt("admin_section", HOME));
    }

    @Override protected void onResume() {
        super.onResume();
        if (findViewById(R.id.adminRoot) != null) {
            if (!validAdminSession()) {
                returnToLogin();
                return;
            }
            if (section == HOME || section == NOTIFICATIONS) loadReports();
            ProfilePhotoStore.show(this, findViewById(R.id.adminAvatar));
            refreshHandler.postDelayed(refreshReports, 60_000L);
        }
    }

    @Override protected void onPause() {
        refreshHandler.removeCallbacks(refreshReports);
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("admin_section", section);
        if (pendingCameraUri != null) state.putString("admin_pending_camera_uri", pendingCameraUri.toString());
        if (pendingCameraFile != null) state.putString("admin_pending_camera_path", pendingCameraFile.getAbsolutePath());
        super.onSaveInstanceState(state);
    }

    private boolean validAdminSession() {
        return SessionManager.isAdmin(this) && SessionManager.hasUnexpiredToken(this);
    }

    private void returnToLogin() {
        Intent intent = new Intent(this, LoginGeralActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showSection(int next) {
        int previous = section;
        section = next;
        int[] navIds = {R.id.adminNavHome, R.id.adminNavProfile,
                R.id.adminNavNotifications, R.id.adminNavSettings};
        for (int i = 0; i < navIds.length; i++) {
            View nav = findViewById(navIds[i]);
            ImageView icon = (ImageView) ((android.view.ViewGroup) nav).getChildAt(0);
            icon.setBackgroundResource(i == next ? R.drawable.main_home_nav_selected
                    : android.R.color.transparent);
            int padding = dp(i == next ? 10 : 0);
            icon.setPadding(padding, padding, padding, padding);
            android.view.ViewGroup.LayoutParams params = icon.getLayoutParams();
            params.width = dp(i == next ? 48 : 28);
            params.height = dp(i == next ? 48 : 28);
            icon.setLayoutParams(params);
        }
        TextView title = findViewById(R.id.adminTitle);
        TextView subtitle = findViewById(R.id.adminSubtitle);
        int[] titles = {R.string.admin_home_title, R.string.admin_profile_title,
                R.string.settings_notifications, R.string.admin_settings_title};
        int[] subtitles = {R.string.admin_home_subtitle, R.string.admin_profile_subtitle,
                R.string.admin_notifications_subtitle, R.string.admin_settings_subtitle};
        title.setText(titles[next]);
        subtitle.setText(subtitles[next]);
        if (next == PROFILE && !SessionManager.userName(this).isBlank()) {
            title.setText(getString(R.string.main_home_greeting, SessionManager.userName(this)));
        }
        LinearLayout content = findViewById(R.id.adminContent);
        content.removeAllViews();
        if (next == HOME) renderHome(content);
        else if (next == PROFILE) renderProfile(content);
        else if (next == NOTIFICATIONS) renderNotifications(content);
        else renderSettings(content);
        if (previous != next && (next == HOME || next == NOTIFICATIONS)) loadReports();
    }

    private void loadReports() {
        if (!validAdminSession() || SessionManager.companyId(this) < 0) {
            loadError = getString(R.string.admin_company_missing);
            loaded = true;
            refreshReportsSection();
            return;
        }
        if (listCall != null) listCall.cancel();
        int generation = ++requestGeneration;
        submittedReports.clear();
        loaded = false;
        loadError = "";
        refreshReportsSection();
        loadPage(0, generation);
    }

    private void loadPage(int page, int generation) {
        String auth = SessionManager.authorizationHeader(this);
        listCall = RetrofitClient.getInstance().create(RouteDiaryService.class)
                .listAll(auth, page, 30);
        listCall.enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!active(generation)) return;
                if (!response.isSuccessful() || response.body() == null) {
                    loadError = getString(R.string.admin_reports_error, response.code());
                    loaded = true;
                    refreshReportsSection();
                    return;
                }
                JsonArray items = response.body().has("itens")
                        && response.body().get("itens").isJsonArray()
                        ? response.body().getAsJsonArray("itens") : new JsonArray();
                long companyId = SessionManager.companyId(MainADMActivity.this);
                for (JsonElement item : items) {
                    if (!item.isJsonObject()) continue;
                    JsonObject report = item.getAsJsonObject();
                    if (number(report, "empresaId") == companyId && sent(report))
                        submittedReports.add(report);
                }
                if (items.size() == 30) loadPage(page + 1, generation);
                else {
                    loaded = true;
                    refreshReportsSection();
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                if (!active(generation) || call.isCanceled()) return;
                loadError = getString(R.string.admin_reports_connection);
                loaded = true;
                refreshReportsSection();
            }
        });
    }

    private boolean active(int generation) {
        return generation == requestGeneration && !isFinishing() && !isDestroyed();
    }

    private void refreshReportsSection() {
        if (findViewById(R.id.adminRoot) != null && (section == HOME || section == NOTIFICATIONS))
            showSection(section);
    }

    private void renderHome(LinearLayout content) {
        View summary = LayoutInflater.from(this).inflate(R.layout.view_admin_home_summary, content, false);
        ((TextView) summary.findViewById(R.id.adminSummaryCount))
                .setText(getString(R.string.admin_summary_count, submittedReports.size()));
        content.addView(summary);
        heading(content, R.string.admin_reports_title);
        if (renderLoadState(content)) return;
        if (submittedReports.isEmpty()) {
            note(content, getString(R.string.admin_reports_empty));
            return;
        }
        for (JsonObject report : submittedReports) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_document_report, content, false);
            ((TextView) card.findViewById(R.id.txtDocumentDate)).setText(value(report, "dataEmbarque"));
            ((TextView) card.findViewById(R.id.txtDocumentTitle)).setText(reportTitle(report));
            card.setOnClickListener(v -> openReport(report));
            card.findViewById(R.id.btnDocumentMore).setOnClickListener(v -> openReport(report));
            content.addView(card);
        }
    }

    private boolean renderLoadState(LinearLayout content) {
        if (!loaded) {
            note(content, getString(R.string.reports_consulting));
            return true;
        }
        if (!loadError.isEmpty()) {
            TextView retry = note(content, loadError + "\n" + getString(R.string.reports_retry));
            retry.setOnClickListener(v -> loadReports());
            return true;
        }
        return false;
    }

    private void openReport(JsonObject summary) {
        int id = (int) number(summary, "id");
        if (id < 1) return;
        RetrofitClient.getInstance().create(RouteDiaryService.class)
                .get(SessionManager.authorizationHeader(this), id)
                .enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (isFinishing() || isDestroyed()) return;
                        JsonObject report = response.body();
                        if (!response.isSuccessful() || report == null
                                || number(report, "empresaId") != SessionManager.companyId(MainADMActivity.this)
                                || !sent(report)) {
                            new AlertDialog.Builder(MainADMActivity.this)
                                    .setMessage(R.string.admin_report_unavailable)
                                    .setPositiveButton(android.R.string.ok, null).show();
                            return;
                        }
                        RouteReportPdf.open(MainADMActivity.this, report,
                                SessionManager.authorizationHeader(MainADMActivity.this));
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                        if (!isFinishing() && !isDestroyed())
                            new AlertDialog.Builder(MainADMActivity.this)
                                    .setMessage(R.string.admin_report_unavailable)
                                    .setPositiveButton(android.R.string.ok, null).show();
                    }
                });
    }

    private void renderNotifications(LinearLayout content) {
        AppCompatButton toggle = button(content, notificationsEnabled()
                ? R.string.admin_disable_notifications : R.string.admin_enable_notifications,
                () -> {
                    getSharedPreferences("admin_preferences", MODE_PRIVATE).edit()
                            .putBoolean("notifications", !notificationsEnabled()).apply();
                    showSection(NOTIFICATIONS);
                });
        toggle.setAllCaps(false);
        if (!notificationsEnabled()) {
            note(content, getString(R.string.notifications_disabled_message));
            return;
        }
        if (renderLoadState(content)) return;
        if (submittedReports.isEmpty()) {
            note(content, getString(R.string.admin_notifications_empty));
            return;
        }
        for (JsonObject report : submittedReports) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_driver_notification, content, false);
            ((TextView) card.findViewById(R.id.txtNotificationTitle))
                    .setText(R.string.admin_notification_title);
            ((TextView) card.findViewById(R.id.txtNotificationMessage))
                    .setText(getString(R.string.admin_notification_message,
                            display(report, "motoristaId"), reportTitle(report)));
            ((TextView) card.findViewById(R.id.txtNotificationTime))
                    .setText(value(report, "enviadoEm"));
            card.setOnClickListener(v -> openReport(report));
            content.addView(card);
        }
    }

    private void renderProfile(LinearLayout content) {
        LinearLayout identity = new LinearLayout(this);
        identity.setGravity(android.view.Gravity.CENTER_VERTICAL);
        identity.setOrientation(LinearLayout.HORIZONTAL);
        ImageView avatar = new ImageView(this);
        avatar.setId(View.generateViewId());
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        avatarParams.setMarginEnd(dp(14));
        identity.addView(avatar, avatarParams);
        dialogPhoto = null;
        ProfilePhotoStore.show(this, avatar);
        avatar.setOnClickListener(v -> showPhotoDialog());
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this);
        name.setText(SessionManager.userName(this));
        name.setTextColor(0xff062a68);
        name.setTextSize(18);
        name.setTypeface(getResources().getFont(R.font.ibm_plex_sans_semibold));
        details.addView(name);
        AppCompatButton edit = new AppCompatButton(this);
        edit.setText(R.string.profile_edit);
        edit.setAllCaps(false);
        edit.setOnClickListener(v -> showEditOptions());
        details.addView(edit, new LinearLayout.LayoutParams(-1, dp(42)));
        identity.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(identity);
        button(content, R.string.profile_preferences, () -> showSection(SETTINGS));
        heading(content, R.string.profile_my_data);
        field(content, R.string.admin_field_role, SessionManager.adminRole(this));
        field(content, R.string.profile_phone, SessionManager.userPhone(this));
        field(content, R.string.profile_company, SessionManager.adminCompany(this));
        field(content, R.string.profile_email, SessionManager.userEmail(this));
        button(content, R.string.admin_logout, this::logout);
    }

    private void renderSettings(LinearLayout content) {
        View space = new View(this);
        content.addView(space, new LinearLayout.LayoutParams(1, dp(24)));
        option(content, R.string.settings_notifications,
                notificationsEnabled() ? R.string.settings_enabled : R.string.settings_disabled,
                () -> new AlertDialog.Builder(this).setTitle(R.string.settings_notifications)
                        .setSingleChoiceItems(new String[]{getString(R.string.settings_enabled),
                                getString(R.string.settings_disabled)}, notificationsEnabled() ? 0 : 1,
                                (dialog, choice) -> {
                                    getSharedPreferences("admin_preferences", MODE_PRIVATE).edit()
                                            .putBoolean("notifications", choice == 0).apply();
                                    dialog.dismiss();
                                    showSection(SETTINGS);
                                }).show());
        option(content, R.string.settings_language, R.string.settings_portuguese, this::chooseLanguage);
        option(content, R.string.settings_privacy, R.string.settings_permissions,
                () -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()))));
        option(content, R.string.settings_about, R.string.admin_settings_subtitle,
                () -> new AlertDialog.Builder(this).setTitle(R.string.settings_about)
                        .setMessage(getString(R.string.settings_version, versionName()))
                        .setPositiveButton(android.R.string.ok, null).show());
        option(content, R.string.settings_accessibility, R.string.settings_accessibility_summary,
                this::chooseAccessibility);
        option(content, R.string.admin_help, R.string.admin_help_summary,
                this::contactSupport);
        button(content, R.string.admin_logout, this::logout);
    }

    private void chooseLanguage() {
        String[] tags = {"pt-BR", "en", "es", "fr", "zh-CN", "ja"};
        int[] ids = {R.string.settings_portuguese, R.string.settings_english,
                R.string.settings_spanish, R.string.settings_french,
                R.string.settings_chinese, R.string.settings_japanese};
        String[] labels = new String[ids.length];
        for (int i = 0; i < ids.length; i++) labels[i] = getString(ids[i]);
        new AlertDialog.Builder(this).setTitle(R.string.settings_language)
                .setItems(labels, (dialog, choice) -> AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(tags[choice]))).show();
    }

    private void chooseAccessibility() {
        boolean[] values = {DriverPreferences.highContrast(this),
                DriverPreferences.darkBlue(this), DriverPreferences.largeText(this),
                DriverPreferences.dyslexiaReading(this), DriverPreferences.readAloud(this)};
        new AlertDialog.Builder(this).setTitle(R.string.settings_accessibility)
                .setMultiChoiceItems(new String[]{getString(R.string.settings_high_contrast),
                        getString(R.string.settings_dark_blue), getString(R.string.settings_large_text),
                        getString(R.string.settings_dyslexia_reading),
                        getString(R.string.settings_read_aloud)},
                        values, (dialog, index, checked) -> values[index] = checked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    DriverPreferences.setHighContrast(this, values[0]);
                    DriverPreferences.setDarkBlue(this, values[1]);
                    DriverPreferences.setLargeText(this, values[2]);
                    DriverPreferences.setDyslexiaReading(this, values[3]);
                    DriverPreferences.setReadAloud(this, values[4]);
                    AppCompatDelegate.setDefaultNightMode(values[1]
                            ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                    recreate();
                }).setNeutralButton(R.string.settings_reading_background,
                        (dialog, which) -> chooseReadingBackground())
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void chooseReadingBackground() {
        new AlertDialog.Builder(this).setTitle(R.string.settings_reading_background)
                .setSingleChoiceItems(new String[]{getString(R.string.settings_background_cream),
                                getString(R.string.settings_background_gray),
                                getString(R.string.settings_background_yellow)},
                        DriverPreferences.dyslexiaBackground(this), (dialog, choice) -> {
                            DriverPreferences.setDyslexiaBackground(this, choice);
                            DriverPreferences.setDyslexiaReading(this, true);
                            dialog.dismiss();
                            recreate();
                        }).setNegativeButton(R.string.settings_close, null).show();
    }

    private void contactSupport() {
        Intent email = new Intent(Intent.ACTION_SENDTO,
                Uri.parse("mailto:efficientia.inter@gmail.com"));
        email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.admin_help_subject));
        try {
            startActivity(Intent.createChooser(email, getString(R.string.admin_help)));
        } catch (RuntimeException unavailable) {
            new AlertDialog.Builder(this).setTitle(R.string.admin_help)
                    .setMessage("efficientia.inter@gmail.com")
                    .setPositiveButton(R.string.settings_close, null).show();
        }
    }

    private void showEditOptions() {
        new AlertDialog.Builder(this).setTitle(R.string.profile_edit)
                .setItems(new String[]{getString(R.string.profile_edit_name),
                        getString(R.string.profile_edit_photo)}, (dialog, choice) -> {
                    if (choice == 0) showNameDialog(); else showPhotoDialog();
                }).show();
    }

    private void showNameDialog() {
        EditText input = new EditText(this);
        int padding = dp(24);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(SessionManager.userName(this));
        input.selectAll();
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.profile_edit_name)
                .setView(input).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.profile_save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String updated = input.getText().toString().trim();
                    if (updated.isEmpty()) {
                        input.setError(getString(R.string.profile_name_required));
                        return;
                    }
                    SessionManager.updateUserName(this, updated);
                    dialog.dismiss();
                    showSection(PROFILE);
                }));
        dialog.show();
    }

    private void showPhotoDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_edit_profile_photo, null);
        dialogPhoto = content.findViewById(R.id.imgCurrentProfilePhoto);
        ProfilePhotoStore.show(this, dialogPhoto);
        photoDialog = new AlertDialog.Builder(this).setView(content).create();
        content.findViewById(R.id.btnChangeProfilePhoto).setOnClickListener(v ->
                new AlertDialog.Builder(this).setItems(new String[]{
                                getString(R.string.profile_photo_camera),
                                getString(R.string.profile_photo_gallery)}, (dialog, choice) -> {
                            if (choice == 0) openAdminCamera(); else galleryLauncher.launch("image/*");
                        }).show());
        content.findViewById(R.id.btnRemoveProfilePhoto).setOnClickListener(v -> {
            ProfilePhotoStore.remove(this);
            updateAdminPhotos();
            photoDialog.dismiss();
        });
        photoDialog.show();
    }

    private void openAdminCamera() {
        try {
            File directory = new File(getCacheDir(), "profile_camera");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("Camera cache unavailable");
            pendingCameraFile = File.createTempFile("admin_", ".jpg", directory);
            pendingCameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".profilephotos", pendingCameraFile);
            cameraLauncher.launch(pendingCameraUri);
        } catch (IOException | IllegalArgumentException error) {
            Toast.makeText(this, R.string.profile_photo_camera_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void saveAdminPhoto(Uri source) {
        try {
            ProfilePhotoStore.saveBytes(this, ProfilePhotoStore.prepare(this, source));
            updateAdminPhotos();
            if (photoDialog != null) photoDialog.dismiss();
        } catch (IOException | RuntimeException error) {
            Toast.makeText(this, R.string.profile_photo_error, Toast.LENGTH_LONG).show();
        }
    }

    private void updateAdminPhotos() {
        ImageView toolbarPhoto = findViewById(R.id.adminAvatar);
        if (toolbarPhoto != null) ProfilePhotoStore.show(this, toolbarPhoto);
        if (dialogPhoto != null) ProfilePhotoStore.show(this, dialogPhoto);
        if (section == PROFILE) showSection(PROFILE);
    }

    private void logout() {
        SessionManager.clear(this);
        returnToLogin();
    }

    private boolean notificationsEnabled() {
        return getSharedPreferences("admin_preferences", MODE_PRIVATE)
                .getBoolean("notifications", true);
    }

    private void heading(LinearLayout parent, int text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xff062a68);
        view.setTextSize(18);
        view.setTypeface(getResources().getFont(R.font.ibm_plex_sans_semibold));
        view.setPadding(0, dp(28), 0, dp(6));
        parent.addView(view);
    }

    private TextView note(LinearLayout parent, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(0xff345477);
        view.setTextSize(14);
        view.setPadding(0, dp(10), 0, dp(10));
        parent.addView(view);
        return view;
    }

    private AppCompatButton button(LinearLayout parent, int label, Runnable action) {
        AppCompatButton button = new AppCompatButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTypeface(getResources().getFont(R.font.ibm_plex_sans_semibold));
        button.setTextSize(14);
        boolean logout = label == R.string.admin_logout;
        button.setBackgroundResource(logout ? R.drawable.profile_logout_background
                : label == R.string.admin_enable_notifications
                ? R.drawable.main_home_action_green
                : R.drawable.main_home_action_blue);
        button.setTextColor(logout ? 0xffe00000
                : label == R.string.admin_enable_notifications
                ? 0xff002060 : 0xffffffff);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,
                dp(logout ? 50 : 42));
        params.setMargins(dp(logout ? 0 : 16), dp(logout ? 22 : 8), dp(logout ? 0 : 16), 0);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> action.run());
        parent.addView(button);
        return button;
    }

    private void field(LinearLayout parent, int label, String value) {
        View card = LayoutInflater.from(this).inflate(R.layout.view_profile_data_field, parent, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(9);
        card.setLayoutParams(params);
        ((TextView) card.findViewById(R.id.profileFieldLabel)).setText(label);
        ((TextView) card.findViewById(R.id.profileFieldValue)).setText(
                value == null || value.isBlank() ? getString(R.string.profile_not_informed) : value);
        parent.addView(card);
    }

    private void option(LinearLayout parent, int label, int subtitle, Runnable action) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_driver_setting, parent, false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        card.setLayoutParams(params);
        ((TextView) card.findViewById(R.id.settingsOptionTitle)).setText(label);
        ((TextView) card.findViewById(R.id.settingsOptionSubtitle)).setText(subtitle);
        card.setOnClickListener(v -> action.run());
        parent.addView(card);
    }

    private String versionName() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return version == null ? "" : version;
        } catch (PackageManager.NameNotFoundException ignored) { return ""; }
    }

    private static String value(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }

    private String display(JsonObject object, String key) {
        String value = value(object, key);
        return value.isBlank() ? getString(R.string.profile_not_informed) : value;
    }

    private static long number(JsonObject object, String key) {
        try { return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsLong() : -1; }
        catch (RuntimeException ignored) { return -1; }
    }

    private static boolean sent(JsonObject report) {
        String status = value(report, "status");
        return "pendente".equalsIgnoreCase(status) || "aprovado".equalsIgnoreCase(status)
                || "concluido".equalsIgnoreCase(status) || "reprovado".equalsIgnoreCase(status);
    }

    private String reportTitle(JsonObject report) {
        String origin = value(report, "nomeOrigem");
        String destination = value(report, "nomeDestino");
        if (!origin.isBlank() && !destination.isBlank()) return origin + " – " + destination;
        return getString(R.string.reports_diary_number, value(report, "id"));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        ++requestGeneration;
        if (listCall != null) listCall.cancel();
        super.onDestroy();
    }
}
