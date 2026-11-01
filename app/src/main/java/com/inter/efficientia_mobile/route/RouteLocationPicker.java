package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;
import com.inter.efficientia_mobile.network.RetrofitClient;
import com.inter.efficientia_mobile.network.RouteDiaryService;
import com.inter.efficientia_mobile.network.RouteLocationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RouteLocationPicker {
    interface Selection { void onSelected(int id, String name); }
    private static final int PAGE_SIZE = 50;
    private RouteLocationPicker() { }

    private static final class Place {
        final int id;
        final String name;
        final String label;
        Place(int id, String name, String label) {
            this.id = id;
            this.name = name;
            this.label = label;
        }
    }

    static void show(Activity activity, boolean farm, Selection selected) {
        String auth = SessionManager.authorizationHeader(activity);
        if (auth == null) return;
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(activity, 18);
        content.setPadding(padding, padding, padding, padding);
        EditText search = new EditText(activity);
        search.setSingleLine(true);
        search.setHint(R.string.location_search_hint);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        content.addView(search);
        Button searchButton = new Button(activity);
        searchButton.setText(R.string.route_search_location);
        content.addView(searchButton);
        TextView state = new TextView(activity);
        content.addView(state);
        ListView list = new ListView(activity);
        content.addView(list, new LinearLayout.LayoutParams(-1, dp(activity, 280)));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(farm ? R.string.location_select_farm : R.string.location_select_facility)
                .setView(content).setNegativeButton(R.string.main_home_delete_cancel, null).create();

        Map<String, Place> history = new LinkedHashMap<>();
        List<Place> apiPlaces = new ArrayList<>();
        String nameField = farm ? "nomeOrigem" : "nomeDestino";
        String idField = farm ? "fazendaId" : "unidadeFrigorificaId";
        int inputId = farm ? R.id.inputRouteOrigin : R.id.inputRouteDestination;
        String draftIdField = farm ? "fazenda_id" : "unidade_frigorifica_id";
        for (RouteDiaryDraft draft : RouteDiaryDraft.saved(activity))
            addHistory(history, draft.get(inputId), parseId(draft.get(draftIdField)));

        Runnable render = () -> {
            String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            Map<String, Place> visible = new LinkedHashMap<>();
            for (Place place : history.values()) {
                if (place.name.toLowerCase(Locale.ROOT).contains(query)) visible.put(key(place.name), place);
            }
            for (Place place : apiPlaces) {
                if (place.name.toLowerCase(Locale.ROOT).contains(query)) visible.put(key(place.name), place);
            }
            List<Place> choices = new ArrayList<>(visible.values());
            List<String> labels = new ArrayList<>();
            for (Place place : choices) labels.add(place.label);
            state.setText(labels.isEmpty() ? R.string.location_none : R.string.location_select_prompt);
            list.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, labels));
            list.setOnItemClickListener((parent, view, position, id) -> {
                Place place = choices.get(position);
                selected.onSelected(place.id, place.name);
                dialog.dismiss();
            });
        };

        RouteDiaryService diaryApi = RetrofitClient.getInstance().create(RouteDiaryService.class);
        loadHistory(diaryApi, auth, activity, dialog, history, nameField, idField, 0, render);
        RouteLocationService locationsApi = RetrofitClient.getInstance().create(RouteLocationService.class);
        final int[] request = {0};
        Runnable load = () -> {
            int current = ++request[0];
            Call<JsonObject> call = farm ? locationsApi.farms(auth, search.getText().toString(), 0, PAGE_SIZE)
                    : locationsApi.facilities(auth, search.getText().toString(), 0, PAGE_SIZE);
            call.enqueue(new Callback<JsonObject>() {
                @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (current != request[0] || !dialog.isShowing()) return;
                    apiPlaces.clear();
                    if (response.isSuccessful() && response.body() != null) {
                        for (JsonElement element : array(response.body(), "itens")) {
                            if (!element.isJsonObject()) continue;
                            JsonObject place = element.getAsJsonObject();
                            String name = value(place, "nome");
                            int id = parseId(value(place, "id"));
                            if (name.isEmpty() || id == 0) continue;
                            String label = name;
                            String city = value(place, "cidade");
                            String state = value(place, "uf");
                            if (!city.isEmpty()) label += " · " + city;
                            if (!state.isEmpty()) label += "/" + state;
                            apiPlaces.add(new Place(id, name, label));
                        }
                    }
                    render.run();
                }
                @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                    if (current == request[0] && dialog.isShowing()) render.run();
                }
            });
        };
        searchButton.setOnClickListener(view -> load.run());
        search.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            load.run();
            return true;
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { render.run(); }
            @Override public void afterTextChanged(Editable text) { }
        });
        dialog.show();
        render.run();
        load.run();
    }

    private static void loadHistory(RouteDiaryService api, String auth, Activity activity,
                                    AlertDialog dialog, Map<String, Place> history,
                                    String nameField, String idField, int page, Runnable render) {
        api.listMine(auth, page, PAGE_SIZE).enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!dialog.isShowing() || activity.isFinishing() || activity.isDestroyed()
                        || !response.isSuccessful() || response.body() == null) return;
                JsonArray items = array(response.body(), "itens");
                for (JsonElement element : items) {
                    if (!element.isJsonObject()) continue;
                    JsonObject report = element.getAsJsonObject();
                    String driver = value(report, "motoristaId");
                    if (!driver.isEmpty() && parseId(driver) != SessionManager.userId(activity)) continue;
                    addHistory(history, value(report, nameField), parseId(value(report, idField)));
                }
                render.run();
                if (items.size() == PAGE_SIZE)
                    loadHistory(api, auth, activity, dialog, history, nameField, idField, page + 1, render);
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable error) { }
        });
    }

    private static void addHistory(Map<String, Place> history, String name, int id) {
        name = name.trim();
        if (name.isEmpty()) return;
        String key = key(name);
        Place previous = history.get(key);
        if (previous == null || (previous.id == 0 && id != 0))
            history.put(key, new Place(id, name, name));
    }
    private static String key(String name) { return name.trim().toLowerCase(Locale.ROOT); }
    private static int parseId(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }
    private static String value(JsonObject object, String field) {
        return object.has(field) && !object.get(field).isJsonNull() ? object.get(field).getAsString() : "";
    }
    private static JsonArray array(JsonObject object, String field) {
        return object.has(field) && object.get(field).isJsonArray()
                ? object.getAsJsonArray(field) : new JsonArray();
    }
    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
