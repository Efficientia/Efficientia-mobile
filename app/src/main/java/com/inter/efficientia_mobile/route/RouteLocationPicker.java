package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import com.inter.efficientia_mobile.network.RouteLocationService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

final class RouteLocationPicker {
    interface Selection { void onSelected(int id, String name); }

    private RouteLocationPicker() { }

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
        state.setText(R.string.profile_loading);
        content.addView(state);
        ListView list = new ListView(activity);
        content.addView(list, new LinearLayout.LayoutParams(-1, dp(activity, 280)));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(farm ? R.string.location_select_farm : R.string.location_select_facility)
                .setView(content).setNegativeButton(R.string.main_home_delete_cancel, null).create();
        RouteLocationService api = RetrofitClient.getInstance().create(RouteLocationService.class);
        final int[] request = {0};
        Runnable load = () -> {
            int current = ++request[0];
            state.setText(R.string.location_searching);
            Call<JsonObject> call = farm ? api.farms(auth, search.getText().toString(), 0, 50)
                    : api.facilities(auth, search.getText().toString(), 0, 50);
            call.enqueue(new Callback<JsonObject>() {
                @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (current != request[0] || !dialog.isShowing()) return;
                    if (!response.isSuccessful() || response.body() == null) {
                        state.setText(activity.getString(R.string.location_lookup_error, response.code()));
                        return;
                    }
                    JsonArray items = response.body().getAsJsonArray("itens");
                    List<JsonObject> locations = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    if (items != null) for (JsonElement element : items) {
                        if (!element.isJsonObject()) continue;
                        JsonObject place = element.getAsJsonObject();
                        if (!place.has("id") || !place.has("nome")) continue;
                        locations.add(place);
                        String label = place.get("nome").getAsString();
                        if (place.has("cidade") && !place.get("cidade").isJsonNull())
                            label += " · " + place.get("cidade").getAsString();
                        if (place.has("uf") && !place.get("uf").isJsonNull())
                            label += "/" + place.get("uf").getAsString();
                        labels.add(label);
                    }
                    state.setText(labels.isEmpty() ? R.string.location_none : R.string.location_select_prompt);
                    list.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, labels));
                    list.setOnItemClickListener((parent, view, position, id) -> {
                        JsonObject place = locations.get(position);
                        selected.onSelected(place.get("id").getAsInt(), place.get("nome").getAsString());
                        dialog.dismiss();
                    });
                }
                @Override public void onFailure(Call<JsonObject> call, Throwable error) {
                    if (current == request[0] && dialog.isShowing())
                        state.setText(R.string.location_offline);
                }
            });
        };
        searchButton.setOnClickListener(view -> load.run());
        search.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            load.run();
            return true;
        });
        dialog.show();
        load.run();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
