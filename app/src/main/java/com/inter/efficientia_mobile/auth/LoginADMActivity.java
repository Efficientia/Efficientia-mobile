package com.inter.efficientia_mobile.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.main.MainADMActivity;

public class LoginADMActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_adm);
        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.main), true);

        EditText editTextCNPJ = findViewById(R.id.et_cnpj);
        EditText editTextSenha = findViewById(R.id.et_senha);
        Button buttonAvancar = findViewById(R.id.btn_avancar);

        buttonAvancar.setOnClickListener(v -> {
            Intent intent = new Intent(LoginADMActivity.this, MainADMActivity.class);
            startActivity(intent);
        });
    }
}
