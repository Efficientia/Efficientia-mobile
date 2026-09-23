package com.inter.efficientia_mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.inter.efficientia_mobile.models.LoginResponse;
import com.inter.efficientia_mobile.models.Usuario;

public final class SessionManager {

    private static final String PREFERENCES = "efficientia_session";

    private SessionManager() {
    }

    public static void save(Context context, LoginResponse loginResponse) {
        Usuario usuario = loginResponse.getUsuario();
        SharedPreferences.Editor editor = context
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString("token", loginResponse.getToken())
                .putString("token_type", loginResponse.getTokenType());

        if (usuario != null) {
            editor.putInt("usuario_id", usuario.getId())
                    .putString("usuario_nome", usuario.getNome())
                    .putString("usuario_tipo", usuario.getTipo())
                    .putString("usuario_cpf", usuario.getCpf())
                    .putString("usuario_email", usuario.getEmail())
                    .putString("usuario_codigo_interno", usuario.getCodigoInterno())
                    .putString("usuario_telefone", usuario.getTelefone());
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

    public static void clear(Context context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
