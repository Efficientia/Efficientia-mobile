package com.inter.efficientia_mobile.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.inter.efficientia_mobile.R;

public class CodigoEmailActivity extends AppCompatActivity {
    public static final String EXTRA_EMAIL = "email";
    private final EditText[] campos = new EditText[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codigo_email);

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
        findViewById(R.id.btnReenviarCodigo).setOnClickListener(v ->
                Toast.makeText(this, R.string.codigo_servico_indisponivel, Toast.LENGTH_SHORT).show());
    }

    private void validarCodigo() {
        for (EditText campo : campos) {
            if (!campo.getText().toString().matches("[0-9]")) {
                campo.setError(getString(R.string.codigo_incompleto));
                campo.requestFocus();
                return;
            }
        }
        Toast.makeText(this, R.string.codigo_servico_indisponivel, Toast.LENGTH_SHORT).show();
    }
}
