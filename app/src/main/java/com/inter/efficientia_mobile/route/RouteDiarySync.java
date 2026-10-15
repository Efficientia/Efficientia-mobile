package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
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
            syncError(activity, "Sessão expirada. Entre novamente para salvar o diário na API.",
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
                    if (id > 0 && response.code() == 404
                            && !"1".equals(draft.get("edit_pending"))) {
                        new AlertDialog.Builder(activity).setTitle("Rascunho não encontrado")
                                .setMessage("O diário #" + id + " não está mais disponível na API. Os dados preenchidos continuam neste aparelho. Deseja salvá-los como um novo diário?")
                                .setPositiveButton("Salvar como novo", (dialog, which) -> {
                                    draft.put("server_report_id", "");
                                    draft.put("submission_key", "");
                                    saveAndContinue(activity, draft, button, continueFlow);
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    } else {
                        syncError(activity, error(response, id > 0),
                                () -> saveAndContinue(activity, draft, button, continueFlow));
                    }
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable failure) {
                button.setEnabled(true);
                if (!activity.isFinishing() && !activity.isDestroyed())
                    syncError(activity,
                            "Sem conexão com a API. O preenchimento permanece neste aparelho, mas ainda não foi salvo no servidor.",
                            () -> saveAndContinue(activity, draft, button, continueFlow));
            }
        });
    }

    private static void syncError(Activity activity, String message, Runnable retry) {
        new AlertDialog.Builder(activity).setTitle("Rascunho não sincronizado")
                .setMessage(message + "\n\nNão é possível avançar até a API confirmar o salvamento.")
                .setPositiveButton("Tentar novamente", (dialog, which) -> retry.run())
                .setNegativeButton("Voltar ao formulário", null)
                .show();
    }

    private static String error(Response<?> response, boolean updating) {
        String message = "API recusou " + (updating ? "a atualização" : "a criação")
                + " do rascunho (HTTP " + response.code() + ").";
        if (response.code() == 404 && !updating)
            message += " Confira se a versão atual da API foi implantada.";
        ResponseBody body = response.errorBody();
        if (body != null) try {
            String raw = body.string().trim();
            if (!raw.isEmpty()) {
                String details = raw;
                try {
                    JsonElement parsed = new JsonParser().parse(raw);
                    if (parsed.isJsonObject()) {
                        JsonObject problem = parsed.getAsJsonObject();
                        StringBuilder readable = new StringBuilder();
                        for (String key : new String[]{"detail", "message", "title", "error"}) {
                            if (problem.has(key) && problem.get(key).isJsonPrimitive()) {
                                String value = problem.get(key).getAsString();
                                if (!value.isBlank() && readable.indexOf(value) < 0)
                                    readable.append("\n").append(value);
                            }
                        }
                        for (String key : new String[]{"fieldErrors", "errors", "violations"}) {
                            if (problem.has(key) && !problem.get(key).isJsonNull())
                                readable.append("\n").append(key).append(": ")
                                        .append(problem.get(key).toString());
                        }
                        if (readable.length() > 0) details = readable.toString().trim();
                    }
                } catch (RuntimeException ignored) { /* O corpo pode ser texto simples. */ }
                message += "\n" + (details.length() > 800 ? details.substring(0, 800) + "…" : details);
            }
        } catch (IOException ignored) { }
        if (response.code() == 400 && message.equals("API recusou "
                + (updating ? "a atualização" : "a criação") + " do rascunho (HTTP 400)."))
            message += " A API não informou qual campo foi rejeitado. Confira se a versão que aceita rascunhos parciais e locais digitados já foi implantada.";
        return message;
    }

    private static int parseId(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return -1; }
    }
}
