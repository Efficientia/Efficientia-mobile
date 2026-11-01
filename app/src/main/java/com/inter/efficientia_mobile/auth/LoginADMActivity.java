package com.inter.efficientia_mobile.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.ImageView;
import android.widget.Toast;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.main.MainADMActivity;
import com.inter.efficientia_mobile.models.AdminLoginRequest;
import com.inter.efficientia_mobile.models.AdminLoginResponse;
import com.inter.efficientia_mobile.network.AuthService;
import com.inter.efficientia_mobile.network.RetrofitClient;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginADMActivity extends AppCompatActivity {
    private EditText cnpjField;
    private EditText passwordField;
    private Button advanceButton;
    private boolean loginInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_adm);
        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.main), true);

        cnpjField = findViewById(R.id.et_cnpj);
        passwordField = findViewById(R.id.et_senha);
        advanceButton = findViewById(R.id.btn_avancar);
        findViewById(R.id.btn_voltar).setOnClickListener(v -> finish());
        findViewById(R.id.tv_esqueci_senha).setOnClickListener(v ->
                startActivity(new Intent(this, SelecaoRecuperarActivity.class)));
        ImageView toggle = findViewById(R.id.btn_toggle_senha);
        toggle.setOnClickListener(v -> {
            int selection = passwordField.getSelectionStart();
            boolean visible = passwordField.getTransformationMethod()
                    instanceof HideReturnsTransformationMethod;
            passwordField.setTransformationMethod(visible
                    ? PasswordTransformationMethod.getInstance()
                    : HideReturnsTransformationMethod.getInstance());
            passwordField.setSelection(Math.max(0, selection));
        });
        advanceButton.setOnClickListener(v -> login());
    }

    private void login() {
        if (loginInProgress) return;
        String cnpj = cnpjField.getText().toString().replaceAll("\\D", "");
        String password = passwordField.getText().toString();
        if (cnpj.length() != 14) {
            cnpjField.setError(getString(R.string.admin_invalid_cnpj));
            cnpjField.requestFocus();
            return;
        }
        if (password.isBlank()) {
            passwordField.setError(getString(R.string.login_enter_password));
            passwordField.requestFocus();
            return;
        }
        loginInProgress = true;
        advanceButton.setEnabled(false);
        advanceButton.setText(R.string.login_entrando);
        RetrofitClient.getInstance().create(AuthService.class)
                .loginAdmin(new AdminLoginRequest(cnpj, password))
                .enqueue(new Callback<AdminLoginResponse>() {
                    @Override public void onResponse(Call<AdminLoginResponse> call,
                                                     Response<AdminLoginResponse> response) {
                        if (isFinishing() || isDestroyed()) return;
                        resetButton();
                        AdminLoginResponse body = response.body();
                        if (response.isSuccessful() && body != null
                                && body.getToken() != null && !body.getToken().isBlank()) {
                            SessionManager.saveAdmin(LoginADMActivity.this, body);
                            Intent intent = new Intent(LoginADMActivity.this, MainADMActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            return;
                        }
                        String message = getString(R.string.login_http_error, response.code());
                        if (response.isSuccessful() && body != null && body.getMensagem() != null) {
                            message = body.getMensagem();
                        } else if (response.errorBody() != null) {
                            try {
                                JSONObject error = new JSONObject(response.errorBody().string());
                                message = error.optString("detail", error.optString("message", message));
                            } catch (Exception ignored) { }
                        }
                        Toast.makeText(LoginADMActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                    @Override public void onFailure(Call<AdminLoginResponse> call, Throwable error) {
                        if (isFinishing() || isDestroyed()) return;
                        resetButton();
                        Toast.makeText(LoginADMActivity.this,
                                R.string.login_connection_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void resetButton() {
        loginInProgress = false;
        advanceButton.setEnabled(true);
        advanceButton.setText(R.string.login_next);
    }
}
