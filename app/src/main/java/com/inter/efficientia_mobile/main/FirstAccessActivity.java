package com.inter.efficientia_mobile.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.signature.SignatureOptionsActivity;

public class FirstAccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_access);

        String firstName = SessionManager.firstName(this);
        TextView greeting = findViewById(R.id.txtHomeGreeting);
        Button verifySubscription = findViewById(R.id.btnVerifySubscription);

        greeting.setText(getString(R.string.home_greeting, firstName));
        verifySubscription.setOnClickListener(view -> startActivity(
                new Intent(this, SignatureOptionsActivity.class)
        ));
    }
}
