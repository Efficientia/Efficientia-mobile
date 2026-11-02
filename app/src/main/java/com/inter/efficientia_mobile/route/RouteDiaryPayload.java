package com.inter.efficientia_mobile.route;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.inter.efficientia_mobile.R;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
        JsonArray stops = originalArray(draft.get("original_stops"));
        if (hasCompleteStop(draft)) {
            String displayed = draft.get(R.id.inputRouteStopReason) + "|" + draft.get(R.id.inputRouteStopDate)
                    + "|" + draft.get(R.id.inputRouteStopStart) + "|" + draft.get(R.id.inputRouteStopEnd);
            if (stops.size() == 0 || !displayed.equals(draft.get("original_stop_display"))) {
                JsonObject stop = new JsonObject();
                stop.addProperty("motivo", "outro");
                stop.addProperty("descricaoMotivo", draft.get(R.id.inputRouteStopReason));
                String date = isoDate(draft.get(R.id.inputRouteStopDate));
                String start = draft.get(R.id.inputRouteStopStart);
                String end = draft.get(R.id.inputRouteStopEnd);
                stop.addProperty("dataHoraInicio", date + "T" + start + ":00");
                if (end.compareTo(start) < 0) date = LocalDate.parse(date).plusDays(1).toString();
                stop.addProperty("dataHoraFim", date + "T" + end + ":00");
                if (stops.size() == 0) stops.add(stop);
                else stops.set(0, stop);
            }
        } else if (stops.size() > 0) {
            stops.remove(0);
        }
        result.add("paradasImprevistas", stops);
        JsonArray anomalies = originalArray(draft.get("original_anomalies"));
        if (hasCompleteAnomaly(draft)) {
            String displayed = draft.get(R.id.inputRouteAnomalyAnimals) + "|"
                    + draft.get(R.id.inputRouteAnomalyDescription);
            if (anomalies.size() == 0 || !displayed.equals(draft.get("original_anomaly_display"))) {
                JsonObject anomaly = new JsonObject();
                anomaly.addProperty("anomalia", "outro");
                anomaly.addProperty("descricaoOutros", draft.get(R.id.inputRouteAnomalyDescription));
                anomaly.addProperty("quantidadeAnimais", Integer.parseInt(draft.get(R.id.inputRouteAnomalyAnimals)));
                if (anomalies.size() == 0) anomalies.add(anomaly);
                else anomalies.set(0, anomaly);
            }
        } else if (anomalies.size() > 0) {
            anomalies.remove(0);
        }
        result.add("anomaliasEmbarque", anomalies);
        return result;
    }

    private static JsonArray originalArray(String json) {
        try { return json.isEmpty() ? new JsonArray() : new JsonParser().parse(json).getAsJsonArray(); }
        catch (RuntimeException ignored) { return new JsonArray(); }
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

    static String validationError(RouteDiaryDraft draft) {
        String[] timeKeys = {"horarioEmbarque", "horarioSaidaPropriedade", "horarioChegadaUnidade", "horarioDesembarque"};
        int[] timeIds = {R.id.inputRouteBoardingTime, R.id.inputRouteDepartureTime,
                R.id.inputRouteArrivalTime, R.id.inputRouteUnloadingTime};
        String[] timeLabels = {"embarque", "saída da propriedade", "chegada", "desembarque"};
        LocalDate boarding = parseDate(draft.get(R.id.inputRouteBoardingDate));
        LocalDate arrival = parseDate(draft.get(R.id.inputRouteArrivalDate));
        LocalDateTime previous = null;
        LocalDate previousDate = null;
        for (int i = 0; i < timeIds.length; i++) {
            String raw = draft.get(timeIds[i]).trim();
            if (raw.isEmpty()) continue;
            LocalTime time = parseTime(raw);
            if (time == null) return "Confira o horário de " + timeLabels[i] + ". Use o formato HH:mm.";
            LocalDate date = (i < 2 ? boarding : arrival);
            if (date == null) date = previousDate;
            if (date == null) date = LocalDate.of(2000, 1, 1);
            if (previous != null && date.isBefore(previous.toLocalDate())) date = date.plusDays(1);
            LocalDateTime current = LocalDateTime.of(date, time);
            if (previous != null && current.isBefore(previous))
                return "O horário de " + timeLabels[i] + " precisa ser igual ou posterior ao horário anterior.";
            previous = current;
            previousDate = date;
        }

        int loaded = count(draft, R.id.counterMales) + count(draft, R.id.counterFemales)
                + count(draft, R.id.counterMarrucos);
        int standing = count(draft, R.id.counterStanding);
        int lying = count(draft, R.id.counterLying);
        int dead = count(draft, R.id.counterDead);
        int emergency = count(draft, R.id.counterEmergency);
        if (loaded <= 0) return "Informe a quantidade de animais embarcados.";
        if (standing + lying + dead + emergency > loaded)
            return "A quantidade de animais no desembarque não pode ser maior que a quantidade embarcada.";
        if (hasIncompleteOccurrences(draft))
            return "Complete os dados da parada ou da anomalia, ou deixe os campos vazios se não houve ocorrência.";
        return "";
    }

    private static int count(RouteDiaryDraft draft, int id) {
        try { return Math.max(0, Integer.parseInt(draft.get(id))); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || !raw.matches("\\d{2}/\\d{2}/\\d{4}")) return null;
        try { return LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd/MM/uuuu")); }
        catch (DateTimeParseException ignored) { return null; }
    }

    private static LocalTime parseTime(String raw) {
        try { return LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm")); }
        catch (DateTimeParseException ignored) { return null; }
    }

    private static boolean hasCompleteStop(RouteDiaryDraft draft) {
        return !draft.get(R.id.inputRouteStopReason).trim().isEmpty()
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
                    && !draft.get(R.id.inputRouteAnomalyDescription).trim().isEmpty()
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
        if (value != null && !value.trim().isEmpty()) target.addProperty(name, value.trim());
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
