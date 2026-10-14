package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;
import com.inter.efficientia_mobile.main.MainActivity;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Submit only after the server has confirmed the draft, all PNGs and the final status. */
final class RouteDiarySubmitter {
    private static final String[] ROLES = {"PECUARISTA", "MANOBRISTA", "CURRALEIRO"};
    private final Activity activity;
    private final RouteDiaryDraft draft;
    private final RouteDiaryService service;
    private final String authorization;
    private final View button;
    private final CharSequence originalButtonText;

    RouteDiarySubmitter(Activity activity, RouteDiaryDraft draft, View button) {
        this.activity = activity;
        this.draft = draft;
        this.button = button;
        this.originalButtonText = button instanceof TextView ? ((TextView) button).getText() : null;
        this.service = RetrofitClient.getInstance().create(RouteDiaryService.class);
        this.authorization = SessionManager.authorizationHeader(activity);
    }

    void submit() {
        if (authorization == null) {
            explain("Sua sessão expirou. Entre novamente antes de lançar o diário.");
            return;
        }
        if ((draft.get("fazenda_id").isEmpty() && draft.get(R.id.inputRouteOrigin).isBlank())
                || (draft.get("unidade_frigorifica_id").isEmpty()
                && draft.get(R.id.inputRouteDestination).isBlank())) {
            explain("Informe a origem e o destino da viagem: digite o nome ou selecione um local na busca.");
            return;
        }
        if (RouteDiaryPayload.hasIncompleteOccurrences(draft)) {
            explain("Complete a data, os horários e o motivo da parada; e a quantidade e descrição da anomalia, se houver. O diário continua salvo neste aparelho.");
            return;
        }
        button.setEnabled(false);
        if (button instanceof TextView) ((TextView) button).setText("Enviando…");
        int reportId = number(draft.get("server_report_id"));
        if ("1".equals(draft.get("edit_pending"))) {
            if (reportId <= 0) {
                fail("Não foi possível identificar o relatório para salvar a edição.");
                return;
            }
            service.update(authorization, reportId, null, RouteDiaryPayload.from(draft, "pendente"))
                    .enqueue(new Callback<JsonObject>() {
                        @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            if (!response.isSuccessful() || response.body() == null) { fail(response); return; }
                            restoreButton();
                            draft.clearActive();
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                Intent home = new Intent(activity, MainActivity.class);
                                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                activity.startActivity(home);
                                activity.finish();
                            }
                        }
                        @Override public void onFailure(Call<JsonObject> call, Throwable error) { fail(error); }
                    });
            return;
        }
        JsonObject body = RouteDiaryPayload.from(draft, "rascunho");
        if (reportId > 0) {
            service.update(authorization, reportId, null, body).enqueue(new Callback<JsonObject>() {
                @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) uploadNext(reportId, 0);
                    else fail(response);
                }
                @Override public void onFailure(Call<JsonObject> call, Throwable error) { fail(error); }
            });
        } else {
            service.create(authorization, body).enqueue(new Callback<JsonObject>() {
                @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    JsonObject saved = response.body();
                    if (!response.isSuccessful() || saved == null || !saved.has("id")) {
                        fail(response);
                        return;
                    }
                    int id = saved.get("id").getAsInt();
                    draft.put("server_report_id", String.valueOf(id));
                    RouteDiaryReportIndex.remember(activity, id);
                    uploadNext(id, 0);
                }
                @Override public void onFailure(Call<JsonObject> call, Throwable error) { fail(error); }
            });
        }
    }

    private void uploadNext(int reportId, int roleIndex) {
        if (roleIndex == ROLES.length) {
            sendFinal(reportId);
            return;
        }
        String role = ROLES[roleIndex];
        File image = new File(draft.get("signature_" + role));
        if (!image.isFile() && !draft.get("remote_signature_" + role).isEmpty()) {
            uploadNext(reportId, roleIndex + 1);
            return;
        }
        if (!image.isFile() || image.length() == 0 || image.length() > 1_048_576) {
            fail("A assinatura de " + role.toLowerCase() + " não foi encontrada ou ultrapassa 1 MB.");
            return;
        }
        RequestBody body = RequestBody.create(MediaType.parse("image/png"), image);
        MultipartBody.Part part = MultipartBody.Part.createFormData("arquivo", "assinatura-" + role.toLowerCase() + ".png", body);
        service.uploadSignature(authorization, reportId, role.toLowerCase(), part)
                .enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) uploadNext(reportId, roleIndex + 1);
                        else fail(response);
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable error) { fail(error); }
                });
    }

    private void sendFinal(int reportId) {
        String key = draft.get("submission_key");
        if (key.isEmpty()) {
            key = UUID.randomUUID().toString();
            draft.put("submission_key", key);
        }
        service.update(authorization, reportId, key, RouteDiaryPayload.from(draft, "pendente"))
                .enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            restoreButton();
                            draft.clearActive();
                            if (!activity.isFinishing() && !activity.isDestroyed())
                                activity.startActivity(new Intent(activity, RouteDiaryLaunchActivity.class));
                        } else fail(response);
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable error) { fail(error); }
                });
    }

    private void fail(Response<?> response) {
        String message = "A API recusou o diário (HTTP " + response.code() + ").";
        ResponseBody body = response.errorBody();
        if (body != null) {
            try {
                JsonObject error = new JsonParser().parse(body.string()).getAsJsonObject();
                if (error.has("detail")) message += " " + error.get("detail").getAsString();
                if (error.has("fieldErrors")) message += "\n" + error.get("fieldErrors").toString();
            } catch (IOException | RuntimeException ignored) { }
        }
        fail(message);
    }

    private void fail(Throwable error) {
        fail("Não foi possível conectar à API. O diário permanece neste aparelho para tentar novamente.");
    }

    private void fail(String message) {
        restoreButton();
        explain(message);
    }

    private void restoreButton() {
        button.setEnabled(true);
        if (button instanceof TextView && originalButtonText != null)
            ((TextView) button).setText(originalButtonText);
    }

    private void explain(String message) {
        if (!activity.isFinishing() && !activity.isDestroyed())
            new AlertDialog.Builder(activity).setTitle("Diário não lançado")
                    .setMessage(message).setPositiveButton("Entendi", null).show();
    }

    private static int number(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return -1; }
    }
}
