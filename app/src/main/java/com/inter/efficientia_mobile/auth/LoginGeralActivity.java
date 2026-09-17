package com.inter.efficientia_mobile.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;

public class LoginGeralActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_geral);

        Button btnMotorista = findViewById(R.id.btnMotorista);
        Button btnAdministrador = findViewById(R.id.btnAdministrador);

        // Direciona para a tela de Login do Motorista
        btnMotorista.setOnClickListener(v -> {
            Intent intent = new Intent(LoginGeralActivity.this, LoginMotoristaActivity.class);
            startActivity(intent);
        });

        // Clique no botão Administrador
        btnAdministrador.setOnClickListener(v -> {
            Intent intent = new Intent(LoginGeralActivity.this, LoginADMActivity.class);
            startActivity(intent);
        });
    }
}