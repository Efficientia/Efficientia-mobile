package com.inter.efficientia_mobile.route;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;

public class RouteDiaryArrivalConditionActivity extends AppCompatActivity {
    private RouteDiaryDraft draft;
    private static final int[] FIELDS = {R.id.inputRouteIncidentReason, R.id.inputRouteArrivalComments};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_arrival_condition);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
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
        findViewById(R.id.btnRouteArrivalConditionContinue).setOnClickListener(view ->
                startActivity(draft.next(this, RouteDiaryStepFiveActivity.class)));
    }

    @Override protected void onPause() {
        draft.saveText(this, FIELDS);
        for (int id : new int[]{R.id.counterStanding, R.id.counterLying,
                R.id.counterDead, R.id.counterEmergency}) draft.saveCounter(this, id);
        super.onPause();
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
}
