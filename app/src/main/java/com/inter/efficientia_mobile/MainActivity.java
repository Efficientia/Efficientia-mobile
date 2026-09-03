package com.inter.efficientia_mobile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_geral);

        // Localiza os botões pelo ID definido no XML
        Button btnMotorista = findViewById(R.id.btnMotorista);
        Button btnAdministrador = findViewById(R.id.btnAdministrador);

        // Clique no botão Motorista
        btnMotorista.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Selecionado: Motorista", Toast.LENGTH_SHORT).show();
        });

        // Clique no botão Administrador
        btnAdministrador.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Selecionado: Administrador", Toast.LENGTH_SHORT).show();
        });
    }
}