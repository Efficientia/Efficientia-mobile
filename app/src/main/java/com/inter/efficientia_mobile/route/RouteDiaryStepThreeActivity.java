package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;

public class RouteDiaryStepThreeActivity extends AppCompatActivity {
    private static final String RANCHER = RouteStepSignature.RANCHER;
    private RouteDiaryDraft draft;
    private final ActivityResultLauncher<Intent> signatureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK
                        && RouteStepSignature.saveResult(this, draft, result.getData(), RANCHER))
                    renderRancherSignature();
            });
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_three);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
        renderRancherSignature();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepThreeRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        findViewById(R.id.btnRouteStepThreeBack).setOnClickListener(view -> finish());
        configureCounter(R.id.counterMales, R.string.route_males, 0);
        configureCounter(R.id.counterFemales, R.string.route_females, 0);
        configureCounter(R.id.counterMarrucos, R.string.route_marrucos, 0);
        draft.restoreCounter(this, R.id.counterMales);
        draft.restoreCounter(this, R.id.counterFemales);
        draft.restoreCounter(this, R.id.counterMarrucos);
        findViewById(R.id.btnRouteStepThreeContinue).setOnClickListener(view -> {
            draft.saveCounter(this, R.id.counterMales);
            draft.saveCounter(this, R.id.counterFemales);
            draft.saveCounter(this, R.id.counterMarrucos);
            String validation = RouteDiaryPayload.validationError(draft);
            if (!validation.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.route_check_form_title).setMessage(validation)
                        .setPositiveButton(R.string.dialog_understood, null).show();
                return;
            }
            if (!RouteStepSignature.isSigned(this, draft, RANCHER)) {
                RouteStepSignature.launchCapture(this, RANCHER, signatureLauncher);
                return;
            }
            RouteDiarySync.saveAndContinue(this, draft, view,
                    () -> startActivity(draft.next(this, RouteDiaryStepFourActivity.class)));
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (draft != null) {
            draft.put("current_stage", "2");
            renderRancherSignature();
        }
    }

    @Override protected void onPause() {
        draft.saveCounter(this, R.id.counterMales);
        draft.saveCounter(this, R.id.counterFemales);
        draft.saveCounter(this, R.id.counterMarrucos);
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        draft.saveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    private void configureCounter(int containerId, int labelId, int initialValue) {
        View container = findViewById(containerId);
        TextView label = container.findViewById(R.id.counterLabel);
        TextView value = container.findViewById(R.id.counterValue);
        label.setText(labelId);
        value.setText(String.valueOf(initialValue));
        container.findViewById(R.id.counterMinus).setOnClickListener(view -> {
            int current = Integer.parseInt(value.getText().toString());
            value.setText(String.valueOf(Math.max(0, current - 1)));
        });
        container.findViewById(R.id.counterPlus).setOnClickListener(view -> {
            int current = Integer.parseInt(value.getText().toString());
            value.setText(String.valueOf(current + 1));
        });
    }

    private void renderRancherSignature() {
        if (draft != null && findViewById(R.id.signatureRancher) != null)
            RouteStepSignature.bindCapture(this, draft, R.id.signatureRancher,
                    R.string.route_rancher, RANCHER, signatureLauncher);
    }
}
