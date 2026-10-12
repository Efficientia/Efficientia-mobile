package com.inter.efficientia_mobile.main;

import android.content.Context;

import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.DriverPhotoService;
import com.inter.efficientia_mobile.network.RetrofitClient;

import java.io.IOException;
import java.nio.file.Files;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class ProfilePhotoRemote {
    interface Result { void done(boolean success); }

    private static int version;
    private static final String SYNC_PREFS = "driver_profile_photo_sync";

    private ProfilePhotoRemote() { }

    static void refresh(Context context, Runnable updated) {
        String auth = SessionManager.authorizationHeader(context);
        int userId = SessionManager.userId(context);
        if (auth == null || userId <= 0) return;
        int requestVersion = ++version;
        Context app = context.getApplicationContext();
        service().content(auth).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (requestVersion != version || SessionManager.userId(app) != userId) return;
                if (response.isSuccessful() && response.body() != null) {
                    try (ResponseBody body = response.body()) {
                        ProfilePhotoStore.saveBytes(app, body.bytes());
                        markSynced(app, userId);
                        updated.run();
                    } catch (IOException ignored) { }
                } else if (response.code() == 404) {
                    if (!wasSynced(app, userId) && ProfilePhotoStore.photoFile(app).isFile()) {
                        try {
                            upload(app, Files.readAllBytes(ProfilePhotoStore.photoFile(app).toPath()),
                                    success -> { if (success) updated.run(); });
                        } catch (IOException ignored) { }
                    } else {
                        ProfilePhotoStore.remove(app);
                        markSynced(app, userId);
                        updated.run();
                    }
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable error) { }
        });
    }

    static void upload(Context context, byte[] jpeg, Result result) {
        String auth = SessionManager.authorizationHeader(context);
        int userId = SessionManager.userId(context);
        if (auth == null || userId <= 0) { result.done(false); return; }
        int requestVersion = ++version;
        Context app = context.getApplicationContext();
        RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), jpeg);
        MultipartBody.Part part = MultipartBody.Part.createFormData("arquivo", "foto-perfil.jpg", body);
        service().save(auth, part).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (requestVersion != version || SessionManager.userId(app) != userId) return;
                boolean success = response.isSuccessful();
                if (success) {
                    try {
                        ProfilePhotoStore.saveBytes(app, jpeg);
                        markSynced(app, userId);
                    } catch (IOException error) { success = false; }
                }
                result.done(success);
            }
            @Override public void onFailure(Call<Void> call, Throwable error) {
                if (requestVersion == version && SessionManager.userId(app) == userId) result.done(false);
            }
        });
    }

    static void remove(Context context, Result result) {
        String auth = SessionManager.authorizationHeader(context);
        int userId = SessionManager.userId(context);
        if (auth == null || userId <= 0) { result.done(false); return; }
        int requestVersion = ++version;
        Context app = context.getApplicationContext();
        service().remove(auth).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (requestVersion != version || SessionManager.userId(app) != userId) return;
                boolean success = response.isSuccessful();
                if (success) {
                    success = ProfilePhotoStore.remove(app);
                    markSynced(app, userId);
                }
                result.done(success);
            }
            @Override public void onFailure(Call<Void> call, Throwable error) {
                if (requestVersion == version && SessionManager.userId(app) == userId) result.done(false);
            }
        });
    }

    private static DriverPhotoService service() {
        return RetrofitClient.getInstance().create(DriverPhotoService.class);
    }

    private static boolean wasSynced(Context context, int userId) {
        return context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
                .getBoolean(String.valueOf(userId), false);
    }

    private static void markSynced(Context context, int userId) {
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(String.valueOf(userId), true).apply();
    }
}
