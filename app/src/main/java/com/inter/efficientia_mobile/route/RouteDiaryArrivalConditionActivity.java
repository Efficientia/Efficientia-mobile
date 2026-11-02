package com.inter.efficientia_mobile.route;

import android.content.Intent;
import android.os.Bundle;
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

public class RouteDiaryArrivalConditionActivity extends AppCompatActivity {
    private static final String MANEUVERER = RouteStepSignature.MANEUVERER;
    private RouteDiaryDraft draft;
    private final ActivityResultLauncher<Intent> signatureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK
                        && RouteStepSignature.saveResult(this, draft, result.getData(), MANEUVERER))
                    renderManeuvererSignature();
            });
    private static final int[] FIELDS = {R.id.inputRouteIncidentReason, R.id.inputRouteArrivalComments};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_arrival_condition);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
        renderManeuvererSignature();
        draft.restoreText(this, FIELDS);
        com.inter.efficientia_mobile.FormKeyboardInsets.install(
                this, findViewById(R.id.routeArrivalConditionRoot), true);

        findViewById(R.id.btnRouteArrivalConditionBack).setOnClickListener(view -> finish());
        configureCounter(R.id.counterStanding, R.string.route_standing);
        configureCounter(R.id.counterLying, R.string.route_lying);
        configureCounter(R.id.counterDead, R.string.route_dead);
        configureCounter(R.id.counterEmergency, R.string.route_emergency);
        for (int id : new int[]{R.id.counterStanding, R.id.counterLying,
                R.id.counterDead, R.id.counterEmergency}) draft.restoreCounter(this, id);
        findViewById(R.id.btnRouteArrivalConditionContinue).setOnClickListener(view -> {
            saveForm();
            String validation = RouteDiaryPayload.validationError(draft);
            if (!validation.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.route_check_form_title).setMessage(validation)
                        .setPositiveButton(R.string.dialog_understood, null).show();
                return;
            }
            if (!RouteStepSignature.isSigned(this, draft, MANEUVERER)) {
                RouteStepSignature.launchCapture(this, MANEUVERER, signatureLauncher);
                return;
            }
            RouteDiarySync.saveAndContinue(this, draft, view,
                    () -> startActivity(draft.next(this, RouteDiaryStepFiveActivity.class)));
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (draft != null) {
            draft.put("current_stage", "5");
            renderManeuvererSignature();
        }
    }

    @Override protected void onPause() {
        saveForm();
        super.onPause();
    }

    private void saveForm() {
        draft.saveText(this, FIELDS);
        for (int id : new int[]{R.id.counterStanding, R.id.counterLying,
                R.id.counterDead, R.id.counterEmergency}) draft.saveCounter(this, id);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        draft.saveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    private void configureCounter(int containerId, int labelId) {
        View container = findViewById(containerId);
        ((TextView) container.findViewById(R.id.counterLabel)).setText(labelId);
        TextView value = container.findViewById(R.id.counterValue);
        value.setText("0");
        container.findViewById(R.id.counterMinus).setOnClickListener(view -> {
            int current = Integer.parseInt(value.getText().toString());
            value.setText(String.valueOf(Math.max(0, current - 1)));
        });
        container.findViewById(R.id.counterPlus).setOnClickListener(view -> {
            int current = Integer.parseInt(value.getText().toString());
            value.setText(String.valueOf(current + 1));
        });
    }

    private void renderManeuvererSignature() {
        if (draft != null && findViewById(R.id.signatureManeuverer) != null)
            RouteStepSignature.bindCapture(this, draft, R.id.signatureManeuverer,
                    R.string.route_maneuverer, MANEUVERER, signatureLauncher);
    }
}
