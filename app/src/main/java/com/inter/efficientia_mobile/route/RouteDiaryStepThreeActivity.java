package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;

public class RouteDiaryStepThreeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_three);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepThreeRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        findViewById(R.id.btnRouteStepThreeBack).setOnClickListener(view -> finish());
        configureCounter(R.id.counterMales, R.string.route_males, 22);
        configureCounter(R.id.counterFemales, R.string.route_females, 22);
        configureCounter(R.id.counterMarrucos, R.string.route_marrucos, 22);
        configureCounter(R.id.counterStanding, R.string.route_standing, 37);
        configureCounter(R.id.counterLying, R.string.route_lying, 1);
        configureCounter(R.id.counterDead, R.string.route_dead, 0);
        configureCounter(R.id.counterEmergency, R.string.route_emergency, 0);
        findViewById(R.id.btnRouteStepThreeContinue).setOnClickListener(view ->
                Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show());
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
}
