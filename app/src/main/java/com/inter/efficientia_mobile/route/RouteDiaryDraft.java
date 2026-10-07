package com.inter.efficientia_mobile.route;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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
        if (id == null) id = UUID.randomUUID().toString();
        activity.getIntent().putExtra(EXTRA_ID, id);
        return new RouteDiaryDraft(activity, id);
    }

    void saveInstanceState(Bundle state) { state.putString(EXTRA_ID, id); }

    Intent next(Activity activity, Class<? extends Activity> target) {
        return new Intent(activity, target).putExtra(EXTRA_ID, id);
    }

    String get(int viewId) { return get(key(viewId)); }
    String get(String key) { return preferences.getString(key, ""); }
    void put(String key, String value) { preferences.edit().putString(key, value).apply(); }

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
