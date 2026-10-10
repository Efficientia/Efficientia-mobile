package com.inter.efficientia_mobile.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.LoginGeralActivity;
import com.inter.efficientia_mobile.auth.SessionManager;

@SuppressLint("CustomSplashScreen")
public class MainSplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Reutiliza a sessão privada do dispositivo; a API valida o token na próxima tela.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Class<?> destination = SessionManager.authorizationHeader(MainSplashActivity.this) == null
                        ? LoginGeralActivity.class : AuthenticatedSplashActivity.class;
                startActivity(new Intent(MainSplashActivity.this, destination));
                finish();
            }
        }, 2000);
    }
}
