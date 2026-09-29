package com.inter.efficientia_mobile.signature;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.main.MainADMActivity;

public class SignatureCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "signature_mode";
    public static final String MODE_DRAW = "draw";
    public static final String MODE_TYPE = "type";

    private SignaturePadView signaturePad;
    private EditText signatureName;
    private boolean drawMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature_capture);

        TextView instruction = findViewById(R.id.txtSignatureInstruction);
        View drawContainer = findViewById(R.id.signatureDrawContainer);
        signaturePad = findViewById(R.id.signaturePad);
        signatureName = findViewById(R.id.edtSignatureName);
        ImageView penHint = findViewById(R.id.imgSignaturePenHint);

        drawMode = MODE_DRAW.equals(getIntent().getStringExtra(EXTRA_MODE));
        instruction.setText(drawMode
                ? R.string.signature_draw_instruction
                : R.string.signature_type_instruction);
        drawContainer.setVisibility(drawMode ? View.VISIBLE : View.GONE);
        signatureName.setVisibility(drawMode ? View.GONE : View.VISIBLE);

        signaturePad.setOnSignatureStartedListener(
                () -> penHint.setVisibility(View.GONE)
        );
        findViewById(R.id.btnFinishSignature).setOnClickListener(
                view -> finishSignature()
        );
    }

    private void finishSignature() {
        boolean valid = drawMode
                ? signaturePad.hasSignature()
                : !signatureName.getText().toString().trim().isEmpty();

        if (!valid) {
            Toast.makeText(this, R.string.signature_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MainADMActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
