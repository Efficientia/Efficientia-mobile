package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.inter.efficientia_mobile.auth.SessionManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.inter.efficientia_mobile.R;

import java.util.UUID;

/** Local form draft shared by the route diary's separate Activity screens. */
final class RouteDiaryDraft {
    private static final String EXTRA_ID = "route_diary_draft_id";
    private final Context context;
    private final String id;
    private final SharedPreferences preferences;

    private RouteDiaryDraft(Context context, String id) {
        this.context = context;
        this.id = id;
        this.preferences = context.getSharedPreferences("route_diary_draft_" + id, Context.MODE_PRIVATE);
    }

    static RouteDiaryDraft open(Activity activity, Bundle savedState) {
        String id = savedState == null ? null : savedState.getString(EXTRA_ID);
        if (id == null) id = activity.getIntent().getStringExtra(EXTRA_ID);
        SharedPreferences active = activity.getSharedPreferences("route_diary_active", Context.MODE_PRIVATE);
        String activeKey = "driver_" + SessionManager.userId(activity);
        if (id == null) id = active.getString(activeKey, null);
        if (id == null) id = UUID.randomUUID().toString();
        active.edit().putString(activeKey, id).apply();
        activity.getIntent().putExtra(EXTRA_ID, id);
        return new RouteDiaryDraft(activity, id);
    }

    static RouteDiaryDraft fromReport(Activity activity, JsonObject report) {
        int reportId = report.get("id").getAsInt();
        String id = "report-" + SessionManager.userId(activity) + "-" + reportId;
        RouteDiaryDraft draft = new RouteDiaryDraft(activity, id);
        if (!String.valueOf(reportId).equals(draft.get("server_report_id"))) {
            draft.preferences.edit().clear().apply();
            draft.put("server_report_id", String.valueOf(reportId));
            draft.put("fazenda_id", value(report, "fazendaId"));
            draft.put("unidade_frigorifica_id", value(report, "unidadeFrigorificaId"));
            draft.put(R.id.inputRouteOrigin, value(report, "nomeOrigem"));
            draft.put(R.id.inputRouteDestination, value(report, "nomeDestino"));
            draft.put(R.id.inputRouteGta, value(report, "numeroGta"));
            draft.put(R.id.inputRouteInvoice, value(report, "numeroNotaFiscal"));
            draft.put(R.id.inputRouteBoardingDate, displayDate(value(report, "dataEmbarque")));
            draft.put(R.id.inputRouteBoardingTime, displayTime(value(report, "horarioEmbarque")));
            draft.put(R.id.inputRouteDepartureTime, displayTime(value(report, "horarioSaidaPropriedade")));
            draft.put(R.id.inputRouteDepartureKm, value(report, "kmSaidaEmbarcadouro"));
            draft.put(R.id.inputRouteArrivalDate, displayDate(value(report, "dataChegadaUnidade")));
            draft.put(R.id.inputRouteArrivalTime, displayTime(value(report, "horarioChegadaUnidade")));
            draft.put(R.id.inputRouteUnloadingTime, displayTime(value(report, "horarioDesembarque")));
            draft.put(R.id.inputRouteArrivalKm, value(report, "kmChegadaDesembarcadouro"));
            draft.put(R.id.inputRouteCorral, value(report, "numeroCurral"));
            draft.put("reverse_alarm", report.has("sireneReFuncionou") && !report.get("sireneReFuncionou").isJsonNull()
                    ? (report.get("sireneReFuncionou").getAsBoolean() ? "yes" : "no") : "");
            counter(draft, R.id.counterMales, report, "quantidadeMachos");
            counter(draft, R.id.counterFemales, report, "quantidadeFemeas");
            counter(draft, R.id.counterMarrucos, report, "quantidadeMarrucos");
            counter(draft, R.id.counterStanding, report, "quantidadeEmPe");
            counter(draft, R.id.counterLying, report, "quantidadeDeitado");
            counter(draft, R.id.counterDead, report, "quantidadeMorto");
            counter(draft, R.id.counterEmergency, report, "quantidadeEmergencia");
            draft.put(R.id.inputRouteIncidentReason, value(report, "motivoEmergencia"));
            draft.put(R.id.inputRouteArrivalComments, value(report, "comentarios"));
            JsonArray stops = array(report, "paradasImprevistas");
            draft.put("original_stops", stops.toString());
            if (stops.size() > 0 && stops.get(0).isJsonObject()) {
                JsonObject stop = stops.get(0).getAsJsonObject();
                draft.put(R.id.inputRouteStopReason, first(value(stop, "descricaoMotivo"), value(stop, "motivo")));
                String start = value(stop, "dataHoraInicio");
                draft.put(R.id.inputRouteStopDate, displayDate(start.length() >= 10 ? start.substring(0, 10) : ""));
                draft.put(R.id.inputRouteStopStart, displayTime(start.length() >= 16 ? start.substring(11) : ""));
                String end = value(stop, "dataHoraFim");
                draft.put(R.id.inputRouteStopEnd, displayTime(end.length() >= 16 ? end.substring(11) : ""));
                draft.put("original_stop_display", draft.get(R.id.inputRouteStopReason) + "|"
                        + draft.get(R.id.inputRouteStopDate) + "|" + draft.get(R.id.inputRouteStopStart)
                        + "|" + draft.get(R.id.inputRouteStopEnd));
            }
            JsonArray anomalies = array(report, "anomaliasEmbarque");
            draft.put("original_anomalies", anomalies.toString());
            if (anomalies.size() > 0 && anomalies.get(0).isJsonObject()) {
                JsonObject anomaly = anomalies.get(0).getAsJsonObject();
                draft.put(R.id.inputRouteAnomalyAnimals, value(anomaly, "quantidadeAnimais"));
                draft.put(R.id.inputRouteAnomalyDescription, first(value(anomaly, "descricaoOutros"), value(anomaly, "anomalia")));
                draft.put("original_anomaly_display", draft.get(R.id.inputRouteAnomalyAnimals)
                        + "|" + draft.get(R.id.inputRouteAnomalyDescription));
            }
            for (String role : new String[]{"PECUARISTA", "MANOBRISTA", "CURRALEIRO"})
                draft.put("remote_signature_" + role, value(report, "urlAssinatura" + role.charAt(0) + role.substring(1).toLowerCase()));
            draft.put("edit_pending", "pendente".equalsIgnoreCase(value(report, "status")) ? "1" : "");
        }
        activity.getSharedPreferences("route_diary_active", Context.MODE_PRIVATE).edit()
                .putString("driver_" + SessionManager.userId(activity), id).apply();
        return draft;
    }

