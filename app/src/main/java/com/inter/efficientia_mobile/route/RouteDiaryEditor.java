package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;

import com.google.gson.JsonObject;

/** Opens a server report in the existing multi-step form. */
public final class RouteDiaryEditor {
    private RouteDiaryEditor() { }

    public static void open(Activity activity, JsonObject report) {
        String status = report.has("status") && !report.get("status").isJsonNull()
                ? report.get("status").getAsString().toLowerCase() : "";
        if (!"rascunho".equals(status) && !"pendente".equals(status)) {
            new AlertDialog.Builder(activity).setTitle("Edição indisponível")
                    .setMessage("Este diário já foi analisado e não pode mais ser alterado.")
                    .setPositiveButton("Entendi", null).show();
            return;
        }
        RouteDiaryDraft draft = RouteDiaryDraft.fromReport(activity, report);
        activity.startActivity(draft.next(activity, RouteDiaryStepOneActivity.class));
    }
}
