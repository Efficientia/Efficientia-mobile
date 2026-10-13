package com.inter.efficientia_mobile.route;

import android.content.Context;
import android.content.SharedPreferences;

import com.inter.efficientia_mobile.auth.SessionManager;

import java.util.ArrayList;
import java.util.List;

/** IDs created on this device; the API still needs a driver-scoped listing endpoint. */
public final class RouteDiaryReportIndex {
    private RouteDiaryReportIndex() { }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("route_diary_report_index", Context.MODE_PRIVATE);
    }

    private static String key(Context context) {
        return "driver_" + SessionManager.userId(context);
    }

    public static void remember(Context context, int id) {
        if (id <= 0 || SessionManager.userId(context) <= 0) return;
        List<Integer> ids = ids(context);
        ids.remove(Integer.valueOf(id));
        ids.add(0, id);
        StringBuilder value = new StringBuilder();
        for (int entry : ids) {
            if (value.length() > 0) value.append(',');
            value.append(entry);
        }
        preferences(context).edit().putString(key(context), value.toString()).apply();
    }

    public static List<Integer> ids(Context context) {
        List<Integer> result = new ArrayList<>();
        String raw = preferences(context).getString(key(context), "");
        if (raw == null || raw.isEmpty()) return result;
        for (String part : raw.split(",")) {
            try {
                int id = Integer.parseInt(part);
                if (id > 0 && !result.contains(id)) result.add(id);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }
}
