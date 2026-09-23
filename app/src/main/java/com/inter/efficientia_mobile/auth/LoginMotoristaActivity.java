package com.inter.efficientia_mobile.auth;

import android.content.Intent;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.main.MainActivity;
import com.inter.efficientia_mobile.network.AuthService;
import com.inter.efficientia_mobile.network.RetrofitClient;

import com.google.gson.JsonObject;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginMotoristaActivity extends AppCompatActivity {

    private ImageButton btnVoltar;
    private EditText edtCpf;
    private EditText edtEmail;
    private EditText edtSenha;
    private EditText edtCodigoEmpresa;
    private Button btnFazerLogin;
    private TextView txtEsqueciSenha;
    private boolean loginEmAndamento;

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

        configurarCampos();

        // Botão voltar (seta verde): fecha a tela e volta para a anterior (LoginGeralActivity)
        btnVoltar.setOnClickListener(v -> finish());

        // Botão de login com validação simples dos campos
        btnFazerLogin.setOnClickListener(v -> validarELogar());

        // Link "Esqueci minha senha"
        txtEsqueciSenha.setOnClickListener(v ->
                startActivity(new Intent(LoginMotoristaActivity.this, SelecaoRecuperarActivity.class)));
    }

    private void configurarCampos() {
        edtCpf.setKeyListener(DigitsKeyListener.getInstance("0123456789.-"));
        edtCpf.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        edtCpf.addTextChangedListener(new TextWatcher() {
            private boolean formatando;
            private int separadorApagado = -1;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (formatando) return;
                separadorApagado = count == 1 && after == 0
                        && (s.charAt(start) == '.' || s.charAt(start) == '-') ? start : -1;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (formatando) return;
                int cursor = Math.max(0, edtCpf.getSelectionStart());
                int digitosAntes = s.subSequence(0, cursor).toString()
                        .replaceAll("[^0-9]", "").length();
                String digitos = s.toString().replaceAll("[^0-9]", "");
                // Ao apagar um separador, apaga também o dígito anterior.
                if (separadorApagado >= 0 && digitosAntes > 0) {
                    digitos = digitos.substring(0, digitosAntes - 1)
                            + digitos.substring(digitosAntes);
                    digitosAntes--;
                }
                if (digitos.length() > 11) digitos = digitos.substring(0, 11);
                StringBuilder mascara = new StringBuilder();
                for (int i = 0; i < digitos.length(); i++) {
                    if (i == 3 || i == 6) mascara.append('.');
                    if (i == 9) mascara.append('-');
                    mascara.append(digitos.charAt(i));
                }
                String formatado = mascara.toString();
                if (formatado.contentEquals(s)) return;
                int novaPosicao = 0;
                int encontrados = 0;
                while (novaPosicao < formatado.length() && encontrados < digitosAntes) {
                    if (Character.isDigit(formatado.charAt(novaPosicao))) encontrados++;
                    novaPosicao++;
                }
                formatando = true;
                s.replace(0, s.length(), formatado);
                edtCpf.setSelection(novaPosicao);
                formatando = false;
            }
        });

        edtEmail.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edtSenha.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edtCodigoEmpresa.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        EditText[] campos = {edtCpf, edtEmail, edtSenha, edtCodigoEmpresa};
        for (int i = 0; i < campos.length; i++) {
            campos[i].setSingleLine(true);
            campos[i].setImeOptions(i == campos.length - 1
                    ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
            if (i < campos.length - 1) campos[i].setNextFocusForwardId(campos[i + 1].getId());
        }
        // setSingleLine substitui a transformação; restaura a máscara da senha depois.
        edtSenha.setTransformationMethod(PasswordTransformationMethod.getInstance());
        ImageButton btnVisibilidadeSenha = findViewById(R.id.btnVisibilidadeSenha);
        btnVisibilidadeSenha.setOnClickListener(v -> {
            int inicio = edtSenha.getSelectionStart();
            int fim = edtSenha.getSelectionEnd();
            boolean mostrar = edtSenha.getTransformationMethod()
                    instanceof PasswordTransformationMethod;
            edtSenha.setTransformationMethod(mostrar
                    ? null : PasswordTransformationMethod.getInstance());
            btnVisibilidadeSenha.setImageResource(mostrar
                    ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            btnVisibilidadeSenha.setContentDescription(getString(mostrar
                    ? R.string.ocultar_senha : R.string.mostrar_senha));
            if (inicio >= 0 && fim >= 0) edtSenha.setSelection(inicio, fim);
        });

        edtCodigoEmpresa.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validarELogar();
                return true;
            }
            return false;
        });
    }

    private void validarELogar() {
        if (loginEmAndamento) return;

        String cpf = edtCpf.getText().toString().replaceAll("[^0-9]", "");
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString();
        String codigoEmpresa = edtCodigoEmpresa.getText().toString().trim();

        if (TextUtils.isEmpty(cpf)) {
            edtCpf.setError("Informe o CPF");
            edtCpf.requestFocus();
            return;
        }

        if (cpf.length() != 11) {
            edtCpf.setError("Informe o CPF com 11 dígitos");
            edtCpf.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Informe o e-mail");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Informe um e-mail válido");
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

        setLoginEmAndamento(true);
        
        JsonObject request = new JsonObject();
        request.addProperty("cpf", cpf);
        request.addProperty("email", email);
        request.addProperty("senha", senha);
        request.addProperty("codigoEmpresa", codigoEmpresa);

        AuthService service = RetrofitClient.getInstance().create(AuthService.class);
        service.login(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (isFinishing() || isDestroyed()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().toString());
                        if (jsonResponse.optString("token").isEmpty()) {
                            mostrarErro("A API não retornou o token de autenticação.");
                            return;
                        }
                        
                        SessionManager.save(LoginMotoristaActivity.this, jsonResponse);
                        setLoginEmAndamento(false);
                        Toast.makeText(LoginMotoristaActivity.this,
                                "Login realizado com sucesso!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginMotoristaActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        
                    } catch (JSONException e) {
                        mostrarErro("A API retornou uma resposta inválida.");
                    }
                } else {
                    String errorMessage = "Falha no login (HTTP " + response.code() + ").";
                    if (response.errorBody() != null) {
                        try {
                            JSONObject errorJson = new JSONObject(response.errorBody().string());
                            if (errorJson.has("detail") && !errorJson.getString("detail").isEmpty()) {
                                errorMessage = errorJson.getString("detail");
                            } else if (errorJson.has("title") && !errorJson.getString("title").isEmpty()) {
                                errorMessage = errorJson.getString("title");
                            } else if (errorJson.has("message") && !errorJson.getString("message").isEmpty()) {
                                errorMessage = errorJson.getString("message");
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    mostrarErro(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                mostrarErro("Não foi possível conectar à API. Verifique sua conexão.");
            }
            
            private void mostrarErro(String msg) {
                setLoginEmAndamento(false);
                Toast.makeText(LoginMotoristaActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoginEmAndamento(boolean emAndamento) {
        loginEmAndamento = emAndamento;
        btnFazerLogin.setEnabled(!emAndamento);
        btnFazerLogin.setText(emAndamento
                ? R.string.login_entrando : R.string.login_fazer);
    }
}
