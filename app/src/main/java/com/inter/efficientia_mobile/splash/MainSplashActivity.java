package com.inter.efficientia_mobile.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.LoginGeralActivity;

@SuppressLint("CustomSplashScreen")
public class MainSplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Segura a splash por 2 segundos, depois vai para a tela de login geral
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(MainSplashActivity.this, LoginGeralActivity.class));
                finish();
            }
        }, 2000);
    }
}