package com.inter.efficientia_mobile.splash;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.LoginGeralActivity;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.main.FirstAccessActivity;
import com.inter.efficientia_mobile.main.MainActivity;
import com.inter.efficientia_mobile.signature.DriverSignatureRepository;

public class AuthenticatedSplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 3_000L;
    private static final long SIGNATURE_CHECK_TIMEOUT_MS = 30_000L;
    private enum SignatureState { CHECKING, PRESENT, MISSING, SESSION_EXPIRED, ERROR }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean animationFinished;
    private boolean navigationStarted;
    private boolean errorDialogShown;
    private int checkAttempt;
    private SignatureState signatureState = SignatureState.CHECKING;
    private Runnable signatureCheckTimeout;
    private final Runnable finishAnimation = () -> {
        animationFinished = true;
        openDestinationIfReady();
    };

    private void openDestinationIfReady() {
        if (!animationFinished || navigationStarted || isFinishing() || isDestroyed()) return;
        if (signatureState == SignatureState.CHECKING) return;
        if (signatureState == SignatureState.ERROR) {
            if (errorDialogShown) return;
            errorDialogShown = true;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.signature_check_error_title)
                    .setMessage(R.string.signature_check_error_message)
                    .setPositiveButton(R.string.signature_check_retry, (dialog, which) -> {
                        errorDialogShown = false;
                        checkSignature();
                    })
                    .setNegativeButton(R.string.signature_check_login, (dialog, which) -> {
                        SessionManager.clear(this);
                        navigate(LoginGeralActivity.class);
                    })
                    .setCancelable(false)
                    .show();
            return;
        }
        if (signatureState == SignatureState.SESSION_EXPIRED) {
            SessionManager.clear(this);
            navigate(LoginGeralActivity.class);
        } else if (signatureState == SignatureState.PRESENT) {
            navigate(MainActivity.class);
        } else {
            navigate(FirstAccessActivity.class);
        }
    }

    private void navigate(Class<?> destination) {
        if (navigationStarted) return;
        navigationStarted = true;
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
        checkSignature();
    }

    private void checkSignature() {
        final int attempt = ++checkAttempt;
        signatureState = SignatureState.CHECKING;
        if (signatureCheckTimeout != null) handler.removeCallbacks(signatureCheckTimeout);
        signatureCheckTimeout = () -> {
            if (attempt != checkAttempt || signatureState != SignatureState.CHECKING) return;
            signatureState = SignatureState.ERROR;
            openDestinationIfReady();
        };
        handler.postDelayed(signatureCheckTimeout, SIGNATURE_CHECK_TIMEOUT_MS);

        // A API é a fonte da verdade, inclusive após novo login ou reinício do app.
        DriverSignatureRepository.fetchWithStatus(this, (image, statusCode, error) -> {
            if (image != null) image.recycle();
            if (isFinishing() || isDestroyed() || attempt != checkAttempt
                    || signatureState != SignatureState.CHECKING) return;
            handler.removeCallbacks(signatureCheckTimeout);
            if (statusCode == 200 && error == null) {
                signatureState = SignatureState.PRESENT;
            } else if (statusCode == 404) {
                SessionManager.markDriverSignatureMissing(this);
                signatureState = SignatureState.MISSING;
            } else if (statusCode == 401 || statusCode == 403) {
                signatureState = SignatureState.SESSION_EXPIRED;
            } else {
                signatureState = SignatureState.ERROR;
            }
            openDestinationIfReady();
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(finishAnimation);
        if (signatureCheckTimeout != null) handler.removeCallbacks(signatureCheckTimeout);
        super.onDestroy();
    }
}
