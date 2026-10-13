package com.inter.efficientia_mobile.main;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;
import com.inter.efficientia_mobile.route.RouteDiaryReportIndex;
import com.inter.efficientia_mobile.route.RouteDiaryEditor;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.auth.LoginGeralActivity;
import com.inter.efficientia_mobile.splash.AuthenticatedSplashActivity;
import com.inter.efficientia_mobile.feedback.RouteFeedbackActivity;
import com.inter.efficientia_mobile.route.RouteDiaryStepOneActivity;

public class MainActivity extends AppCompatActivity {

    private PopupWindow reportActionsPopup;
    private final List<JsonObject> recentReports = new ArrayList<>();
    private int reportsLoadGeneration;
    private boolean reportsListUnavailable;
    private static final int[] REPORT_CARDS = {R.id.cardRouteOne, R.id.cardRouteTwo,
            R.id.cardRouteThree, R.id.cardRouteFour};
    private static final int[] REPORT_DATES = {R.id.txtRouteOneDate, R.id.txtRouteTwoDate,
            R.id.txtRouteThreeDate, R.id.txtRouteFourDate};
    private static final int[] REPORT_TITLES = {R.id.txtRouteOneCity, R.id.txtRouteTwoCity,
            R.id.txtRouteThreeCity, R.id.txtRouteFourCity};
    private static final int[] REPORT_ACTIONS = {R.id.btnRouteOneMore, R.id.btnRouteTwoMore,
            R.id.btnRouteThreeMore, R.id.btnRouteFourMore};

