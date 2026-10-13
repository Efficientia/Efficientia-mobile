package com.inter.efficientia_mobile.route;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.inter.efficientia_mobile.R;

import java.time.LocalDate;

/** Maps the local form to the names and date formats accepted by the route API. */
final class RouteDiaryPayload {
    private RouteDiaryPayload() { }

    static JsonObject from(RouteDiaryDraft draft, String status) {
        JsonObject result = new JsonObject();
        result.addProperty("status", status);
        integer(result, "fazendaId", draft.get("fazenda_id"));
        integer(result, "unidadeFrigorificaId", draft.get("unidade_frigorifica_id"));
        string(result, "nomeOrigem", draft.get(R.id.inputRouteOrigin));
        string(result, "nomeDestino", draft.get(R.id.inputRouteDestination));
        string(result, "numeroGta", draft.get(R.id.inputRouteGta));
        string(result, "numeroNotaFiscal", draft.get(R.id.inputRouteInvoice));
        date(result, "dataEmbarque", draft.get(R.id.inputRouteBoardingDate));
        time(result, "horarioEmbarque", draft.get(R.id.inputRouteBoardingTime));
        time(result, "horarioSaidaPropriedade", draft.get(R.id.inputRouteDepartureTime));
        integer(result, "kmSaidaEmbarcadouro", draft.get(R.id.inputRouteDepartureKm));
        date(result, "dataChegadaUnidade", draft.get(R.id.inputRouteArrivalDate));
        time(result, "horarioChegadaUnidade", draft.get(R.id.inputRouteArrivalTime));
        time(result, "horarioDesembarque", draft.get(R.id.inputRouteUnloadingTime));
        integer(result, "kmChegadaDesembarcadouro", draft.get(R.id.inputRouteArrivalKm));
        string(result, "numeroCurral", draft.get(R.id.inputRouteCorral));
        String alarm = draft.get("reverse_alarm");
        if ("yes".equals(alarm) || "no".equals(alarm))
            result.addProperty("sireneReFuncionou", "yes".equals(alarm));
        integer(result, "quantidadeMachos", draft.get(R.id.counterMales));
        integer(result, "quantidadeFemeas", draft.get(R.id.counterFemales));
        integer(result, "quantidadeMarrucos", draft.get(R.id.counterMarrucos));
        integer(result, "quantidadeEmPe", draft.get(R.id.counterStanding));
        integer(result, "quantidadeDeitado", draft.get(R.id.counterLying));
        integer(result, "quantidadeMorto", draft.get(R.id.counterDead));
        integer(result, "quantidadeEmergencia", draft.get(R.id.counterEmergency));
        string(result, "motivoEmergencia", draft.get(R.id.inputRouteIncidentReason));
        string(result, "comentarios", draft.get(R.id.inputRouteArrivalComments));
        JsonArray stops = new JsonArray();
        if (hasCompleteStop(draft)) {
            JsonObject stop = new JsonObject();
            stop.addProperty("motivo", "outro");
            stop.addProperty("descricaoMotivo", draft.get(R.id.inputRouteStopReason));
            String date = isoDate(draft.get(R.id.inputRouteStopDate));
            String start = draft.get(R.id.inputRouteStopStart);
            String end = draft.get(R.id.inputRouteStopEnd);
            stop.addProperty("dataHoraInicio", date + "T" + start + ":00");
            if (end.compareTo(start) < 0) date = LocalDate.parse(date).plusDays(1).toString();
            stop.addProperty("dataHoraFim", date + "T" + end + ":00");
            stops.add(stop);
        }
        result.add("paradasImprevistas", stops);
        JsonArray anomalies = new JsonArray();
        if (hasCompleteAnomaly(draft)) {
            JsonObject anomaly = new JsonObject();
            anomaly.addProperty("anomalia", "outro");
            anomaly.addProperty("descricaoOutros", draft.get(R.id.inputRouteAnomalyDescription));
            anomaly.addProperty("quantidadeAnimais", Integer.parseInt(draft.get(R.id.inputRouteAnomalyAnimals)));
            anomalies.add(anomaly);
        }
        result.add("anomaliasEmbarque", anomalies);
        return result;
    }

    static boolean hasIncompleteOccurrences(RouteDiaryDraft draft) {
        boolean anyStop = !draft.get(R.id.inputRouteStopReason).isEmpty()
                || !draft.get(R.id.inputRouteStopDate).isEmpty()
                || !draft.get(R.id.inputRouteStopStart).isEmpty()
                || !draft.get(R.id.inputRouteStopEnd).isEmpty();
        boolean anyAnomaly = !draft.get(R.id.inputRouteAnomalyAnimals).isEmpty()
                || !draft.get(R.id.inputRouteAnomalyDescription).isEmpty();
        return (anyStop && !hasCompleteStop(draft)) || (anyAnomaly && !hasCompleteAnomaly(draft));
    }

    private static boolean hasCompleteStop(RouteDiaryDraft draft) {
        return !draft.get(R.id.inputRouteStopReason).isBlank()
                && draft.get(R.id.inputRouteStopReason).length() <= 255
                && !isoDate(draft.get(R.id.inputRouteStopDate)).isEmpty()
                && draft.get(R.id.inputRouteStopStart).matches("\\d{2}:\\d{2}")
                && draft.get(R.id.inputRouteStopEnd).matches("\\d{2}:\\d{2}");
    }

    private static boolean hasCompleteAnomaly(RouteDiaryDraft draft) {
        String count = draft.get(R.id.inputRouteAnomalyAnimals);
        if (!count.matches("\\d+")) return false;
        try {
            return Integer.parseInt(count) > 0
                    && !draft.get(R.id.inputRouteAnomalyDescription).isBlank()
                    && draft.get(R.id.inputRouteAnomalyDescription).length() <= 150;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String isoDate(String value) {
        if (value == null || !value.matches("\\d{2}/\\d{2}/\\d{4}")) return "";
        String iso = value.substring(6) + "-" + value.substring(3, 5) + "-" + value.substring(0, 2);
        try { LocalDate.parse(iso); return iso; }
        catch (java.time.format.DateTimeParseException ignored) { return ""; }
    }

    private static void string(JsonObject target, String name, String value) {
        if (value != null && !value.isBlank()) target.addProperty(name, value.trim());
    }

    private static void integer(JsonObject target, String name, String value) {
        if (value == null || !value.trim().matches("\\d+")) return;
        try { target.addProperty(name, Integer.parseInt(value.trim())); }
        catch (NumberFormatException ignored) { }
    }

    private static void date(JsonObject target, String name, String value) {
        if (value != null && value.matches("\\d{2}/\\d{2}/\\d{4}"))
            target.addProperty(name, value.substring(6) + "-" + value.substring(3, 5) + "-" + value.substring(0, 2));
    }

    private static void time(JsonObject target, String name, String value) {
        if (value != null && value.matches("\\d{2}:\\d{2}"))
            target.addProperty(name, value + ":00");
    }
}
