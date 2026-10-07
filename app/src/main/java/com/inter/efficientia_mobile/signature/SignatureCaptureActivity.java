package com.inter.efficientia_mobile.signature;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.main.MainActivity;

import java.io.IOException;

public class SignatureCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "signature_mode";
    public static final String MODE_DRAW = "draw";
    public static final String MODE_TYPE = "type";
    public static final String EXTRA_SIGNER_ROLE = "signer_role";
    public static final String EXTRA_IMAGE_PATH = "signature_image_path";

    private SignaturePadView signaturePad;
    private EditText signatureName;
    private View signatureNamePreviewContainer;
    private View finishButton;
    private TextView signatureNamePreview;
    private boolean drawMode;
    private String confirmedSignatureName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature_capture);
        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.signatureCaptureRoot), false);

        TextView instruction = findViewById(R.id.txtSignatureInstruction);
        View drawContainer = findViewById(R.id.signatureDrawContainer);
        signaturePad = findViewById(R.id.signaturePad);
        signatureName = findViewById(R.id.edtSignatureName);
        signatureNamePreviewContainer = findViewById(R.id.signatureNamePreviewContainer);
        signatureNamePreview = findViewById(R.id.txtSignatureNamePreview);
        View confirmNameButton = findViewById(R.id.btnConfirmSignatureName);
        finishButton = findViewById(R.id.btnFinishSignature);
        ImageView penHint = findViewById(R.id.imgSignaturePenHint);

        drawMode = MODE_DRAW.equals(getIntent().getStringExtra(EXTRA_MODE));
        instruction.setText(drawMode
                ? R.string.signature_draw_instruction
                : R.string.signature_type_instruction);
        drawContainer.setVisibility(drawMode ? View.VISIBLE : View.GONE);
        signatureName.setVisibility(drawMode ? View.GONE : View.VISIBLE);
        confirmNameButton.setVisibility(drawMode ? View.GONE : View.VISIBLE);
        finishButton.setVisibility(drawMode ? View.VISIBLE : View.GONE);

        signaturePad.setOnSignatureStartedListener(
                () -> penHint.setVisibility(View.GONE)
        );
        confirmNameButton.setOnClickListener(view -> confirmTypedSignature());
        signatureName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                if (!drawMode && !confirmedSignatureName.isEmpty()
                        && !confirmedSignatureName.equals(text.toString().trim())) {
                    clearTypedSignatureConfirmation();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        finishButton.setOnClickListener(
                view -> finishSignature()
        );
    }

    private void confirmTypedSignature() {
        String fullName = signatureName.getText().toString().trim();
        if (fullName.isEmpty()) {
            Toast.makeText(this, R.string.signature_required, Toast.LENGTH_SHORT).show();
            return;
        }

        confirmedSignatureName = fullName;
        signatureNamePreview.setText(fullName);
        signatureNamePreviewContainer.setVisibility(View.VISIBLE);
        finishButton.setVisibility(View.VISIBLE);

        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(signatureName.getWindowToken(), 0);
        }
        signatureName.clearFocus();
    }

    private void clearTypedSignatureConfirmation() {
        confirmedSignatureName = "";
        signatureNamePreviewContainer.setVisibility(View.GONE);
        finishButton.setVisibility(View.GONE);
    }

    private void finishSignature() {
        boolean valid = drawMode
                ? signaturePad.hasSignature()
                : !confirmedSignatureName.isEmpty()
                && confirmedSignatureName.equals(signatureName.getText().toString().trim());

        if (!valid) {
            Toast.makeText(this, R.string.signature_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap image = drawMode ? signaturePad.renderSignature() : renderTypedSignature();
        try {
            String role = getIntent().getStringExtra(EXTRA_SIGNER_ROLE);
            if (role != null) {
                String path = "MOTORISTA".equals(role)
                        ? SignatureImageStore.saveDriver(this, image)
                        : SignatureImageStore.saveRoute(this, image);
                Intent result = new Intent()
                        .putExtra(EXTRA_SIGNER_ROLE, role)
                        .putExtra(EXTRA_IMAGE_PATH, path);
                setResult(RESULT_OK, result);
                finish();
            } else {
                SignatureImageStore.saveDriver(this, image);
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        } catch (IOException exception) {
            Toast.makeText(this, R.string.signature_save_error, Toast.LENGTH_LONG).show();
        } finally {
            image.recycle();
        }
    }

    private Bitmap renderTypedSignature() {
        Bitmap image = Bitmap.createBitmap(1024, 256, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        Paint textPaint = new Paint(signatureNamePreview.getPaint());
        textPaint.setAntiAlias(true);
        textPaint.setColor(signatureNamePreview.getCurrentTextColor());
        float width = textPaint.measureText(confirmedSignatureName);
        if (width > 960f) {
            textPaint.setTextSize(textPaint.getTextSize() * 960f / width);
        }
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = 128f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(confirmedSignatureName,
                (1024f - textPaint.measureText(confirmedSignatureName)) / 2f,
                baseline, textPaint);
        return image;
    }
}
