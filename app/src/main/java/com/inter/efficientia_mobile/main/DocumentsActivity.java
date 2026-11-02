package com.inter.efficientia_mobile.main;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

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
import com.inter.efficientia_mobile.FriendlyHttpError;
import com.inter.efficientia_mobile.TechnicalDetails;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;
import com.inter.efficientia_mobile.route.RouteDiaryDeletion;
import com.inter.efficientia_mobile.route.RouteDiaryEditor;
import com.inter.efficientia_mobile.route.RouteDiaryFlow;
import com.inter.efficientia_mobile.route.RouteDiaryReportIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Complete, driver-scoped report history with the same actions as the home cards. */
public class DocumentsActivity extends AppCompatActivity {
    private static final int PAGE_SIZE = 20;
    private final List<JsonObject> reports = new ArrayList<>();
    private final Set<Integer> loadedIds = new HashSet<>();
    private final Handler reportsHandler = new Handler(Looper.getMainLooper());
    private Call<JsonObject> activeCall;
    private Runnable reportsTimeout;
    private PopupWindow actionsPopup;
    private int loadGeneration;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_documents);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.documentsRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        findViewById(R.id.btnDocumentsTopHome).setOnClickListener(view -> ToolbarNavigation.home(this));
        findViewById(R.id.navDocumentsHome).setOnClickListener(view -> ToolbarNavigation.home(this));
        findViewById(R.id.btnDocumentsNotifications).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverNotificationsActivity.class));
        findViewById(R.id.imgDocumentsProfile).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverProfileActivity.class));
        findViewById(R.id.navDocumentsProfile).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverProfileActivity.class));
        findViewById(R.id.navDocumentsSettings).setOnClickListener(view ->
                ToolbarNavigation.open(this, DriverSettingsActivity.class));
        findViewById(R.id.navDocumentsAssistant).setOnClickListener(view ->
                ToolbarNavigation.open(this, CinttiaActivity.class));
        View.OnClickListener startNew = view -> RouteDiaryFlow.startNew(this);
        findViewById(R.id.btnDocumentsNew).setOnClickListener(startNew);
        findViewById(R.id.btnDocumentsAdd).setOnClickListener(startNew);
    }

    @Override protected void onResume() {
        super.onResume();
        ProfilePhotoStore.show(this, findViewById(R.id.imgDocumentsProfile));
        loadReports();
    }

    private void loadReports() {
        if (reportsTimeout != null) reportsHandler.removeCallbacks(reportsTimeout);
        if (activeCall != null) activeCall.cancel();
        reports.clear();
        loadedIds.clear();
        ((LinearLayout) findViewById(R.id.documentsList)).removeAllViews();
        String auth = SessionManager.authorizationHeader(this);
        if (auth == null) {
            showState(getString(R.string.reports_login_again), false);
            return;
        }
        showState(getString(R.string.reports_consulting), false);
        loadPage(auth, 0, ++loadGeneration);
    }

    private void loadPage(String auth, int page, int generation) {
        Call<JsonObject> listCall = RetrofitClient.getInstance().create(RouteDiaryService.class)
                .listMine(auth, page, PAGE_SIZE);
        activeCall = listCall;
        reportsTimeout = () -> {
            if (generation != loadGeneration || activeCall != listCall) return;
            listCall.cancel();
            loadKnownReports(auth, RouteDiaryReportIndex.ids(DocumentsActivity.this), 0,
                    generation, getString(R.string.reports_retry));
        };
        reportsHandler.postDelayed(reportsTimeout, 8_000L);
        listCall.enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (isFinishing() || isDestroyed() || generation != loadGeneration || call.isCanceled()) return;
                reportsHandler.removeCallbacks(reportsTimeout);
                if (!response.isSuccessful() || response.body() == null) {
                    loadKnownReports(auth, RouteDiaryReportIndex.ids(DocumentsActivity.this), 0,
                            generation, getString(R.string.reports_api_retry));
                    return;
                }
                JsonObject body = response.body();
                JsonArray items = body.has("itens") && body.get("itens").isJsonArray()
                        ? body.getAsJsonArray("itens") : new JsonArray();
                int added = 0;
                for (JsonElement item : items) {
                    if (!item.isJsonObject()) continue;
                    JsonObject report = item.getAsJsonObject();
                    if (!report.has("id") || report.get("id").isJsonNull()
                            || !loadedIds.add(report.get("id").getAsInt())) continue;
                    added++;
                    if (!report.has("motoristaId") || report.get("motoristaId").isJsonNull()
                            || report.get("motoristaId").getAsInt() == SessionManager.userId(DocumentsActivity.this)) {
                        reports.add(report);
                        addCard(report);
                    }
                }
                showState(reports.isEmpty() ? getString(R.string.reports_empty) : "", false);
                if (items.size() == PAGE_SIZE && added > 0) loadPage(auth, page + 1, generation);
            }

            @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                if (isFinishing() || isDestroyed() || generation != loadGeneration || call.isCanceled()) return;
                reportsHandler.removeCallbacks(reportsTimeout);
                loadKnownReports(auth, RouteDiaryReportIndex.ids(DocumentsActivity.this), 0,
                        generation, getString(R.string.reports_offline_retry));
            }
        });
    }

    private void loadKnownReports(String auth, List<Integer> ids, int position,
                                  int generation, String failureMessage) {
        if (generation != loadGeneration || isFinishing() || isDestroyed()) return;
        while (position < ids.size() && loadedIds.contains(ids.get(position))) position++;
        if (position >= ids.size()) {
            showState(reports.isEmpty() ? failureMessage : "", reports.isEmpty());
            return;
        }
        int nextPosition = position + 1;
        activeCall = RetrofitClient.getInstance().create(RouteDiaryService.class)
                .get(auth, ids.get(position));
        activeCall.enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (generation != loadGeneration || isFinishing() || isDestroyed()) return;
                JsonObject report = response.body();
                if (response.isSuccessful() && report != null && report.has("id")
                        && !report.get("id").isJsonNull()
                        && (!report.has("motoristaId") || report.get("motoristaId").isJsonNull()
                        || report.get("motoristaId").getAsInt() == SessionManager.userId(DocumentsActivity.this))
                        && loadedIds.add(report.get("id").getAsInt())) {
                    reports.add(report);
                    addCard(report);
                    showState("", false);
                }
                loadKnownReports(auth, ids, nextPosition, generation, failureMessage);
            }

            @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                if (generation != loadGeneration || isFinishing() || isDestroyed()) return;
                loadKnownReports(auth, ids, nextPosition, generation, failureMessage);
            }
        });
    }

    private void addCard(JsonObject report) {
        LinearLayout list = findViewById(R.id.documentsList);
        View card = LayoutInflater.from(this).inflate(R.layout.item_document_report, list, false);
        ((TextView) card.findViewById(R.id.txtDocumentDate)).setText(reportDate(report));
        ((TextView) card.findViewById(R.id.txtDocumentTitle)).setText(reportTitle(report));
        card.findViewById(R.id.btnDocumentMore).setOnClickListener(anchor -> showActions(anchor, report));
        list.addView(card);
    }

    private void showState(String message, boolean retry) {
        TextView state = findViewById(R.id.txtDocumentsState);
        state.setText(message);
        state.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
        state.setOnClickListener(retry ? view -> loadReports() : null);
    }

    private void showActions(View anchor, JsonObject report) {
        if (actionsPopup != null) actionsPopup.dismiss();
        View content = LayoutInflater.from(this).inflate(R.layout.popup_report_actions, null);
        String title = reportTitle(report);
        ((TextView) content.findViewById(R.id.txtPopupReportDate)).setText(reportDate(report));
        ((TextView) content.findViewById(R.id.txtPopupReportRoute)).setText(title);
        int width = dp(300);
        actionsPopup = new PopupWindow(content, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        actionsPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        actionsPopup.setOutsideTouchable(true);
        actionsPopup.setElevation(dp(12));
        content.findViewById(R.id.actionSaveReportPdf).setOnClickListener(view -> {
            actionsPopup.dismiss();
            withFreshReport(report, fresh -> RouteReportPdf.open(this, fresh,
                    SessionManager.authorizationHeader(this)));
        });
        content.findViewById(R.id.actionEditReport).setOnClickListener(view -> {
            actionsPopup.dismiss();
            withFreshReport(report, fresh -> RouteDiaryEditor.open(this, fresh));
        });
        content.findViewById(R.id.actionDeleteReport).setOnClickListener(view -> {
            actionsPopup.dismiss();
            if (!"rascunho".equalsIgnoreCase(value(report, "status"))) {
                Toast.makeText(this, R.string.reports_delete_only_draft, Toast.LENGTH_LONG).show();
                return;
            }
            if (!report.has("id") || report.get("id").isJsonNull()) {
                Toast.makeText(this, R.string.reports_delete_missing_id, Toast.LENGTH_LONG).show();
                return;
            }
            RouteDiaryDeletion.confirmServerDraft(this, report.get("id").getAsInt(), title,
                    this::loadReports);
        });
        actionsPopup.showAsDropDown(anchor, anchor.getWidth() - width, -dp(8));
    }

    private void withFreshReport(JsonObject listed, Consumer<JsonObject> action) {
        String auth = SessionManager.authorizationHeader(this);
        if (auth == null || !listed.has("id")) {
            Toast.makeText(this, R.string.reports_login_diary, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.reports_loading_diary, Toast.LENGTH_SHORT).show();
        RetrofitClient.getInstance().create(RouteDiaryService.class)
                .get(auth, listed.get("id").getAsInt()).enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (isFinishing() || isDestroyed()) return;
                        JsonObject fresh = response.body();
                        if (response.isSuccessful() && fresh != null
                                && (!fresh.has("motoristaId") || fresh.get("motoristaId").isJsonNull()
                                || fresh.get("motoristaId").getAsInt() == SessionManager.userId(DocumentsActivity.this)))
                            action.accept(fresh);
                        else TechnicalDetails.showError(DocumentsActivity.this,
                                getString(R.string.reports_loading_error) + "\n\n"
                                        + FriendlyHttpError.message(DocumentsActivity.this, response.code()),
                                "Relatório: HTTP " + response.code());
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                        if (!isFinishing() && !isDestroyed()) Toast.makeText(DocumentsActivity.this,
                                R.string.reports_loading_offline, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static String value(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull() ? json.get(field).getAsString() : "";
    }

    private String reportDate(JsonObject report) {
        String date = value(report, "dataEmbarque");
        String time = value(report, "horarioEmbarque");
        if (date.matches("\\d{4}-\\d{2}-\\d{2}"))
            date = date.substring(8, 10) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4);
        if (time.length() >= 5) time = time.substring(0, 5);
        return (time + "  " + date).trim();
    }

    private String reportTitle(JsonObject report) {
        String origin = value(report, "nomeOrigem");
        String destination = value(report, "nomeDestino");
        if (!origin.isEmpty() && !destination.isEmpty()) return origin + " – " + destination;
        String gta = value(report, "numeroGta");
        String status = value(report, "status");
        return (gta.isEmpty() ? getString(R.string.reports_diary_number, value(report, "id")) : gta)
                + (status.isEmpty() ? "" : " · " + status);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        ++loadGeneration;
        if (reportsTimeout != null) reportsHandler.removeCallbacks(reportsTimeout);
        if (activeCall != null) activeCall.cancel();
        if (actionsPopup != null) actionsPopup.dismiss();
        super.onDestroy();
    }
}