    @Override
    protected void onResume() {
        super.onResume();
        if (findViewById(R.id.btnHomeProfile) != null) {
            ProfilePhotoStore.show(this, findViewById(R.id.btnHomeProfile));
            loadRecentReports();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SessionManager.authorizationHeader(this) == null) {
            startActivity(new Intent(this, LoginGeralActivity.class));
            finish();
            return;
        }
        if (!SessionManager.hasVerifiedDriverSignatureThisProcess()) {
            startActivity(new Intent(this, AuthenticatedSplashActivity.class));
            finish();
            return;
        }
        if (!SessionManager.hasRegisteredDriverSignature(this)) {
            startActivity(new Intent(this, FirstAccessActivity.class));
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_main);
        ProfilePhotoRemote.refresh(this, () -> {
            if (!isFinishing() && !isDestroyed()) {
                ProfilePhotoStore.show(this, findViewById(R.id.btnHomeProfile));
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String userName = SessionManager.userName(this);
        if (userName.isBlank()) {
            userName = "Motorista";
        }
        TextView greeting = findViewById(R.id.txtMainHomeGreeting);
        greeting.setText(getString(R.string.main_home_greeting, userName));

        int[] pendingActions = {
                R.id.btnHomeNotifications,
                R.id.navMainDocuments,
                R.id.navMainAssistant,
                R.id.navMainSettings
        };
        for (int actionId : pendingActions) {
            findViewById(actionId).setOnClickListener(this::showPendingFeature);
        }
        View.OnClickListener openProfile = view ->
                startActivity(new Intent(this, DriverProfileActivity.class));
        findViewById(R.id.btnHomeProfile).setOnClickListener(openProfile);
        findViewById(R.id.navMainProfile).setOnClickListener(openProfile);
        findViewById(R.id.btnMainFeedback).setOnClickListener(view ->
                startActivity(new Intent(this, RouteFeedbackActivity.class))
        );
        findViewById(R.id.btnMainTruck).setOnClickListener(view ->
                startActivity(new Intent(this, MyVehiclesActivity.class))
        );
        View.OnClickListener openRouteDiary = view ->
                startActivity(new Intent(this, RouteDiaryStepOneActivity.class));
        findViewById(R.id.btnMainReports).setOnClickListener(openRouteDiary);
        findViewById(R.id.btnMainAdd).setOnClickListener(openRouteDiary);

        for (int i = 0; i < REPORT_ACTIONS.length; i++) {
            final int position = i;
            findViewById(REPORT_ACTIONS[i]).setOnClickListener(anchor -> {
                if (position < recentReports.size()) {
                    JsonObject report = recentReports.get(position);
                    showReportActions(anchor, report);
                }
            });
        }
    }

    private void showPendingFeature(View ignored) {
        Toast.makeText(this, R.string.main_home_feature_pending, Toast.LENGTH_SHORT).show();
    }

    private void loadRecentReports() {
        String auth = SessionManager.authorizationHeader(this);
        if (auth == null) return;
        recentReports.clear();
        reportsListUnavailable = false;
        int generation = ++reportsLoadGeneration;
        for (int id : REPORT_CARDS) findViewById(id).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.txtHomeReportsState)).setText("Carregando relatórios…");
        RouteDiaryService api = RetrofitClient.getInstance().create(RouteDiaryService.class);
        api.listMine(auth, 0, 4).enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (isFinishing() || isDestroyed() || generation != reportsLoadGeneration) return;
                if (!response.isSuccessful() || response.body() == null) {
                    reportsListUnavailable = true;
                    loadKnownReport(auth, RouteDiaryReportIndex.ids(MainActivity.this), 0, generation);
                    return;
                }
                JsonObject body = response.body();
                JsonArray items = body.has("itens") && body.get("itens").isJsonArray()
                        ? body.getAsJsonArray("itens") : new JsonArray();
                for (JsonElement item : items) {
                    if (!item.isJsonObject()) continue;
                    JsonObject report = item.getAsJsonObject();
                    if (report.has("motoristaId") && !report.get("motoristaId").isJsonNull()
                            && report.get("motoristaId").getAsInt() == SessionManager.userId(MainActivity.this))
                        recentReports.add(report);
                }
                renderReports();
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                if (!isFinishing() && !isDestroyed() && generation == reportsLoadGeneration)
                    reportsListUnavailable = true;
                if (!isFinishing() && !isDestroyed() && generation == reportsLoadGeneration)
                    loadKnownReport(auth, RouteDiaryReportIndex.ids(MainActivity.this), 0, generation);
            }
        });
    }

    private void loadKnownReport(String auth, List<Integer> ids, int position, int generation) {
        if (generation != reportsLoadGeneration) return;
        if (position >= ids.size() || recentReports.size() == REPORT_CARDS.length) {
            renderReports();
            return;
        }
        RouteDiaryService api = RetrofitClient.getInstance().create(RouteDiaryService.class);
        api.get(auth, ids.get(position)).enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (isFinishing() || isDestroyed() || generation != reportsLoadGeneration) return;
                JsonObject report = response.body();
                if (response.isSuccessful() && report != null
                        && report.has("motoristaId") && !report.get("motoristaId").isJsonNull()
                        && report.get("motoristaId").getAsInt() == SessionManager.userId(MainActivity.this)) {
                    recentReports.add(report);
                }
                renderReports();
                loadKnownReport(auth, ids, position + 1, generation);
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                if (!isFinishing() && !isDestroyed() && generation == reportsLoadGeneration)
                    showReportState("Sem conexão para carregar os relatórios. Toque para tentar novamente.", true);
            }
        });
    }

    private void renderReports() {
        for (int i = 0; i < REPORT_CARDS.length; i++) {
            boolean visible = i < recentReports.size();
            findViewById(REPORT_CARDS[i]).setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                ((TextView) findViewById(REPORT_DATES[i])).setText(getReportDate(recentReports.get(i)));
                ((TextView) findViewById(REPORT_TITLES[i])).setText(getReportTitle(recentReports.get(i)));
            }
        }
        showReportState(recentReports.isEmpty()
                ? (reportsListUnavailable ? "Não foi possível consultar seus relatórios na API. Toque para tentar novamente."
                : "Nenhum relatório encontrado para sua conta.") : "", reportsListUnavailable);
    }

    private void showReportState(String message, boolean retry) {
        TextView state = findViewById(R.id.txtHomeReportsState);
        state.setText(message);
        state.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
        state.setOnClickListener(retry ? view -> loadRecentReports() : null);
    }

    private static String value(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull() ? json.get(field).getAsString() : "";
    }

    private String getReportDate(JsonObject report) {
        String date = value(report, "dataEmbarque");
        String time = value(report, "horarioEmbarque");
        if (date.matches("\\d{4}-\\d{2}-\\d{2}"))
            date = date.substring(8, 10) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4);
        if (time.length() >= 5) time = time.substring(0, 5);
        return (time + "  " + date).trim();
    }

    private String getReportTitle(JsonObject report) {
        String origin = value(report, "nomeOrigem");
        String destination = value(report, "nomeDestino");
        if (!origin.isEmpty() && !destination.isEmpty()) return origin + " – " + destination;
        String gta = value(report, "numeroGta");
        String status = value(report, "status");
        return (gta.isEmpty() ? "Diário de rota #" + value(report, "id") : gta)
                + (status.isEmpty() ? "" : " · " + status);
    }

    private void showReportActions(View anchor, JsonObject report) {
        if (reportActionsPopup != null) {
            reportActionsPopup.dismiss();
        }

        View content = LayoutInflater.from(this).inflate(R.layout.popup_report_actions, null);
        String route = getReportTitle(report);
        ((TextView) content.findViewById(R.id.txtPopupReportDate)).setText(getReportDate(report));
        ((TextView) content.findViewById(R.id.txtPopupReportRoute)).setText(route);

        int popupWidth = dpToPx(300);
        reportActionsPopup = new PopupWindow(
                content,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        reportActionsPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        reportActionsPopup.setOutsideTouchable(true);
        reportActionsPopup.setElevation(dpToPx(12));

        content.findViewById(R.id.actionSaveReportPdf).setOnClickListener(view -> {
            reportActionsPopup.dismiss();
            withFreshReport(report, fresh -> RouteReportPdf.open(this, fresh,
                    SessionManager.authorizationHeader(this)));
        });
        content.findViewById(R.id.actionEditReport).setOnClickListener(view -> {
            reportActionsPopup.dismiss();
            withFreshReport(report, fresh -> RouteDiaryEditor.open(this, fresh));
        });
        content.findViewById(R.id.actionDeleteReport).setOnClickListener(view -> {
            reportActionsPopup.dismiss();
            Toast.makeText(this, "Exclusão no servidor ainda indisponível.", Toast.LENGTH_SHORT).show();
        });

        int horizontalOffset = anchor.getWidth() - popupWidth;
        reportActionsPopup.showAsDropDown(anchor, horizontalOffset, -dpToPx(8));
    }

    private void withFreshReport(JsonObject listed, java.util.function.Consumer<JsonObject> action) {
        String auth = SessionManager.authorizationHeader(this);
        if (auth == null || !listed.has("id")) {
            Toast.makeText(this, "Entre novamente para consultar o diário.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Carregando diário…", Toast.LENGTH_SHORT).show();
        RetrofitClient.getInstance().create(RouteDiaryService.class)
                .get(auth, listed.get("id").getAsInt()).enqueue(new Callback<JsonObject>() {
                    @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (isFinishing() || isDestroyed()) return;
                        JsonObject fresh = response.body();
                        if (response.isSuccessful() && fresh != null
                                && fresh.has("motoristaId") && !fresh.get("motoristaId").isJsonNull()
                                && fresh.get("motoristaId").getAsInt() == SessionManager.userId(MainActivity.this))
                            action.accept(fresh);
                        else Toast.makeText(MainActivity.this,
                                "Não foi possível carregar este diário (HTTP " + response.code() + ").",
                                Toast.LENGTH_LONG).show();
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                        if (!isFinishing() && !isDestroyed()) Toast.makeText(MainActivity.this,
                                "Sem conexão para carregar este diário.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (reportActionsPopup != null) {
            reportActionsPopup.dismiss();
        }
        super.onDestroy();
    }
}
