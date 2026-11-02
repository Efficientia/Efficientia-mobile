package com.inter.efficientia_mobile.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.network.AuthService;
import com.inter.efficientia_mobile.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CodigoCampoActivity extends AppCompatActivity {
    public static final String EXTRA_PERFIL = "perfil";
    public static final String PERFIL_ADMIN = "administrador";
    public static final String PERFIL_MOTORISTA = "motorista";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codigo_campo);
        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.codigoCampoRoot), false);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        EditText edtEmail = findViewById(R.id.edtEmail);
        findViewById(R.id.btnContinuar).setOnClickListener(v -> {
            String email = edtEmail != null && edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            if (TextUtils.isEmpty(email)) {
                if (edtEmail != null) {
                    edtEmail.setError(getString(R.string.login_enter_email));
                    edtEmail.requestFocus();
                }
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (edtEmail != null) {
                    edtEmail.setError(getString(R.string.login_invalid_email));
                    edtEmail.requestFocus();
                }
                return;
            }
            String perfil = PERFIL_ADMIN.equals(getIntent().getStringExtra(EXTRA_PERFIL))
                    ? PERFIL_ADMIN : PERFIL_MOTORISTA;
            findViewById(R.id.btnContinuar).setEnabled(false);
            Map<String, String> request = new HashMap<>();
            request.put("email", email);
            request.put("perfil", perfil);
            RetrofitClient.getInstance().create(AuthService.class)
                    .solicitarCodigo(request)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            findViewById(R.id.btnContinuar).setEnabled(true);
                            if (response.isSuccessful()) {
                                Intent intent = new Intent(CodigoCampoActivity.this, CodigoEmailActivity.class);
                                intent.putExtra(CodigoEmailActivity.EXTRA_EMAIL, email);
                                intent.putExtra(EXTRA_PERFIL, perfil);
                                startActivity(intent);
                            } else {
                                Toast.makeText(CodigoCampoActivity.this,
                                        response.code() == 429 ? "Aguarde um minuto para pedir outro código."
                                                : "Não foi possível enviar o código. Tente novamente.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override public void onFailure(Call<ResponseBody> call, Throwable error) {
                            findViewById(R.id.btnContinuar).setEnabled(true);
                            Toast.makeText(CodigoCampoActivity.this,
                                    "Sem conexão. Tente novamente.", Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
