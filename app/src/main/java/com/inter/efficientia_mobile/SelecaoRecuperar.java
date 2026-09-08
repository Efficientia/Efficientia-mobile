package com.inter.efficientia_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SelecaoRecuperar extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selecao_recuperar);

        // Voltar para a tela anterior
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        // Redireciona para a tela de redefinir
        findViewById(R.id.btnRedefinir).setOnClickListener(v -> {
            Intent intent = new Intent(SelecaoRecuperar.this, RedefinirCampo.class);
            startActivity(intent);
        });

        // Enviar código único
        findViewById(R.id.btnEnviarCodigo).setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidade de enviar código em desenvolvimento", Toast.LENGTH_SHORT).show();
        });
    }
}