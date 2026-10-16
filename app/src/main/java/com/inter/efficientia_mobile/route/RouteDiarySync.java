package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.inter.efficientia_mobile.R;
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
            syncError(activity, activity.getString(R.string.draft_session_expired),
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
                        new AlertDialog.Builder(activity).setTitle(R.string.draft_not_found_title)
                                .setMessage(activity.getString(R.string.draft_not_found_message, id))
                                .setPositiveButton(R.string.draft_save_as_new, (dialog, which) -> {
                                    draft.put("server_report_id", "");
                                    draft.put("submission_key", "");
                                    saveAndContinue(activity, draft, button, continueFlow);
                                })
                                .setNegativeButton(R.string.main_home_delete_cancel, null)
                                .show();
                    } else {
                        syncError(activity, error(activity, response, id > 0),
                                () -> saveAndContinue(activity, draft, button, continueFlow));
                    }
                }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable failure) {
                button.setEnabled(true);
                if (!activity.isFinishing() && !activity.isDestroyed())
                    syncError(activity,
                            activity.getString(R.string.draft_offline),
                            () -> saveAndContinue(activity, draft, button, continueFlow));
            }
        });
    }

    private static void syncError(Activity activity, String message, Runnable retry) {
        new AlertDialog.Builder(activity).setTitle(R.string.draft_not_synced_title)
                .setMessage(message + "\n\n" + activity.getString(R.string.draft_cannot_advance))
                .setPositiveButton(R.string.draft_try_again, (dialog, which) -> retry.run())
                .setNegativeButton(R.string.draft_back_to_form, null)
                .show();
    }

    private static String error(Activity activity, Response<?> response, boolean updating) {
        String message = activity.getString(updating ? R.string.draft_api_rejected_update
                : R.string.draft_api_rejected_create, response.code());
        if (response.code() == 404 && !updating)
            message += " " + activity.getString(R.string.draft_check_api_version);
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
        if (response.code() == 400 && message.equals(activity.getString(updating
                ? R.string.draft_api_rejected_update : R.string.draft_api_rejected_create, 400)))
            message += " " + activity.getString(R.string.draft_no_field_error);
        return message;
    }

    private static int parseId(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return -1; }
    }
}
