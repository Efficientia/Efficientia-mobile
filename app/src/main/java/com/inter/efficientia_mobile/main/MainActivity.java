package com.inter.efficientia_mobile.main;

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
import com.inter.efficientia_mobile.auth.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String userName = SessionManager.userName(this);
        if (userName.isBlank()) {
            userName = "Motorista";
        }
        TextView greeting = findViewById(R.id.txtMainHomeGreeting);
        greeting.setText(getString(R.string.main_home_greeting, userName));

        int[] pendingActions = {
                R.id.btnHomeNotifications,
                R.id.btnHomeProfile,
                R.id.btnMainReports,
                R.id.btnMainFeedback,
                R.id.btnMainTruck,
                R.id.btnMainAdd,
                R.id.navMainProfile,
                R.id.navMainDocuments,
                R.id.navMainAssistant,
                R.id.navMainSettings
        };
        for (int actionId : pendingActions) {
            findViewById(actionId).setOnClickListener(this::showPendingFeature);
        }
    }

    private void showPendingFeature(View ignored) {
        Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show();
    }
}
