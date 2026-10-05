package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.inter.efficientia_mobile.R;

public class RouteDiaryStepFourActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_four);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepFourRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        findViewById(R.id.btnRouteStepFourBack).setOnClickListener(view -> finish());
        findViewById(R.id.btnAddUnexpectedStop).setOnClickListener(view ->
                Toast.makeText(this, R.string.route_stop_added, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnRouteStepFourContinue).setOnClickListener(view ->
                startActivity(new Intent(this, RouteDiaryStepFiveActivity.class)));
    }
}
