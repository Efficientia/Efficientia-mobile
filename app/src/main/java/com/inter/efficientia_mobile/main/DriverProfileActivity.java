package com.inter.efficientia_mobile.main;

import android.content.Intent;
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
import com.inter.efficientia_mobile.network.CompanyService;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.splash.MainSplashActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverProfileActivity extends AppCompatActivity {
    private Call<CompanyService.CompanyResponse> companyCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SessionManager.authorizationHeader(this) == null) {
            returnToStart();
            return;
        }
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_driver_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        String name = SessionManager.userName(this);
        ((TextView) findViewById(R.id.txtDriverProfileGreeting))
                .setText(getString(R.string.main_home_greeting,
                        name.isBlank() ? getString(R.string.profile_driver) : name));
        String category = SessionManager.userCnhCategory(this);
        ((TextView) findViewById(R.id.txtDriverProfileSubtitle))
                .setText(category.isBlank() ? getString(R.string.profile_driver)
                        : getString(R.string.profile_driver_category, category));

        bindField(R.id.profileCpfField, R.string.profile_cpf,
                formatCpf(SessionManager.userCpf(this)));
        String cnh = SessionManager.userCnhNumber(this);
        if (!cnh.isBlank() && !category.isBlank()) {
            cnh = getString(R.string.profile_cnh_value, cnh, category);
        }
        bindField(R.id.profileCnhField, R.string.profile_cnh, cnh);
        bindField(R.id.profilePhoneField, R.string.profile_phone,
                formatPhone(SessionManager.userPhone(this)));
        bindField(R.id.profileCompanyField, R.string.profile_company,
                getString(R.string.profile_loading));
        bindField(R.id.profileEmailField, R.string.profile_email,
                SessionManager.userEmail(this));
        loadCompany();

        View.OnClickListener goHome = view -> finish();
        findViewById(R.id.btnProfileTopHome).setOnClickListener(goHome);
        findViewById(R.id.navProfileHome).setOnClickListener(goHome);
        findViewById(R.id.btnDriverLogout).setOnClickListener(view -> {
            SessionManager.clear(this);
            returnToStart();
        });
        findViewById(R.id.btnEditDriverProfile).setOnClickListener(view ->
                Toast.makeText(this, R.string.profile_edit_pending, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnDriverPreferences).setOnClickListener(view ->
                Toast.makeText(this, R.string.profile_preferences_pending, Toast.LENGTH_SHORT).show());
        int[] pending = {
                R.id.btnProfileNotifications,
                R.id.navProfileDocuments,
                R.id.navProfileAssistant,
                R.id.navProfileSettings
        };
        for (int id : pending) {
            findViewById(id).setOnClickListener(view ->
                    Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show());
        }
    }

    private void bindField(int containerId, int labelId, String value) {
        View field = findViewById(containerId);
        ((TextView) field.findViewById(R.id.profileFieldLabel)).setText(labelId);
        ((TextView) field.findViewById(R.id.profileFieldValue)).setText(
                value == null || value.isBlank() ? getString(R.string.profile_not_informed) : value);
    }

    private void loadCompany() {
        long companyId = SessionManager.companyId(this);
        if (companyId <= 0) {
            bindField(R.id.profileCompanyField, R.string.profile_company, "");
            return;
        }
        companyCall = RetrofitClient.getInstance().create(CompanyService.class)
                .getById(SessionManager.authorizationHeader(this), companyId);
        companyCall.enqueue(new Callback<CompanyService.CompanyResponse>() {
            @Override
            public void onResponse(Call<CompanyService.CompanyResponse> call,
                                   Response<CompanyService.CompanyResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.code() == 401) {
                    SessionManager.clear(DriverProfileActivity.this);
                    returnToStart();
                    return;
                }
                String name = response.isSuccessful() && response.body() != null
                        ? response.body().displayName() : "";
                bindField(R.id.profileCompanyField, R.string.profile_company, name);
            }

            @Override
            public void onFailure(Call<CompanyService.CompanyResponse> call, Throwable error) {
                if (!isFinishing() && !isDestroyed()) {
                    bindField(R.id.profileCompanyField, R.string.profile_company, "");
                }
            }
        });
    }

    private String formatCpf(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11) return value;
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "."
                + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    private String formatPhone(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return "(" + digits.substring(0, 2) + ") " + digits.substring(2, 7)
                    + "-" + digits.substring(7);
        }
        if (digits.length() == 10) {
            return "(" + digits.substring(0, 2) + ") " + digits.substring(2, 6)
                    + "-" + digits.substring(6);
        }
        return value;
    }

    private void returnToStart() {
        Intent intent = new Intent(this, MainSplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (companyCall != null) companyCall.cancel();
        super.onDestroy();
    }
}
