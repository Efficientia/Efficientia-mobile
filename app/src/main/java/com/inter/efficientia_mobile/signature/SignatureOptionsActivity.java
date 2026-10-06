package com.inter.efficientia_mobile.signature;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;

public class SignatureOptionsActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> captureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature_options);

        findViewById(R.id.btnDrawSignature).setOnClickListener(
                view -> openCapture(SignatureCaptureActivity.MODE_DRAW)
        );
        findViewById(R.id.btnTypeSignature).setOnClickListener(
                view -> openCapture(SignatureCaptureActivity.MODE_TYPE)
        );
    }

    private void openCapture(String mode) {
        Intent intent = new Intent(this, SignatureCaptureActivity.class);
        intent.putExtra(SignatureCaptureActivity.EXTRA_MODE, mode);
        String role = getIntent().getStringExtra(SignatureCaptureActivity.EXTRA_SIGNER_ROLE);
        if (role == null) {
            startActivity(intent);
        } else {
            intent.putExtra(SignatureCaptureActivity.EXTRA_SIGNER_ROLE, role);
            captureLauncher.launch(intent);
        }
    }
}
