package com.inter.efficientia_mobile.route;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.main.MainActivity;

public class RouteDiaryLaunchActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION_MS = 3_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openHome = () -> {
        if (isFinishing() || isDestroyed()) return;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_launch);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.postDelayed(openHome, SPLASH_DURATION_MS);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(openHome);
        super.onPause();
    }
}
