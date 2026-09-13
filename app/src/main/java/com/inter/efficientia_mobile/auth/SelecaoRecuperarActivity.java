package com.inter.efficientia_mobile.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;

public class SelecaoRecuperarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selecao_recuperar);

        // Voltar para a tela anterior
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        // Redireciona para a tela de redefinir
        findViewById(R.id.btnRedefinir).setOnClickListener(v -> {
            Intent intent = new Intent(SelecaoRecuperarActivity.this, RedefinirCampoActivity.class);
            startActivity(intent);
        });

        // Enviar código único
        findViewById(R.id.btnEnviarCodigo).setOnClickListener(v -> {
            Intent intent = new Intent(SelecaoRecuperarActivity.this, CodigoCampoActivity.class);
            startActivity(intent);
        });
    }
}