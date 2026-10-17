package com.inter.efficientia_mobile.main;

import android.content.Intent;
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
        findViewById(R.id.btnVehiclesBack).setOnClickListener(view -> ToolbarNavigation.back(this));
        findViewById(R.id.navVehiclesHome).setOnClickListener(view -> ToolbarNavigation.home(this));
        findViewById(R.id.navVehiclesProfile).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverProfileActivity.class));
        findViewById(R.id.navVehiclesSettings).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverSettingsActivity.class));
        findViewById(R.id.navVehiclesAssistant).setOnClickListener(view ->
                ToolbarNavigation.open(this, CinttiaActivity.class));
        findViewById(R.id.navVehiclesDocuments).setOnClickListener(view ->
                ToolbarNavigation.open(this, DocumentsActivity.class));
    }
}
