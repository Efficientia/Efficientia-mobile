package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

import com.inter.efficientia_mobile.R;

import java.util.List;

/** Lets the driver start a distinct trip or resume one of their local diaries. */
public final class RouteDiaryFlow {
    private RouteDiaryFlow() { }

    public static void open(Activity activity) {
        List<RouteDiaryDraft> saved = RouteDiaryDraft.saved(activity);
        if (saved.isEmpty()) {
            startNew(activity);
            return;
        }
        String[] choices = new String[saved.size() + 1];
        choices[0] = "Iniciar nova rota";
        for (int i = 0; i < saved.size(); i++) choices[i + 1] = "Continuar: " + saved.get(i).label();
        new AlertDialog.Builder(activity)
                .setTitle("Diário de rota")
                .setMessage("Escolha uma rota. Para encerrá-la, registre o desembarque e lance o diário na última etapa.")
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) {
                        new AlertDialog.Builder(activity)
                                .setTitle("Iniciar outra rota?")
                                .setMessage("Os rascunhos anteriores continuarão disponíveis para retomada neste aparelho.")
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Iniciar nova rota", (confirm, ignored) -> startNew(activity))
                                .show();
                    } else {
                        activity.startActivity(saved.get(which - 1).resume(activity));
                    }
                }).show();
    }

    public static void startNew(Activity activity) {
        RouteDiaryDraft draft = RouteDiaryDraft.startNew(activity);
        activity.startActivity(draft.next(activity, RouteDiaryStepOneActivity.class));
    }

    public static String currentRouteSummary(Context context) {
        String id = RouteDiaryDraft.activeId(context);
        if (id == null) return "Nenhuma rota em andamento";
        RouteDiaryDraft draft = RouteDiaryDraft.find(context, id);
        String origin = draft.get(R.id.inputRouteOrigin).trim();
        String destination = draft.get(R.id.inputRouteDestination).trim();
        if (origin.isEmpty() && destination.isEmpty()) return "Nova rota • Informe origem e destino";
        return "Em rota • " + (origin.isEmpty() ? "Origem pendente" : origin)
                + " → " + (destination.isEmpty() ? "Destino pendente" : destination);
    }
}
