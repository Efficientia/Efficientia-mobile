package com.inter.efficientia_mobile.auth;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;

public class EmailRecuperar extends AppCompatActivity {
    public static final String EXTRA_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_recuperar);

        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email != null) {
            TextView txtSubtitulo = findViewById(R.id.txtSubtitulo);
            txtSubtitulo.setText(getString(R.string.recuperacao_email_enviado, email));
        }

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnContinuar).setOnClickListener(v -> finish());
    }
}
