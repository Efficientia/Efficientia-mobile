package com.inter.efficientia_mobile.auth;

import android.os.Bundle;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.main.MainADMActivity;
import com.inter.efficientia_mobile.models.AdminLoginResponse;
import com.inter.efficientia_mobile.models.LoginResponse;
import com.inter.efficientia_mobile.network.AuthService;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.splash.AuthenticatedSplashActivity;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CodigoEmailActivity extends AppCompatActivity {
    public static final String EXTRA_EMAIL = "email";
    private final EditText[] campos = new EditText[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codigo_email);
        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.codigoEmailRoot), false);

        TextView txtEmail = findViewById(R.id.txtEmail);
        txtEmail.setText(getIntent().getStringExtra(EXTRA_EMAIL));
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        int[] ids = {R.id.edtCodigo1, R.id.edtCodigo2, R.id.edtCodigo3,
                R.id.edtCodigo4, R.id.edtCodigo5, R.id.edtCodigo6};
        for (int i = 0; i < ids.length; i++) campos[i] = findViewById(ids[i]);
        for (int i = 0; i < campos.length; i++) {
            final int indice = i;
            campos[i].setImeOptions(i == campos.length - 1
                    ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
            campos[i].setSelectAllOnFocus(true);
            campos[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && campos[indice].hasFocus() && indice < campos.length - 1) {
                        campos[indice + 1].requestFocus();
                    }
                }
            });
            campos[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                        && campos[indice].getText().length() == 0 && indice > 0) {
                    campos[indice - 1].requestFocus();
                    campos[indice - 1].setText("");
                    return true;
                }
                return false;
            });
        }
        campos[5].setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validarCodigo();
                return true;
            }
            return false;
        });
        findViewById(R.id.btnFazerLogin).setOnClickListener(v -> validarCodigo());
        findViewById(R.id.btnReenviarCodigo).setOnClickListener(v -> reenviarCodigo());
    }

    private void validarCodigo() {
        for (EditText campo : campos) {
            if (!campo.getText().toString().matches("[0-9]")) {
                campo.setError(getString(R.string.codigo_incompleto));
                campo.requestFocus();
                return;
            }
        }
        StringBuilder codigo = new StringBuilder(6);
        for (EditText campo : campos) codigo.append(campo.getText());
        findViewById(R.id.btnFazerLogin).setEnabled(false);
        Map<String, String> request = codigoRequest();
        request.put("codigo", codigo.toString());
        AuthService service = RetrofitClient.getInstance().create(AuthService.class);
        if (CodigoCampoActivity.PERFIL_ADMIN.equals(perfil())) {
            service.validarCodigoAdmin(request).enqueue(new Callback<AdminLoginResponse>() {
                @Override public void onResponse(Call<AdminLoginResponse> call, Response<AdminLoginResponse> response) {
                    findViewById(R.id.btnFazerLogin).setEnabled(true);
                    AdminLoginResponse body = response.body();
                    if (!response.isSuccessful() || body == null || body.getToken() == null) {
                        erroCodigo(response.code()); return;
                    }
                    SessionManager.saveAdmin(CodigoEmailActivity.this, body);
                    entrar(MainADMActivity.class);
                }
                @Override public void onFailure(Call<AdminLoginResponse> call, Throwable error) { falhaConexao(); }
            });
        } else {
            service.validarCodigoMotorista(request).enqueue(new Callback<LoginResponse>() {
                @Override public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    findViewById(R.id.btnFazerLogin).setEnabled(true);
                    LoginResponse body = response.body();
                    if (!response.isSuccessful() || body == null || body.getToken() == null) {
                        erroCodigo(response.code()); return;
                    }
                    SessionManager.save(CodigoEmailActivity.this, body);
                    entrar(AuthenticatedSplashActivity.class);
                }
                @Override public void onFailure(Call<LoginResponse> call, Throwable error) { falhaConexao(); }
            });
        }
    }

    private String email() { return getIntent().getStringExtra(EXTRA_EMAIL); }
    private Map<String, String> codigoRequest() {
        Map<String, String> request = new HashMap<>();
        request.put("email", email());
        request.put("perfil", perfil());
        return request;
    }
    private String perfil() {
        return CodigoCampoActivity.PERFIL_ADMIN.equals(getIntent().getStringExtra(CodigoCampoActivity.EXTRA_PERFIL))
                ? CodigoCampoActivity.PERFIL_ADMIN : CodigoCampoActivity.PERFIL_MOTORISTA;
    }
    private void entrar(Class<?> destino) {
        Intent intent = new Intent(this, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    private void erroCodigo(int status) {
        Toast.makeText(this, status == 401 ? "Código inválido ou expirado. Peça outro código."
                : "Não foi possível confirmar o código. Tente novamente.", Toast.LENGTH_LONG).show();
    }
    private void falhaConexao() {
        findViewById(R.id.btnFazerLogin).setEnabled(true);
        Toast.makeText(this, "Sem conexão. Tente novamente.", Toast.LENGTH_LONG).show();
    }
    private void reenviarCodigo() {
        findViewById(R.id.btnReenviarCodigo).setEnabled(false);
        RetrofitClient.getInstance().create(AuthService.class)
                .solicitarCodigo(codigoRequest())
                .enqueue(new Callback<ResponseBody>() {
                    @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        findViewById(R.id.btnReenviarCodigo).setEnabled(true);
                        Toast.makeText(CodigoEmailActivity.this,
                                response.isSuccessful() ? "Código enviado. Confira seu e-mail."
                                        : response.code() == 429 ? "Aguarde um minuto para pedir outro código."
                                        : "Não foi possível reenviar o código.", Toast.LENGTH_LONG).show();
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable error) {
                        findViewById(R.id.btnReenviarCodigo).setEnabled(true);
                        Toast.makeText(CodigoEmailActivity.this, "Sem conexão. Tente novamente.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}
