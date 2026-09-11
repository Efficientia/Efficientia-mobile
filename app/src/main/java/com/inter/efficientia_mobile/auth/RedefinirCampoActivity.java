package com.inter.efficientia_mobile.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;

public class RedefinirCampoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redefinir_campo);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        EditText edtEmail = findViewById(R.id.edtEmail);
        findViewById(R.id.btnContinuar).setOnClickListener(v -> {
            String email = edtEmail != null && edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            if (TextUtils.isEmpty(email)) {
                if (edtEmail != null) {
                    edtEmail.setError("Informe o e-mail");
                    edtEmail.requestFocus();
                }
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (edtEmail != null) {
                    edtEmail.setError("Informe um e-mail válido");
                    edtEmail.requestFocus();
                }
                return;
            }
            Intent intent = new Intent(this, EmailRecuperar.class);
            intent.putExtra(EmailRecuperar.EXTRA_EMAIL, email);
            startActivity(intent);
        });
    }
}
