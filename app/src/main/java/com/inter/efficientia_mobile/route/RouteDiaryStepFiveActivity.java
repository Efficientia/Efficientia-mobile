package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.signature.SignatureCaptureActivity;
import com.inter.efficientia_mobile.signature.SignatureImageStore;
import com.inter.efficientia_mobile.signature.DriverSignatureRepository;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.signature.SignatureOptionsActivity;

import java.util.HashMap;
import java.util.Map;

public class RouteDiaryStepFiveActivity extends AppCompatActivity {
    private RouteDiaryDraft draft;
    private static final String RANCHER = "PECUARISTA";
    private static final String DRIVER = "MOTORISTA";
    private static final String MANEUVERER = "MANOBRISTA";
    private static final String CORRAL_WORKER = "CURRALEIRO";
    private final Map<String, String> routeSignatures = new HashMap<>();
    private final ActivityResultLauncher<Intent> signLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                String role = result.getData().getStringExtra(SignatureCaptureActivity.EXTRA_SIGNER_ROLE);
                String path = result.getData().getStringExtra(SignatureCaptureActivity.EXTRA_IMAGE_PATH);
                if (role == null || path == null) return;
                if (!DRIVER.equals(role)) {
                    routeSignatures.put(role, path);
                    draft.put("signature_" + role, path);
                }
                renderRoles();
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_five);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepFiveRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        findViewById(R.id.btnRouteStepFiveBack).setOnClickListener(view -> finish());
        if (savedInstanceState != null) {
            for (String role : new String[]{RANCHER, MANEUVERER, CORRAL_WORKER}) {
                String path = savedInstanceState.getString(role);
                if (path != null) routeSignatures.put(role, path);
            }
        }
        for (String role : new String[]{RANCHER, MANEUVERER, CORRAL_WORKER}) {
            if (!routeSignatures.containsKey(role)) {
                String path = draft.get("signature_" + role);
                if (!path.isEmpty()) routeSignatures.put(role, path);
            }
            String path = routeSignatures.get(role);
            String persistentPath = SignatureImageStore.keepRouteSignature(this, path);
            if (persistentPath != null && !persistentPath.equals(path)) {
                routeSignatures.put(role, persistentPath);
                draft.put("signature_" + role, persistentPath);
            }
        }
        renderRoles();
        if (SignatureImageStore.driverPath(this) == null
                && SessionManager.authorizationHeader(this) != null) {
            DriverSignatureRepository.fetch(this, (image, error) -> {
                if (image != null && !isFinishing() && !isDestroyed()) renderRoles();
                if (image != null) image.recycle();
            });
        }
        findViewById(R.id.btnRouteStepFiveContinue).setOnClickListener(view -> {
            if (!allSigned()) {
                Toast.makeText(this, R.string.route_signatures_required, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(draft.next(this, RouteDiaryStepSixActivity.class));
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (draft != null) renderRoles();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        draft.saveInstanceState(state);
        super.onSaveInstanceState(state);
        for (Map.Entry<String, String> entry : routeSignatures.entrySet()) {
            state.putString(entry.getKey(), entry.getValue());
        }
    }

    private void renderRoles() {
        configureSignature(R.id.signatureRancher, R.string.route_rancher, RANCHER);
        configureSignature(R.id.signatureDriver, R.string.route_driver, DRIVER);
        configureSignature(R.id.signatureManeuverer, R.string.route_maneuverer, MANEUVERER);
        configureSignature(R.id.signatureCorralWorker, R.string.route_corral_worker, CORRAL_WORKER);
    }

    private boolean allSigned() {
        if (SignatureImageStore.read(SignatureImageStore.driverPath(this)) == null) return false;
        for (String role : new String[]{RANCHER, MANEUVERER, CORRAL_WORKER}) {
            if (SignatureImageStore.read(routeSignatures.get(role)) == null) return false;
        }
        return true;
    }

    private void configureSignature(int containerId, int titleId, String role) {
        View row = findViewById(containerId);
        ((TextView) row.findViewById(R.id.signatureRole)).setText(titleId);
        String path = DRIVER.equals(role)
                ? SignatureImageStore.driverPath(this) : routeSignatures.get(role);
        Bitmap image = SignatureImageStore.read(path);
        boolean signed = image != null;
        row.setSelected(signed);
        ((TextView) row.findViewById(R.id.signatureStatus)).setText(signed
                ? R.string.route_signature_registered : R.string.route_signature_pending);
        TextView button = row.findViewById(R.id.btnSignRole);
        button.setText(signed ? R.string.route_resign_action : R.string.route_sign_action);
        button.setOnClickListener(view -> {
            Intent intent = new Intent(this, SignatureOptionsActivity.class);
            intent.putExtra(SignatureCaptureActivity.EXTRA_SIGNER_ROLE, role);
            signLauncher.launch(intent);
        });
        ImageView preview = row.findViewById(R.id.signatureImage);
        preview.setVisibility(signed ? View.VISIBLE : View.GONE);
        preview.setImageBitmap(image);
    }
}
