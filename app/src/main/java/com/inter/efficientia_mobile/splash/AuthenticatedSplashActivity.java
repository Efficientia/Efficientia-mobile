package com.inter.efficientia_mobile.splash;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.main.MainActivity;

public class AuthenticatedSplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 3_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openHome = () -> {
        if (isFinishing() || isDestroyed()) return;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticated_splash);

        TextView greeting = findViewById(R.id.txtAuthenticatedGreeting);
        ProgressBar progress = findViewById(R.id.authenticatedProgress);
        String fullName = SessionManager.userName(this);
        greeting.setText(getString(
                R.string.authenticated_splash_greeting,
                fullName.isBlank() ? SessionManager.firstName(this) : fullName
        ));

        ObjectAnimator animator = ObjectAnimator.ofInt(progress, "progress", 0, 100);
        animator.setDuration(SPLASH_DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
        handler.postDelayed(openHome, SPLASH_DURATION_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openHome);
        super.onDestroy();
    }
}
