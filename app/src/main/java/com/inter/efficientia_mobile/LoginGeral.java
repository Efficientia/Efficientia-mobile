package com.inter.efficientia_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginGeral extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_geral);

        Button btnMotorista = findViewById(R.id.btnMotorista);
        Button btnAdministrador = findViewById(R.id.btnAdministrador);

        // Direciona para a tela de Login do Motorista
        btnMotorista.setOnClickListener(v -> {
            Intent intent = new Intent(LoginGeral.this, LoginMotorista.class);
            startActivity(intent);
        });

        // Clique no botão Administrador
        btnAdministrador.setOnClickListener(v -> {
            Toast.makeText(LoginGeral.this, "Selecionado: Administrador", Toast.LENGTH_SHORT).show();
        });
    }
}