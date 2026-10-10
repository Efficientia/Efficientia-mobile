package com.inter.efficientia_mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.inter.efficientia_mobile.models.LoginResponse;
import com.inter.efficientia_mobile.models.Usuario;

public final class SessionManager {

    private static final String PREFERENCES = "efficientia_session";
    private static boolean driverSignatureVerifiedThisProcess;

    private SessionManager() {
    }

    public static void save(Context context, LoginResponse loginResponse) {
        driverSignatureVerifiedThisProcess = false;
        Usuario usuario = loginResponse.getUsuario();
        SharedPreferences.Editor editor = context
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .putString("token", loginResponse.getToken())
                .putString("token_type", loginResponse.getTokenType());

        if (usuario != null) {
            editor.putInt("usuario_id", usuario.getId())
                    .putString("usuario_nome", usuario.getNome())
                    .putString("usuario_tipo", usuario.getTipo())
                    .putString("usuario_cpf", usuario.getCpf())
                    .putString("usuario_email", usuario.getEmail())
                    .putString("usuario_codigo_interno", usuario.getCodigoInterno())
                    .putString("usuario_telefone", usuario.getTelefone())
                    .putBoolean("assinatura_fixa_cadastrada", usuario.isAssinaturaFixaCadastrada());
        } else {
            editor.putBoolean("assinatura_fixa_cadastrada", false);
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

    public static int userId(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getInt("usuario_id", -1);
    }

    public static boolean hasRegisteredDriverSignature(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean("assinatura_fixa_cadastrada", false);
    }

    public static boolean hasVerifiedDriverSignatureThisProcess() {
        return driverSignatureVerifiedThisProcess;
    }

    public static void markDriverSignatureRegistered(Context context) {
        driverSignatureVerifiedThisProcess = true;
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean("assinatura_fixa_cadastrada", true).apply();
    }

    public static void markDriverSignatureMissing(Context context) {
        driverSignatureVerifiedThisProcess = false;
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean("assinatura_fixa_cadastrada", false).apply();
    }

    public static String firstName(Context context) {
        String name = userName(context);
        if (name.isBlank()) return "Motorista";
        int separator = name.indexOf(' ');
        return separator > 0 ? name.substring(0, separator) : name;
    }

    public static void clear(Context context) {
        driverSignatureVerifiedThisProcess = false;
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
