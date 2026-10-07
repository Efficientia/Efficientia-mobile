package com.inter.efficientia_mobile.route;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.signature.SignatureImageStore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RouteDiaryStepSixActivity extends AppCompatActivity {
    private RouteDiaryDraft draft;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_route_diary_step_six);
        draft = RouteDiaryDraft.open(this, savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.routeStepSixRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()); view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        findViewById(R.id.btnRouteStepSixBack).setOnClickListener(view -> finish());
        renderSummary();
        findViewById(R.id.btnRouteStepSixSubmit).setOnClickListener(view ->
                startActivity(new Intent(this, RouteDiaryLaunchActivity.class)));
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        draft.saveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    private void renderSummary() {
        RouteSummaryRow.bind(findViewById(R.id.summaryGta), R.string.route_summary_gta,
                joined(draft.get(R.id.inputRouteGta), draft.get(R.id.inputRouteInvoice)));
        RouteSummaryRow.bind(findViewById(R.id.summaryOrigin), R.string.route_summary_origin,
                displayed(draft.get(R.id.inputRouteOrigin)));
        RouteSummaryRow.bind(findViewById(R.id.summaryDestination), R.string.route_summary_destination,
                joined(draft.get(R.id.inputRouteDestination), corral()));

        int males = count(R.id.counterMales), females = count(R.id.counterFemales);
        int marrucos = count(R.id.counterMarrucos);
        RouteSummaryRow.bind(findViewById(R.id.summaryAnimals), R.string.route_summary_animals,
                getString(R.string.route_summary_animals_format, males + females + marrucos,
                        males, females, marrucos));
        RouteSummaryRow.bind(findViewById(R.id.summaryArrival), R.string.route_summary_arrival,
                getString(R.string.route_summary_arrival_format, count(R.id.counterStanding),
                        count(R.id.counterLying), count(R.id.counterDead), count(R.id.counterEmergency)));
        RouteSummaryRow.bind(findViewById(R.id.summaryDuration), R.string.route_summary_duration,
                displayed(duration()));
        RouteSummaryRow.bind(findViewById(R.id.summaryAnomalies), R.string.route_summary_anomalies,
                displayed(joined(draft.get(R.id.inputRouteAnomalyAnimals),
                        draft.get(R.id.inputRouteAnomalyDescription))));

        LinearLayout details = (LinearLayout) findViewById(R.id.summaryAnomalies).getParent();
        addDetail(details, R.string.route_invoice_number, draft.get(R.id.inputRouteInvoice));
        addDetail(details, R.string.route_boarding_date, draft.get(R.id.inputRouteBoardingDate));
        addDetail(details, R.string.route_boarding_time, draft.get(R.id.inputRouteBoardingTime));
        addDetail(details, R.string.route_departure_time, draft.get(R.id.inputRouteDepartureTime));
        addDetail(details, R.string.route_departure_km, distance(R.id.inputRouteDepartureKm));
        addDetail(details, R.string.route_arrival_date, draft.get(R.id.inputRouteArrivalDate));
        addDetail(details, R.string.route_arrival_time, draft.get(R.id.inputRouteArrivalTime));
        addDetail(details, R.string.route_unloading_time, draft.get(R.id.inputRouteUnloadingTime));
        addDetail(details, R.string.route_arrival_km, distance(R.id.inputRouteArrivalKm));
        addDetail(details, R.string.route_reverse_alarm, alarm());
        addDetail(details, R.string.route_stop_reason, draft.get(R.id.inputRouteStopReason));
        addDetail(details, R.string.route_stop_start, draft.get(R.id.inputRouteStopStart));
        addDetail(details, R.string.route_stop_end, draft.get(R.id.inputRouteStopEnd));
        addDetail(details, R.string.route_animals_involved, draft.get(R.id.inputRouteAnomalyAnimals));
        addDetail(details, R.string.route_anomaly_description, draft.get(R.id.inputRouteAnomalyDescription));
        addDetail(details, R.string.route_incident_reason, draft.get(R.id.inputRouteIncidentReason));
        addDetail(details, R.string.route_comments, draft.get(R.id.inputRouteArrivalComments));

        ArrayList<String> signatures = new ArrayList<>();
        if (SignatureImageStore.read(draft.get("signature_PECUARISTA")) != null)
            signatures.add(getString(R.string.route_rancher));
        if (SignatureImageStore.read(SignatureImageStore.driverPath(this)) != null)
            signatures.add(getString(R.string.route_driver));
        if (SignatureImageStore.read(draft.get("signature_MANOBRISTA")) != null)
            signatures.add(getString(R.string.route_maneuverer));
        if (SignatureImageStore.read(draft.get("signature_CURRALEIRO")) != null)
            signatures.add(getString(R.string.route_corral_worker));
        ((TextView) findViewById(R.id.summarySignatureStatus)).setText(
                signatures.isEmpty() ? getString(R.string.route_summary_not_informed)
                        : android.text.TextUtils.join(", ", signatures));
    }

    private void addDetail(LinearLayout parent, int label, String value) {
        if (value.isEmpty()) return;
        View row = LayoutInflater.from(this).inflate(R.layout.view_route_summary_row, parent, false);
        RouteSummaryRow.bind(row, label, value);
        parent.addView(row);
    }

    private String corral() {
        String number = draft.get(R.id.inputRouteCorral);
        return number.isEmpty() ? "" : getString(R.string.route_summary_corral_format, number);
    }

    private String distance(int id) {
        String value = draft.get(id);
        return value.isEmpty() ? "" : value + " " + getString(R.string.route_km_suffix);
    }

    private String alarm() {
        String value = draft.get("reverse_alarm");
        return "yes".equals(value) ? getString(R.string.route_yes)
                : "no".equals(value) ? getString(R.string.route_no) : "";
    }

    private int count(int id) {
        try { return Integer.parseInt(draft.get(id)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String displayed(String value) {
        return value.isEmpty() ? getString(R.string.route_summary_not_informed) : value;
    }

    private String joined(String first, String second) {
        if (first.isEmpty()) return second;
        if (second.isEmpty()) return first;
        return first + " • " + second;
    }

    private String duration() {
        String start = joinedDateTime(R.id.inputRouteBoardingDate, R.id.inputRouteBoardingTime);
        String end = joinedDateTime(R.id.inputRouteArrivalDate, R.id.inputRouteArrivalTime);
        if (start.isEmpty() || end.isEmpty()) return "";
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        format.setLenient(false);
        try {
            Date startDate = format.parse(start), endDate = format.parse(end);
            if (startDate == null || endDate == null) return "";
            long minutes = (endDate.getTime() - startDate.getTime()) / 60000;
            return minutes < 0 ? "" : getString(R.string.route_summary_duration_format,
                    minutes / 60, minutes % 60);
        } catch (ParseException ignored) { return ""; }
    }

    private String joinedDateTime(int dateId, int timeId) {
        String date = draft.get(dateId), time = draft.get(timeId);
        return date.isEmpty() || time.isEmpty() ? "" : date + " " + time;
    }
}
