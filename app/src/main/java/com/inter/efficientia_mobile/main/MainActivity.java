package com.inter.efficientia_mobile.main;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String firstName = SessionManager.firstName(this);
        TextView greeting = findViewById(R.id.txtHomeGreeting);
        TextView avatar = findViewById(R.id.txtProfileInitial);
        Button verifySubscription = findViewById(R.id.btnVerifySubscription);

        greeting.setText(getString(R.string.home_greeting, firstName));
        avatar.setText(firstName.substring(0, 1).toUpperCase());
        verifySubscription.setOnClickListener(view -> Toast.makeText(
                this,
                R.string.home_subscription_pending,
                Toast.LENGTH_SHORT
        ).show());
    }
}
