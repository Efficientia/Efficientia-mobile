package com.inter.efficientia_mobile.main;

import android.content.Context;
import android.content.SharedPreferences;

import com.inter.efficientia_mobile.R;
import com.inter.efficientia_mobile.auth.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Local, account-scoped activity feed until a notifications API is available. */
public final class DriverNotificationStore {
    private static final String PREFS = "driver_notifications";
    private static final int MAX_ITEMS = 50;

    private DriverNotificationStore() { }

    public static JSONArray list(Context context) {
        String saved = preferences(context).getString(accountKey(context), "[]");
        try {
            return new JSONArray(saved);
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    public static void ensureWelcome(Context context) {
        if (SessionManager.userId(context) <= 0) return;
        SharedPreferences prefs = preferences(context);
        String key = accountKey(context);
        if (prefs.getBoolean(key + "_welcome_added", false)) return;
        JSONArray notifications = list(context);
        long now = System.currentTimeMillis();
        try {
            notifications.put(new JSONObject()
                    .put("type", "welcome")
                    .put("title", context.getString(R.string.notifications_welcome_title))
                    .put("message", context.getString(R.string.notifications_welcome_message))
                    .put("time", now));
            notifications.put(new JSONObject()
                    .put("type", "welcome")
                    .put("title", context.getString(R.string.notifications_diary_title))
                    .put("message", context.getString(R.string.notifications_diary_message))
                    .put("time", now));
            prefs.edit().putString(key, notifications.toString())
                    .putBoolean(key + "_welcome_added", true).apply();
        } catch (JSONException ignored) { }
    }

    public static void reportSent(Context context, String route) {
        add(context, "success", "Diário de rota enviado",
                route.isBlank() ? "Seu diário foi recebido pela API."
                        : "O diário de " + route + " foi recebido pela API.");
    }

    public static void reportFailed(Context context, String message) {
        add(context, "error", "Falha ao enviar diário",
                message == null || message.isBlank() ? "Confira sua conexão e tente novamente."
                        : message);
    }

    /** Entry point for future AI events; call only when a real event is received. */
    public static void assistantMessage(Context context, String title, String message) {
        add(context, "assistant", title, message);
    }

    private static void add(Context context, String type, String title, String message) {
        if (SessionManager.userId(context) <= 0 || title == null || title.isBlank()
                || message == null || message.isBlank()) return;
        JSONArray existing = list(context);
        JSONArray updated = new JSONArray();
        try {
            updated.put(new JSONObject()
                    .put("type", type)
                    .put("title", title)
                    .put("message", message)
                    .put("time", System.currentTimeMillis()));
            for (int i = 0; i < existing.length() && i < MAX_ITEMS - 1; i++)
                updated.put(existing.getJSONObject(i));
            preferences(context).edit().putString(accountKey(context), updated.toString()).apply();
        } catch (JSONException ignored) { }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String accountKey(Context context) {
        return "user_" + SessionManager.userId(context);
    }
}
