package com.inter.efficientia_mobile.route;

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

public class RouteDiaryStepFiveActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_five);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepFiveRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        findViewById(R.id.btnRouteStepFiveBack).setOnClickListener(view -> finish());
        configureSignature(R.id.signatureRancher, R.string.route_rancher);
        configureSignature(R.id.signatureDriver, R.string.route_driver);
        configureSignature(R.id.signatureManeuverer, R.string.route_maneuverer);
        configureSignature(R.id.signatureCorralWorker, R.string.route_corral_worker);
        findViewById(R.id.btnRouteStepFiveContinue).setOnClickListener(view ->
                android.widget.Toast.makeText(this, R.string.main_home_feature_pending, android.widget.Toast.LENGTH_SHORT).show());
    }

    private void configureSignature(int containerId, int titleId) {
        View row = findViewById(containerId);
        ((TextView) row.findViewById(R.id.signatureRole)).setText(titleId);
        row.setOnClickListener(view -> view.setSelected(!view.isSelected()));
    }
}
