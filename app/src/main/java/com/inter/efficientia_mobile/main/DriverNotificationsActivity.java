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

        View.OnClickListener goHome = view -> {
            Intent home = new Intent(this, MainActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(home);
            finish();
        };
        findViewById(R.id.btnNotificationsTopHome).setOnClickListener(goHome);
        findViewById(R.id.navNotificationsHome).setOnClickListener(goHome);
        View.OnClickListener openProfile = view ->
                startActivity(new Intent(this, DriverProfileActivity.class));
        findViewById(R.id.imgNotificationsProfile).setOnClickListener(openProfile);
        findViewById(R.id.navNotificationsProfile).setOnClickListener(openProfile);
        findViewById(R.id.navNotificationsDocuments).setOnClickListener(this::showPendingFeature);
        findViewById(R.id.navNotificationsAssistant).setOnClickListener(this::showPendingFeature);
        findViewById(R.id.navNotificationsSettings).setOnClickListener(this::showPendingFeature);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ImageView photo = findViewById(R.id.imgNotificationsProfile);
        if (photo != null) {
            ProfilePhotoStore.show(this, photo);
            renderNotifications();
        }
    }

    private void renderNotifications() {
        DriverNotificationStore.ensureWelcome(this);
        LinearLayout list = findViewById(R.id.notificationsList);
        TextView empty = findViewById(R.id.txtNotificationsEmpty);
        list.removeAllViews();
        JSONArray notifications = DriverNotificationStore.list(this);
        empty.setVisibility(notifications.length() == 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < notifications.length(); i++) {
            JSONObject item = notifications.optJSONObject(i);
            if (item == null) continue;
            View card = getLayoutInflater().inflate(R.layout.item_driver_notification, list, false);
            String type = item.optString("type");
            View dot = card.findViewById(R.id.notificationDot);
            dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor("error".equals(type) ? R.color.notification_error : R.color.notification_blue)));
            ((TextView) card.findViewById(R.id.txtNotificationTitle)).setText(item.optString("title"));
            ((TextView) card.findViewById(R.id.txtNotificationMessage)).setText(item.optString("message"));
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
