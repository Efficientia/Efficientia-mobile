package com.inter.efficientia_mobile.splash;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.main.FirstAccessActivity;
import com.inter.efficientia_mobile.main.MainActivity;
import com.inter.efficientia_mobile.signature.DriverSignatureRepository;

public class AuthenticatedSplashActivity extends AppCompatActivity {

    public static final String EXTRA_FORCE_SIGNATURE_FLOW = "force_signature_flow";
    private static final long SPLASH_DURATION_MS = 3_000L;
    private static final long SIGNATURE_CHECK_TIMEOUT_MS = 10_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean animationFinished;
    private boolean signatureCheckFinished;
    private boolean remoteSignatureAvailable;
    private boolean navigationStarted;
    private final Runnable finishAnimation = () -> {
        animationFinished = true;
        openDestinationIfReady();
    };
    private final Runnable signatureCheckTimeout = () -> {
        signatureCheckFinished = true;
        openDestinationIfReady();
    };

    private void openDestinationIfReady() {
        if (!animationFinished || !signatureCheckFinished || navigationStarted) return;
        if (isFinishing() || isDestroyed()) return;
        navigationStarted = true;
        boolean forceSignatureFlow = getIntent().getBooleanExtra(EXTRA_FORCE_SIGNATURE_FLOW, false);
        Class<?> destination = remoteSignatureAvailable && !forceSignatureFlow
                ? MainActivity.class : FirstAccessActivity.class;
        if (remoteSignatureAvailable && !forceSignatureFlow) {
            Toast.makeText(this, "Assinatura existente confirmada na API.",
                    Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

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
        handler.postDelayed(finishAnimation, SPLASH_DURATION_MS);
        if (getIntent().getBooleanExtra(EXTRA_FORCE_SIGNATURE_FLOW, false)) {
            signatureCheckFinished = true;
            return;
        }
        if (SessionManager.hasRegisteredDriverSignature(this)) {
            DriverSignatureRepository.fetch(this, (image, error) -> {
                if (image != null) {
                    remoteSignatureAvailable = true;
                    image.recycle();
                }
                signatureCheckFinished = true;
                handler.removeCallbacks(signatureCheckTimeout);
                openDestinationIfReady();
            });
            handler.postDelayed(signatureCheckTimeout, SIGNATURE_CHECK_TIMEOUT_MS);
        } else {
            signatureCheckFinished = true;
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(finishAnimation);
        handler.removeCallbacks(signatureCheckTimeout);
        super.onDestroy();
    }
}
