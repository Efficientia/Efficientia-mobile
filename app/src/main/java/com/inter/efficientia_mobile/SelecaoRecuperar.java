package com.inter.efficientia_mobile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SelecaoRecuperar extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selecao_recuperar);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
    }
}
