package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.signature.DriverSignatureRepository;
import com.inter.efficientia_mobile.signature.SignatureImageStore;

/** Stage six is a read-only review of signatures collected in their assigned stages. */
public class RouteDiaryStepFiveActivity extends AppCompatActivity {
    private RouteDiaryDraft draft;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_five);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepFiveRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.btnRouteStepFiveBack).setOnClickListener(view -> finish());
        renderRoles();

        if (SignatureImageStore.driverPath(this) == null
                && SessionManager.authorizationHeader(this) != null) {
            DriverSignatureRepository.fetch(this, (image, error) -> {
                if (image != null && !isFinishing() && !isDestroyed()) renderRoles();
                if (image != null) image.recycle();
            });
        }

        findViewById(R.id.btnRouteStepFiveContinue).setOnClickListener(view -> {
            String validation = RouteDiaryPayload.validationError(draft);
            if (!validation.isEmpty()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(R.string.route_check_form_title).setMessage(validation)
                        .setPositiveButton(R.string.dialog_understood, null).show();
                return;
            }
            if (!allSigned()) {
                Toast.makeText(this, R.string.route_signatures_required, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(draft.next(this, RouteDiaryStepSixActivity.class));
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (draft != null) {
            draft.put("current_stage", "6");
            renderRoles();
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        draft.saveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    private void renderRoles() {
        RouteStepSignature.bindReadOnly(this, draft, R.id.signatureRancher,
                R.string.route_rancher, RouteStepSignature.RANCHER);
        RouteStepSignature.bindReadOnly(this, draft, R.id.signatureDriver,
                R.string.route_driver, RouteStepSignature.DRIVER);
        RouteStepSignature.bindReadOnly(this, draft, R.id.signatureManeuverer,
                R.string.route_maneuverer, RouteStepSignature.MANEUVERER);
        RouteStepSignature.bindReadOnly(this, draft, R.id.signatureCorralWorker,
                R.string.route_corral_worker, RouteStepSignature.CORRAL_WORKER);
    }

    private boolean allSigned() {
        return RouteStepSignature.isSigned(this, draft, RouteStepSignature.RANCHER)
                && RouteStepSignature.isSigned(this, draft, RouteStepSignature.DRIVER)
                && RouteStepSignature.isSigned(this, draft, RouteStepSignature.MANEUVERER)
                && RouteStepSignature.isSigned(this, draft, RouteStepSignature.CORRAL_WORKER);
    }
}
