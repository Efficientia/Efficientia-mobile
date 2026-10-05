package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.inter.efficientia_mobile.R;

public class RouteDiaryStepSixActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_six);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepSixRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        findViewById(R.id.btnRouteStepSixBack).setOnClickListener(view -> finish());
        RouteSummaryRow.bind(findViewById(R.id.summaryGta), R.string.route_summary_gta, R.string.route_summary_gta_value);
        RouteSummaryRow.bind(findViewById(R.id.summaryOrigin), R.string.route_summary_origin, R.string.route_summary_origin_value);
        RouteSummaryRow.bind(findViewById(R.id.summaryDestination), R.string.route_summary_destination, R.string.route_summary_destination_value);
        RouteSummaryRow.bind(findViewById(R.id.summaryAnimals), R.string.route_summary_animals, R.string.route_summary_animals_value);
        RouteSummaryRow.bind(findViewById(R.id.summaryArrival), R.string.route_summary_arrival, R.string.route_summary_arrival_value);
        RouteSummaryRow.bind(findViewById(R.id.summaryDuration), R.string.route_summary_duration, R.string.route_summary_duration_value);
        RouteSummaryRow.bind(findViewById(R.id.summaryAnomalies), R.string.route_summary_anomalies, R.string.route_summary_anomalies_value);
        findViewById(R.id.btnRouteStepSixSubmit).setOnClickListener(view ->
                Toast.makeText(this, R.string.route_submit_front_only, Toast.LENGTH_LONG).show());
    }
}
