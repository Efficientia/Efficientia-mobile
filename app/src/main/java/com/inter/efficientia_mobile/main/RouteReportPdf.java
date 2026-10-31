package com.inter.efficientia_mobile.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.gson.JsonObject;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Downloads available report signatures and opens the formatted route report PDF. */
final class RouteReportPdf {
    private static final String[] ROLES = {"pecuarista", "motorista", "manobrista", "curraleiro"};

    private RouteReportPdf() { }

    static void open(Activity activity, JsonObject report, String authorization) {
        Toast.makeText(activity, R.string.pdf_preparing, Toast.LENGTH_SHORT).show();
        RouteDiaryService api = RetrofitClient.getInstance().create(RouteDiaryService.class);
        fetchSignature(activity, api, report, authorization, new HashMap<>(), new ArrayList<>(), 0);
    }

    private static void fetchSignature(Activity activity, RouteDiaryService api, JsonObject report,
                                       String authorization, Map<String, Bitmap> images,
                                       List<String> missing, int index) {
        if (index == ROLES.length) {
            if (!missing.isEmpty()) {
                if (!activity.isFinishing()) new AlertDialog.Builder(activity)
                        .setTitle("Assinaturas indisponíveis")
                        .setMessage("Não foi possível carregar as assinaturas registradas: "
                                + String.join(", ", missing) + ". Tente novamente antes de baixar o PDF.")
                        .setPositiveButton(R.string.dialog_understood, null).show();
                for (Bitmap bitmap : images.values()) bitmap.recycle();
                return;
            }
            try {
                File pdf = RouteReportFormPdf.create(activity, report, images);
                Uri uri = saveToDownloads(activity, pdf);
                Intent view = new Intent(Intent.ACTION_VIEW);
                view.setDataAndType(uri, "application/pdf");
                view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(Intent.createChooser(view, activity.getString(R.string.pdf_open_chooser)));
            } catch (IOException | RuntimeException error) {
                if (!activity.isFinishing()) new AlertDialog.Builder(activity)
                        .setTitle(R.string.pdf_open_error_title)
                        .setMessage(R.string.pdf_open_error_message)
                        .setPositiveButton(R.string.dialog_understood, null).show();
            } finally {
                for (Bitmap bitmap : images.values()) bitmap.recycle();
            }
            return;
        }
        String role = ROLES[index];
        String field = "urlAssinatura" + Character.toUpperCase(role.charAt(0)) + role.substring(1);
        String url = report.has(field) && !report.get(field).isJsonNull()
                ? report.get(field).getAsString().trim() : "";
        int id = report.get("id").getAsInt();
        // The API can point to a document or to the driver's saved signature.
        // Only same-origin API paths are accepted, so the token cannot be sent elsewhere.
        String path = url.startsWith("/api/v1/") && !url.contains("?") && !url.contains("#")
                ? url.substring(1) : "";
        Call<ResponseBody> call = path.isEmpty()
                ? api.signature(authorization, id, role)
                : api.signatureContent(authorization, path);
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                Bitmap bitmap = null;
                if (response.isSuccessful() && response.body() != null) {
                    try (ResponseBody body = response.body()) {
                        bitmap = BitmapFactory.decodeStream(body.byteStream());
                    }
                }
                if (bitmap != null) {
                    images.put(role, bitmap);
                } else if (!path.isEmpty()) {
                    // Older report links can be stale; the report endpoint resolves current content.
                    fetchFromReport(activity, api, report, authorization, images, missing, index, true);
                    return;
                } else if (!url.isEmpty()) {
                    missing.add(role);
                }
                fetchSignature(activity, api, report, authorization, images, missing, index + 1);
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable error) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (!path.isEmpty()) {
                    fetchFromReport(activity, api, report, authorization, images, missing, index, true);
                } else {
                    if (!url.isEmpty()) missing.add(role);
                    fetchSignature(activity, api, report, authorization, images, missing, index + 1);
                }
            }
        });
    }

    private static void fetchFromReport(Activity activity, RouteDiaryService api, JsonObject report,
                                        String authorization, Map<String, Bitmap> images,
                                        List<String> missing, int index, boolean expected) {
        String role = ROLES[index];
        api.signature(authorization, report.get("id").getAsInt(), role)
                .enqueue(new Callback<ResponseBody>() {
                    @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        Bitmap bitmap = null;
                        if (response.isSuccessful() && response.body() != null) {
                            try (ResponseBody body = response.body()) {
                                bitmap = BitmapFactory.decodeStream(body.byteStream());
                            }
                        }
                        if (bitmap != null) images.put(role, bitmap);
                        else if (expected) missing.add(role);
                        fetchSignature(activity, api, report, authorization, images, missing, index + 1);
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable error) {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (expected) missing.add(role);
                        fetchSignature(activity, api, report, authorization, images, missing, index + 1);
                    }
                });
    }

    private static Uri saveToDownloads(Activity activity, File pdf) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".profilephotos", pdf);
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, pdf.getName());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/Efficientia");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri uri = activity.getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Não foi possível salvar o PDF em Downloads.");
        try {
            try (InputStream input = new FileInputStream(pdf);
                 OutputStream output = activity.getContentResolver().openOutputStream(uri)) {
                if (output == null) throw new IOException("Destino do PDF indisponível.");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (activity.getContentResolver().update(uri, values, null, null) != 1)
                throw new IOException("Não foi possível concluir o download do PDF.");
            return uri;
        } catch (IOException | RuntimeException error) {
            activity.getContentResolver().delete(uri, null, null);
            throw error;
        }
    }
}
