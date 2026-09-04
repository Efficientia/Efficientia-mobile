package com.inter.efficientia_mobile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginMotorista extends AppCompatActivity {

    private ImageButton btnVoltar;
    private EditText edtCpf;
    private EditText edtEmail;
    private EditText edtSenha;
    private EditText edtCodigoEmpresa;
    private Button btnFazerLogin;
    private TextView txtEsqueciSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_motorista);

        // Inicializa os componentes da tela pelo ID
        btnVoltar = findViewById(R.id.btnVoltar);
        edtCpf = findViewById(R.id.edtCpf);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        edtCodigoEmpresa = findViewById(R.id.edtCodigoEmpresa);
        btnFazerLogin = findViewById(R.id.btnFazerLogin);
        txtEsqueciSenha = findViewById(R.id.txtEsqueciSenha);

        // Botão voltar (seta verde): fecha a tela e volta para a anterior (LoginGeral)
        btnVoltar.setOnClickListener(v -> finish());

        // Botão de login com validação simples dos campos
        btnFazerLogin.setOnClickListener(v -> validarELogar());

        // Link "Esqueci minha senha"
        txtEsqueciSenha.setOnClickListener(v -> {
            Toast.makeText(LoginMotorista.this, "Recuperação de senha clicada", Toast.LENGTH_SHORT).show();
            // Futuramente: startActivity(new Intent(LoginMotorista.this, EsqueciSenhaActivity.class));
        });
    }

    private void validarELogar() {
        String cpf = edtCpf.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString().trim();
        String codigoEmpresa = edtCodigoEmpresa.getText().toString().trim();

        if (TextUtils.isEmpty(cpf)) {
            edtCpf.setError("Informe o CPF");
            edtCpf.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Informe o e-mail");
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(senha)) {
            edtSenha.setError("Informe a senha");
            edtSenha.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(codigoEmpresa)) {
            edtCodigoEmpresa.setError("Informe o código da empresa");
            edtCodigoEmpresa.requestFocus();
            return;
        }

        // Se todos os campos estiverem preenchidos:
        Toast.makeText(this, "Processando login do motorista...", Toast.LENGTH_SHORT).show();

        // Aqui entrará a chamada da API / Firebase / Banco de dados
    }
}