    private static void counter(RouteDiaryDraft draft, int id, JsonObject report, String field) {
        draft.put(id, first(value(report, field), "0"));
    }

    private static JsonArray array(JsonObject report, String field) {
        return report.has(field) && report.get(field).isJsonArray()
                ? report.getAsJsonArray(field) : new JsonArray();
    }

    private static String first(String first, String fallback) { return first.isEmpty() ? fallback : first; }
    private static String value(JsonObject object, String field) {
        return object.has(field) && !object.get(field).isJsonNull() ? object.get(field).getAsString() : "";
    }
    private static String displayDate(String iso) {
        return iso.matches("\\d{4}-\\d{2}-\\d{2}") ? iso.substring(8) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4) : "";
    }
    private static String displayTime(String iso) { return iso.length() >= 5 ? iso.substring(0, 5) : ""; }

    void clearActive() {
        context.getSharedPreferences("route_diary_active", Context.MODE_PRIVATE)
                .edit().remove("driver_" + SessionManager.userId(context)).apply();
        preferences.edit().clear().apply();
    }

    void saveInstanceState(Bundle state) { state.putString(EXTRA_ID, id); }

    Intent next(Activity activity, Class<? extends Activity> target) {
        return new Intent(activity, target).putExtra(EXTRA_ID, id);
    }

    String get(int viewId) { return get(key(viewId)); }
    String get(String key) { return preferences.getString(key, ""); }
    void put(String key, String value) { preferences.edit().putString(key, value).apply(); }
    void put(int viewId, String value) { put(key(viewId), value); }

    void restoreText(Activity activity, int... ids) {
        for (int id : ids) ((EditText) activity.findViewById(id)).setText(get(id));
    }

    void saveText(Activity activity, int... ids) {
        SharedPreferences.Editor editor = preferences.edit();
        for (int id : ids) {
            EditText input = activity.findViewById(id);
            editor.putString(key(id), input.getText().toString().trim());
        }
        editor.apply();
    }

    void restoreCounter(Activity activity, int containerId) {
        View row = activity.findViewById(containerId);
        ((TextView) row.findViewById(com.inter.efficientia_mobile.R.id.counterValue))
                .setText(preferences.getString(key(containerId), "0"));
    }

    void saveCounter(Activity activity, int containerId) {
        View row = activity.findViewById(containerId);
        String value = ((TextView) row.findViewById(com.inter.efficientia_mobile.R.id.counterValue))
                .getText().toString();
        preferences.edit().putString(key(containerId), value).apply();
    }

    private String key(int viewId) { return context.getResources().getResourceEntryName(viewId); }
}
