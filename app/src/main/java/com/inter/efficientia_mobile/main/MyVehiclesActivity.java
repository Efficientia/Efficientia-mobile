package com.inter.efficientia_mobile.main;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Operational totals from the signed-in driver's submitted route reports. */
public class MyVehiclesActivity extends AppCompatActivity {
    private static final int PAGE_SIZE = 50;
    private final List<Integer> reportIds = new ArrayList<>();
    private final Set<Integer> seenIds = new HashSet<>();
    private RouteDiaryService reportsApi;
    private Call<JsonObject> activeCall;
    private int loadGeneration;
    private long trips;
    private long heads;
    private long incidents;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_my_vehicles);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myVehiclesRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.btnVehiclesBack).setOnClickListener(view -> ToolbarNavigation.back(this));
        findViewById(R.id.navVehiclesHome).setOnClickListener(view -> ToolbarNavigation.home(this));
        findViewById(R.id.navVehiclesProfile).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverProfileActivity.class));
        findViewById(R.id.navVehiclesSettings).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverSettingsActivity.class));
        findViewById(R.id.navVehiclesAssistant).setOnClickListener(view ->
                ToolbarNavigation.open(this, CinttiaActivity.class));
        findViewById(R.id.navVehiclesDocuments).setOnClickListener(view ->
                ToolbarNavigation.open(this, DocumentsActivity.class));
        reportsApi = RetrofitClient.getInstance().create(RouteDiaryService.class);
        findViewById(R.id.txtVehiclesTrips).setOnClickListener(view -> loadStats());
        findViewById(R.id.txtVehiclesHeads).setOnClickListener(view -> loadStats());
        findViewById(R.id.txtVehiclesIncidents).setOnClickListener(view -> loadStats());
    }

    @Override protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        String auth = SessionManager.authorizationHeader(this);
        int generation = ++loadGeneration;
        if (activeCall != null) activeCall.cancel();
        reportIds.clear();
        seenIds.clear();
        trips = heads = incidents = 0;
        show("…", "…", "…");
        if (auth == null) {
            showUnavailable();
            return;
        }
        loadPage(auth, 0, generation);
    }

    private void loadPage(String auth, int page, int generation) {
        activeCall = reportsApi.listMine(auth, page, PAGE_SIZE);
        activeCall.enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (generation != loadGeneration || isFinishing() || isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showUnavailable();
                    return;
                }
                JsonArray items = array(response.body(), "itens");
                int added = 0;
                for (JsonElement element : items) {
                    if (!element.isJsonObject()) continue;
                    JsonObject report = element.getAsJsonObject();
                    int id = number(report, "id");
                    int driverId = number(report, "motoristaId");
                    if (id <= 0 || (driverId > 0 && driverId != SessionManager.userId(MyVehiclesActivity.this))
                            || !seenIds.add(id)) continue;
                    reportIds.add(id);
                    added++;
                }
                if (items.size() == PAGE_SIZE && added > 0) loadPage(auth, page + 1, generation);
                else loadReport(auth, 0, generation);
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                if (generation == loadGeneration && !call.isCanceled()) showUnavailable();
            }
        });
    }

    private void loadReport(String auth, int position, int generation) {
        if (generation != loadGeneration || isFinishing() || isDestroyed()) return;
        if (position >= reportIds.size()) {
            NumberFormat format = NumberFormat.getIntegerInstance(Locale.getDefault());
            show(format.format(trips), format.format(heads), format.format(incidents));
            return;
        }
        activeCall = reportsApi.get(auth, reportIds.get(position));
        activeCall.enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (generation != loadGeneration || isFinishing() || isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showUnavailable();
                    return;
                }
                JsonObject report = response.body();
                int driverId = number(report, "motoristaId");
                if (driverId <= 0 || driverId == SessionManager.userId(MyVehiclesActivity.this))
                    accumulate(report);
                loadReport(auth, position + 1, generation);
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                if (generation == loadGeneration && !call.isCanceled()) showUnavailable();
            }
        });
    }

    private void accumulate(JsonObject report) {
        if ("rascunho".equalsIgnoreCase(value(report, "status"))) return;
        trips++;
        int total = number(report, "totalAnimais");
        if (total <= 0) total = number(report, "quantidadeMachos")
                + number(report, "quantidadeFemeas") + number(report, "quantidadeMarrucos");
        heads += total;
        JsonArray boarding = array(report, "anomaliasEmbarque");
        JsonArray unloading = array(report, "anomaliasDesembarque");
        incidents += boarding.size() + unloading.size()
                + array(report, "paradasImprevistas").size();
        if (number(report, "quantidadeEmergencia") > 0) incidents++;
        // The current form also records unloading occurrences in free-text comments.
        if (boarding.size() == 0 && unloading.size() == 0
                && mentionsIncident(value(report, "comentarios"))) incidents++;
    }

    private void show(String tripCount, String headCount, String incidentCount) {
        ((TextView) findViewById(R.id.txtVehiclesTrips)).setText(tripCount);
        ((TextView) findViewById(R.id.txtVehiclesHeads)).setText(headCount);
        ((TextView) findViewById(R.id.txtVehiclesIncidents)).setText(incidentCount);
    }

    private void showUnavailable() { show("—", "—", "—"); }
    private static String value(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }
    private static int number(JsonObject object, String key) {
        try { return Integer.parseInt(value(object, key)); }
        catch (NumberFormatException ignored) { return 0; }
    }
    private static boolean mentionsIncident(String notes) {
        String text = notes.toLowerCase(Locale.ROOT);
        for (String token : new String[]{"sangr", "choque", "abuso", "manc", "pisote",
                "magrez", "gaiola", "porteira", "chute", "emergência", "emergencia"}) {
            if (text.contains(token)) return true;
        }
        return false;
    }
    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray()
                ? object.getAsJsonArray(key) : new JsonArray();
    }

    @Override protected void onDestroy() {
        ++loadGeneration;
        if (activeCall != null) activeCall.cancel();
        super.onDestroy();
    }
}
