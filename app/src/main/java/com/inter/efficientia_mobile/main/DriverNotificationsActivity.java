package com.inter.efficientia_mobile.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.splash.MainSplashActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class DriverNotificationsActivity extends AppCompatActivity {
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
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_driver_notifications);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notificationsRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        View.OnClickListener goHome = view -> ToolbarNavigation.home(this);
        findViewById(R.id.btnNotificationsTopHome).setOnClickListener(goHome);
        findViewById(R.id.navNotificationsHome).setOnClickListener(goHome);
        View.OnClickListener openProfile = view ->
                ToolbarNavigation.open(this, DriverProfileActivity.class);
        findViewById(R.id.imgNotificationsProfile).setOnClickListener(openProfile);
        findViewById(R.id.navNotificationsProfile).setOnClickListener(openProfile);
        findViewById(R.id.navNotificationsDocuments).setOnClickListener(view ->
                ToolbarNavigation.open(this, DocumentsActivity.class));
        findViewById(R.id.navNotificationsAssistant).setOnClickListener(view ->
                ToolbarNavigation.open(this, CinttiaActivity.class));
        findViewById(R.id.navNotificationsSettings).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverSettingsActivity.class));
        findViewById(R.id.btnEnableNotifications).setOnClickListener(view -> {
            DriverPreferences.setNotificationsEnabled(this, true);
            renderNotifications();
        });
        findViewById(R.id.btnDisableNotifications).setOnClickListener(view -> {
            DriverPreferences.setNotificationsEnabled(this, false);
            renderNotifications();
        });
        MaterialSwitch gpsShortcut = findViewById(R.id.switchHomeGps);
        gpsShortcut.setChecked(DriverPreferences.gpsShortcutVisible(this));
        gpsShortcut.setOnCheckedChangeListener((button, checked) ->
                DriverPreferences.setGpsShortcutVisible(this, checked));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImageView photo = findViewById(R.id.imgNotificationsProfile);
        if (photo != null) ProfilePhotoStore.show(this, photo);
        renderNotifications();
    }

    private void renderNotifications() {
        LinearLayout list = findViewById(R.id.notificationsList);
        TextView empty = findViewById(R.id.txtNotificationsEmpty);
        list.removeAllViews();
        View enable = findViewById(R.id.btnEnableNotifications);
        View disable = findViewById(R.id.btnDisableNotifications);
        if (!DriverPreferences.notificationsEnabled(this)) {
            list.setVisibility(View.GONE);
            empty.setText(R.string.notifications_disabled_message);
            empty.setVisibility(View.VISIBLE);
            enable.setVisibility(View.VISIBLE);
            disable.setVisibility(View.GONE);
            return;
        }
        list.setVisibility(View.VISIBLE);
        enable.setVisibility(View.GONE);
        disable.setVisibility(View.VISIBLE);
        empty.setText(R.string.notifications_empty);
        DriverNotificationStore.ensureWelcome(this);
        JSONArray notifications = DriverNotificationStore.list(this);
        empty.setVisibility(notifications.length() == 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < notifications.length(); i++) {
            JSONObject item = notifications.optJSONObject(i);
            if (item == null) continue;
            View card = getLayoutInflater().inflate(R.layout.item_driver_notification, list, false);
            String type = item.optString("type");
            String savedTitle = item.optString("title");
            boolean diaryTip = "welcome_diary".equals(type)
                    || ("welcome".equals(type) && ("Seu diário de rota".equals(savedTitle)
                    || "Your route diary".equals(savedTitle)));
            View dot = card.findViewById(R.id.notificationDot);
            dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor("error".equals(type) ? R.color.notification_error : R.color.notification_blue)));
            TextView title = card.findViewById(R.id.txtNotificationTitle);
            if (diaryTip) title.setText(R.string.notifications_diary_title);
            else if ("welcome".equals(type)) title.setText(R.string.notifications_welcome_title);
            else if ("success".equals(type)) title.setText(R.string.notification_sent_title);
            else if ("error".equals(type)) title.setText(R.string.notification_failed_title);
            else title.setText(savedTitle);
            String message = item.optString("message");
            if (diaryTip) message = getString(R.string.notifications_diary_message);
            else if ("welcome".equals(type)) message = getString(R.string.notifications_welcome_message);
            else if ("success".equals(type)) {
                String route = item.optString("route");
                message = route.isBlank() ? getString(R.string.notification_sent_generic)
                        : getString(R.string.notification_sent_route, route);
            }
            ((TextView) card.findViewById(R.id.txtNotificationMessage)).setText(message);
            long time = item.optLong("time");
            ((TextView) card.findViewById(R.id.txtNotificationTime)).setText(
                    time > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(new Date(time)) : "");
            list.addView(card);
        }
    }

    private void showPendingFeature(View ignored) {
        Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show();
    }
}
