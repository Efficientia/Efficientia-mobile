package com.inter.efficientia_mobile.signature;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;

public class SignatureOptionsActivity extends AppCompatActivity {

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
        startActivity(intent);
    }
}
