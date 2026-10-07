package com.inter.efficientia_mobile.route;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

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
        View check = findViewById(R.id.routeLaunchCheck);
        check.setScaleX(0.86f);
        check.setScaleY(0.86f);
        check.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(750L).setInterpolator(new DecelerateInterpolator()).start();
        findViewById(R.id.routeLaunchTitle).animate().alpha(1f)
                .setStartDelay(250L).setDuration(650L).start();
        findViewById(R.id.routeLaunchCaption).animate().alpha(1f)
                .setStartDelay(450L).setDuration(650L).start();
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
