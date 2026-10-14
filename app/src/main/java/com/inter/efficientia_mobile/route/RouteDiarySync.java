package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Saves each route stage remotely and keeps an explicit offline continuation. */
final class RouteDiarySync {
    private RouteDiarySync() { }

    static void saveAndContinue(Activity activity, RouteDiaryDraft draft, View button,
                                Runnable continueFlow) {
        String auth = SessionManager.authorizationHeader(activity);
        if (auth == null) {
            offlineDialog(activity, button, continueFlow, "Sessão expirada. O preenchimento ficou neste aparelho.",
                    () -> saveAndContinue(activity, draft, button, continueFlow));
            return;
        }
        button.setEnabled(false);
        RouteDiaryService api = RetrofitClient.getInstance().create(RouteDiaryService.class);
        JsonObject body = RouteDiaryPayload.from(draft,
                "1".equals(draft.get("edit_pending")) ? "pendente" : "rascunho");
        int id = parseId(draft.get("server_report_id"));
        Call<JsonObject> call = id > 0 ? api.update(auth, id, null, body) : api.create(auth, body);
        call.enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                button.setEnabled(true);
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().has("id")) {
                    int savedId = response.body().get("id").getAsInt();
                    draft.put("server_report_id", String.valueOf(savedId));
                    RouteDiaryReportIndex.remember(activity, savedId);
                    continueFlow.run();
                } else {
                    offlineDialog(activity, button, continueFlow, error(response),
                            () -> saveAndContinue(activity, draft, button, continueFlow));
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable failure) {
                button.setEnabled(true);
                if (!activity.isFinishing() && !activity.isDestroyed())
                    offlineDialog(activity, button, continueFlow,
                            "Sem conexão com a API. O preenchimento ficou neste aparelho.",
                            () -> saveAndContinue(activity, draft, button, continueFlow));
            }
        });
    }

    private static void offlineDialog(Activity activity, View button, Runnable continueFlow,
                                      String message, Runnable retry) {
        new AlertDialog.Builder(activity).setTitle("Rascunho não sincronizado")
                .setMessage(message)
                .setPositiveButton("Tentar novamente", (dialog, which) -> retry.run())
                .setNegativeButton("Continuar neste aparelho", (dialog, which) -> continueFlow.run())
                .show();
    }

    private static String error(Response<?> response) {
        String message = "API recusou o rascunho (HTTP " + response.code() + ").";
        ResponseBody body = response.errorBody();
        if (body != null) try {
            JsonObject problem = new JsonParser().parse(body.string()).getAsJsonObject();
            if (problem.has("detail")) message += " " + problem.get("detail").getAsString();
        } catch (IOException | RuntimeException ignored) { }
        return message;
    }

    private static int parseId(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return -1; }
    }
}
