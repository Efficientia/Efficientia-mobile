package com.inter.efficientia_mobile.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.CompanyService;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.splash.MainSplashActivity;

import java.io.File;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverProfileActivity extends AppCompatActivity {
    private Call<CompanyService.CompanyResponse> companyCall;
    private Uri pendingCameraUri;
    private File pendingCameraFile;
    private AlertDialog photoDialog;
    private ImageView dialogPhoto;
    private boolean photoBusy;

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), saved -> {
                if (saved && pendingCameraUri != null) savePhoto(pendingCameraUri);
                if (pendingCameraFile != null) pendingCameraFile.delete();
                pendingCameraFile = null;
                pendingCameraUri = null;
            });
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), selected -> {
                if (selected != null) savePhoto(selected);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String uri = savedInstanceState.getString("pending_camera_uri");
            String path = savedInstanceState.getString("pending_camera_path");
            if (uri != null) pendingCameraUri = Uri.parse(uri);
            if (path != null) pendingCameraFile = new File(path);
        }
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
        ProfilePhotoRemote.refresh(this, () -> {
            if (!isFinishing() && !isDestroyed()) refreshPhotos();
        });

        View.OnClickListener goHome = view -> ToolbarNavigation.home(this);
        findViewById(R.id.btnProfileTopHome).setOnClickListener(goHome);
        findViewById(R.id.navProfileHome).setOnClickListener(goHome);
        findViewById(R.id.btnDriverLogout).setOnClickListener(view -> {
            SessionManager.clear(this);
            returnToStart();
        });
        findViewById(R.id.btnEditDriverProfile).setOnClickListener(view -> showPhotoDialog());
        findViewById(R.id.imgDriverProfile).setOnClickListener(view -> showPhotoDialog());
        findViewById(R.id.btnDriverPreferences).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverSettingsActivity.class));
        findViewById(R.id.navProfileSettings).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverSettingsActivity.class));
        findViewById(R.id.btnProfileNotifications).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverNotificationsActivity.class));
        int[] pending = {
                R.id.navProfileAssistant
        };
        for (int id : pending) {
            findViewById(id).setOnClickListener(view ->
                    Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show());
        }
        findViewById(R.id.navProfileDocuments).setOnClickListener(view ->
                ToolbarNavigation.open(this, DocumentsActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (findViewById(R.id.imgDriverProfile) != null) {
            ProfilePhotoStore.show(this, findViewById(R.id.imgDriverProfile));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (pendingCameraUri != null) state.putString("pending_camera_uri", pendingCameraUri.toString());
        if (pendingCameraFile != null) state.putString("pending_camera_path", pendingCameraFile.getAbsolutePath());
        super.onSaveInstanceState(state);
    }

    private void showPhotoDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_edit_profile_photo, null);
        dialogPhoto = content.findViewById(R.id.imgCurrentProfilePhoto);
        ProfilePhotoStore.show(this, dialogPhoto);
        photoDialog = new AlertDialog.Builder(this).setView(content).create();
        content.findViewById(R.id.btnChangeProfilePhoto).setOnClickListener(view ->
                new AlertDialog.Builder(this)
                        .setItems(new String[]{getString(R.string.profile_photo_camera),
                                getString(R.string.profile_photo_gallery)}, (dialog, choice) -> {
                            if (choice == 0) openCamera();
                            else galleryLauncher.launch("image/*");
                        }).show());
        content.findViewById(R.id.btnRemoveProfilePhoto).setOnClickListener(view -> {
            if (photoBusy) return;
            setPhotoBusy(true);
            ProfilePhotoRemote.remove(this, success -> {
                if (isFinishing() || isDestroyed()) return;
                setPhotoBusy(false);
                if (success) {
                    refreshPhotos();
                    photoDialog.dismiss();
                } else {
                    Toast.makeText(this, R.string.profile_photo_sync_error, Toast.LENGTH_LONG).show();
                }
            });
        });
        photoDialog.show();
    }

    private void openCamera() {
        try {
            File directory = new File(getCacheDir(), "profile_camera");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("Camera cache unavailable");
            pendingCameraFile = File.createTempFile("driver_", ".jpg", directory);
            pendingCameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".profilephotos", pendingCameraFile);
            cameraLauncher.launch(pendingCameraUri);
        } catch (IOException | IllegalArgumentException error) {
            Toast.makeText(this, R.string.profile_photo_camera_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void savePhoto(Uri source) {
        try {
            if (photoBusy) return;
            byte[] photo = ProfilePhotoStore.prepare(this, source);
            setPhotoBusy(true);
            ProfilePhotoRemote.upload(this, photo, success -> {
                if (isFinishing() || isDestroyed()) return;
                setPhotoBusy(false);
                if (success) {
                    refreshPhotos();
                    if (photoDialog != null) photoDialog.dismiss();
                } else {
                    Toast.makeText(this, R.string.profile_photo_sync_error, Toast.LENGTH_LONG).show();
                }
            });
        } catch (IOException | RuntimeException error) {
            Toast.makeText(this, R.string.profile_photo_error, Toast.LENGTH_LONG).show();
        }
    }

    private void setPhotoBusy(boolean busy) {
        photoBusy = busy;
        if (photoDialog != null) {
            View edit = photoDialog.findViewById(R.id.btnChangeProfilePhoto);
            View remove = photoDialog.findViewById(R.id.btnRemoveProfilePhoto);
            if (edit != null) edit.setEnabled(!busy);
            if (remove != null) remove.setEnabled(!busy);
        }
    }

    private void refreshPhotos() {
        ProfilePhotoStore.show(this, findViewById(R.id.imgDriverProfile));
        if (dialogPhoto != null) ProfilePhotoStore.show(this, dialogPhoto);
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
