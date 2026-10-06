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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_arrival_condition);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeArrivalConditionRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        findViewById(R.id.btnRouteArrivalConditionBack).setOnClickListener(view -> finish());
        configureCounter(R.id.counterStanding, R.string.route_standing);
        configureCounter(R.id.counterLying, R.string.route_lying);
        configureCounter(R.id.counterDead, R.string.route_dead);
        configureCounter(R.id.counterEmergency, R.string.route_emergency);
        findViewById(R.id.btnRouteArrivalConditionContinue).setOnClickListener(view ->
                startActivity(new Intent(this, RouteDiaryStepFiveActivity.class)));
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
