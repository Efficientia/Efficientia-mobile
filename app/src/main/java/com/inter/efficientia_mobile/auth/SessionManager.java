package com.inter.efficientia_mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public final class SessionManager {

    private static final String PREFERENCES = "efficientia_session";

    private SessionManager() {
    }

    public static void save(Context context, JSONObject loginResponse) {
        JSONObject usuario = loginResponse.optJSONObject("usuario");
        SharedPreferences.Editor editor = context
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString("token", loginResponse.optString("token"))
                .putString("token_type", loginResponse.optString("tokenType", "Bearer"));

        if (usuario != null) {
            editor.putInt("usuario_id", usuario.optInt("id"))
                    .putString("usuario_nome", usuario.optString("nome"))
                    .putString("usuario_tipo", usuario.optString("tipo"))
                    .putString("usuario_cpf", usuario.optString("cpf"))
                    .putString("usuario_email", usuario.optString("email"))
                    .putString("usuario_codigo_interno", usuario.optString("codigoInterno"))
                    .putString("usuario_telefone", usuario.optString("telefone"));
        }
        editor.apply();
    }

    public static String authorizationHeader(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCES, Context.MODE_PRIVATE);
        String token = preferences.getString("token", "");
        if (token == null || token.isBlank()) return null;
        String tokenType = preferences.getString("token_type", "Bearer");
        return (tokenType == null || tokenType.isBlank() ? "Bearer" : tokenType) + " " + token;
    }

    public static String userName(Context context) {
        String name = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString("usuario_nome", "");
        return name == null ? "" : name.trim();
    }

    public static String firstName(Context context) {
        String name = userName(context);
        if (name.isBlank()) return "Motorista";
        int separator = name.indexOf(' ');
        return separator > 0 ? name.substring(0, separator) : name;
    }

    public static void saveDevelopmentSession(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .putString("usuario_nome", "Motorista Teste")
                .putString("usuario_tipo", "motorista")
                .apply();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
