package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.signature.SignatureCaptureActivity;
import com.inter.efficientia_mobile.signature.SignatureImageStore;
import com.inter.efficientia_mobile.signature.SignatureOptionsActivity;

import java.io.File;

/** Binds a route participant signature to its assigned stage and the shared draft. */
final class RouteStepSignature {
    static final String RANCHER = "PECUARISTA";
    static final String MANEUVERER = "MANOBRISTA";
    static final String CORRAL_WORKER = "CURRALEIRO";
    static final String DRIVER = "MOTORISTA";

    private RouteStepSignature() { }

    static void bindCapture(Activity activity, RouteDiaryDraft draft, int containerId,
                           int titleId, String role, ActivityResultLauncher<Intent> launcher) {
        View row = activity.findViewById(containerId);
        ((TextView) row.findViewById(R.id.signatureRole)).setText(titleId);

        String path = draft.get("signature_" + role);
        String permanentPath = SignatureImageStore.keepRouteSignature(activity, path);
        if (permanentPath != null && !permanentPath.equals(path)) {
            path = permanentPath;
            draft.put("signature_" + role, path);
        }

        Bitmap image = SignatureImageStore.read(path);
        boolean signed = image != null || !draft.get("remote_signature_" + role).isEmpty();
        row.setSelected(signed);
        ((TextView) row.findViewById(R.id.signatureStatus)).setText(signed
                ? R.string.route_signature_registered : R.string.route_signature_pending);

        TextView button = row.findViewById(R.id.btnSignRole);
        button.setVisibility(View.VISIBLE);
        button.setText(signed ? R.string.route_resign_action : R.string.route_sign_action);
        button.setOnClickListener(view -> launchCapture(activity, role, launcher));

        ImageView preview = row.findViewById(R.id.signatureImage);
        preview.setVisibility(image == null ? View.GONE : View.VISIBLE);
        preview.setImageBitmap(image);
    }

    static void bindReadOnly(Activity activity, RouteDiaryDraft draft, int containerId,
                             int titleId, String role) {
        View row = activity.findViewById(containerId);
        ((TextView) row.findViewById(R.id.signatureRole)).setText(titleId);

        String path = DRIVER.equals(role)
                ? SignatureImageStore.driverPath(activity) : draft.get("signature_" + role);
        Bitmap image = SignatureImageStore.read(path);
        boolean signed = image != null || (!DRIVER.equals(role)
                && !draft.get("remote_signature_" + role).isEmpty());
        row.setSelected(signed);
        ((TextView) row.findViewById(R.id.signatureStatus)).setText(signed
                ? R.string.route_signature_registered : R.string.route_signature_pending);

        row.findViewById(R.id.btnSignRole).setVisibility(View.GONE);
        ImageView preview = row.findViewById(R.id.signatureImage);
        preview.setVisibility(image == null ? View.GONE : View.VISIBLE);
        preview.setImageBitmap(image);
    }

    static boolean saveResult(Activity activity, RouteDiaryDraft draft, Intent result, String expectedRole) {
        if (result == null || !expectedRole.equals(
                result.getStringExtra(SignatureCaptureActivity.EXTRA_SIGNER_ROLE))) return false;
        String path = result.getStringExtra(SignatureCaptureActivity.EXTRA_IMAGE_PATH);
        if (path == null || path.trim().isEmpty()) return false;
        String permanentPath = SignatureImageStore.keepRouteSignature(activity, path);
        draft.put("signature_" + expectedRole, permanentPath == null ? path : permanentPath);
        return true;
    }

    static boolean isSigned(Context context, RouteDiaryDraft draft, String role) {
        String path = DRIVER.equals(role)
                ? SignatureImageStore.driverPath(context) : draft.get("signature_" + role);
        boolean local = path != null && !path.trim().isEmpty() && new File(path).isFile();
        return local || (!DRIVER.equals(role) && !draft.get("remote_signature_" + role).isEmpty());
    }

    static void launchCapture(Activity activity, String role, ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(activity, SignatureOptionsActivity.class)
                .putExtra(SignatureCaptureActivity.EXTRA_SIGNER_ROLE, role);
        launcher.launch(intent);
    }
}
