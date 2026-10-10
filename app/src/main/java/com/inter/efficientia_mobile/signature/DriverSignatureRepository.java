package com.inter.efficientia_mobile.signature;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.DriverSignatureService;
import com.inter.efficientia_mobile.network.RetrofitClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class DriverSignatureRepository {
    private static final String PREFS = "driver_signature_upload";
    private static final int MAX_BYTES = 1024 * 1024;

    public interface Result {
        void complete(Bitmap image, String error);
    }

    public interface FetchResult {
        void complete(Bitmap image, int statusCode, String error);
    }

    private DriverSignatureRepository() { }

    public static void upload(Context context, Bitmap image, boolean drawn, String name, Result result) {
        String authorization = SessionManager.authorizationHeader(context);
        if (authorization == null) {
            result.complete(null, "Entre com sua conta de motorista para salvar a assinatura no banco.");
            return;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!image.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            result.complete(null, "Não foi possível gerar o PNG da assinatura.");
            return;
        }
        byte[] bytes = output.toByteArray();
        if (bytes.length > MAX_BYTES) {
            result.complete(null, "A assinatura ultrapassa o limite de 1 MB da API. Desenhe novamente.");
            return;
        }
        try {
            String metadata = new JSONObject()
                    .put("modalidade", drawn ? "DESENHO" : "NOME_DIGITADO")
                    .put("textoOrigem", drawn ? JSONObject.NULL : name)
                    .toString();
            String payloadHash = sha256(bytes, metadata);
            String preferenceKey = "upload_" + SessionManager.userId(context);
            String previous = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(preferenceKey, "");
            String idempotencyKey = previous.startsWith(payloadHash + ":")
                    ? previous.substring(payloadHash.length() + 1) : UUID.randomUUID().toString();
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(preferenceKey, payloadHash + ":" + idempotencyKey).apply();
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/png"), bytes);
            MultipartBody.Part file = MultipartBody.Part.createFormData("arquivo", "assinatura.png", fileBody);
            RequestBody metadataBody = RequestBody.create(MediaType.parse("application/json"), metadata);
            RetrofitClient.getInstance().create(DriverSignatureService.class)
                    .save(authorization, idempotencyKey, file, metadataBody)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (!response.isSuccessful()) {
                                if (response.code() == 409) {
                                    // A mesma chave pode ter sido consumida por uma tentativa anterior
                                    // cuja resposta falhou. Uma nova ação do usuário deve usar outra UUID.
                                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                            .remove(preferenceKey).apply();
                                }
                                result.complete(null, uploadError(response));
                                return;
                            }
                            try {
                                SignatureImageStore.saveDriver(context, image);
                            } catch (IOException exception) {
                                // A API confirmou a persistência; a cópia local é apenas um cache.
                            }
                            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                    .remove(preferenceKey).apply();
                            SessionManager.markDriverSignatureRegistered(context);
                            result.complete(image, null);
                        }
                        @Override public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                            result.complete(null, "Sem conexão com a API. Tente novamente para reenviar a assinatura.");
                        }
                    });
        } catch (JSONException | NoSuchAlgorithmException exception) {
            result.complete(null, "Não foi possível preparar a assinatura para envio.");
        }
    }

    public static void fetch(Context context, Result result) {
        fetchWithStatus(context, (image, statusCode, error) -> result.complete(image, error));
    }

    public static void fetchWithStatus(Context context, FetchResult result) {
        String authorization = SessionManager.authorizationHeader(context);
        if (authorization == null) {
            result.complete(null, 401, "Autenticação necessária para consultar a assinatura.");
            return;
        }
        RetrofitClient.getInstance().create(DriverSignatureService.class)
                .content(authorization).enqueue(new Callback<ResponseBody>() {
                    @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            result.complete(null, response.code(),
                                    "Não foi possível consultar a assinatura (HTTP " + response.code() + ").");
                            return;
                        }
                        try {
                            byte[] bytes = response.body().bytes();
                            Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            if (image == null) throw new IOException("PNG inválido");
                            try {
                                SignatureImageStore.saveDriver(context, image);
                            } catch (IOException ignored) {
                                // O PNG já foi confirmado pela API; o arquivo privado é só um cache.
                            }
                            SessionManager.markDriverSignatureRegistered(context);
                            result.complete(image, 200, null);
                        } catch (IOException exception) {
                            result.complete(null, 200,
                                    "Não foi possível ler a assinatura retornada pela API.");
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                        result.complete(null, 0, "Sem conexão para consultar a assinatura.");
                    }
                });
    }

    private static String sha256(byte[] bytes, String metadata) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes);
        digest.update(metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte part : digest.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", part & 0xff));
        return hex.toString();
    }

    private static String uploadError(Response<ResponseBody> response) {
        String prefix = "API não salvou a assinatura (HTTP " + response.code() + ").";
        if (response.errorBody() == null) return prefix;
        try {
            String body = response.errorBody().string();
            JSONObject problem = new JSONObject(body);
            String detail = problem.optString("detail", "").trim();
            if (detail.isEmpty()) detail = problem.optString("message", "").trim();
            if (detail.isEmpty()) detail = problem.optString("title", "").trim();
            if (detail.isEmpty()) return prefix;
            return prefix + " " + detail;
        } catch (IOException | JSONException ignored) {
            return prefix;
        }
    }
}
