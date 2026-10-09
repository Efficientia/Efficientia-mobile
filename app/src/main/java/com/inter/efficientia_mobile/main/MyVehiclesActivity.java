package com.inter.efficientia_mobile.main;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;

/** Apresentação demonstrativa até a integração dos veículos e viagens da empresa. */
public class MyVehiclesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_my_vehicles);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myVehiclesRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.btnVehiclesBack).setOnClickListener(view -> finish());
        findViewById(R.id.navVehiclesHome).setOnClickListener(view -> finish());
        int[] pending = {
                R.id.navVehiclesProfile, R.id.navVehiclesDocuments,
                R.id.navVehiclesAssistant, R.id.navVehiclesSettings
        };
        for (int id : pending) {
            findViewById(id).setOnClickListener(view -> Toast.makeText(
                    this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show());
        }
    }
}
