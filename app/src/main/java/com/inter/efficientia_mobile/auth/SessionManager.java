package com.inter.efficientia_mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.inter.efficientia_mobile.models.LoginResponse;
import com.inter.efficientia_mobile.models.Usuario;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

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
                    .putString("usuario_cnh_numero", usuario.getCnhNumero())
                    .putString("usuario_cnh_categoria", usuario.getCategoriaCnh())
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

    public static String userCpf(Context context) { return userField(context, "usuario_cpf"); }
    public static String userEmail(Context context) { return userField(context, "usuario_email"); }
    public static String userPhone(Context context) { return userField(context, "usuario_telefone"); }
    public static String userCnhNumber(Context context) { return userField(context, "usuario_cnh_numero"); }
    public static String userCnhCategory(Context context) { return userField(context, "usuario_cnh_categoria"); }

    public static long companyId(Context context) {
        String token = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString("token", "");
        if (token == null) return -1;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return -1;
        try {
            byte[] payload = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return new JSONObject(new String(payload, StandardCharsets.UTF_8))
                    .optLong("empresa_id", -1);
        } catch (IllegalArgumentException | org.json.JSONException ignored) {
            return -1;
        }
    }

    private static String userField(Context context, String key) {
        String value = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(key, "");
        return value == null ? "" : value.trim();
    }

    public static boolean hasRegisteredDriverSignature(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean("assinatura_fixa_cadastrada", false);
    }

    public static boolean hasUnexpiredToken(Context context) {
        String token = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString("token", "");
        if (token == null) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;
        try {
            byte[] payload = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            long expiresAt = new JSONObject(new String(payload, StandardCharsets.UTF_8)).optLong("exp", 0);
            return expiresAt > System.currentTimeMillis() / 1000 + 60;
        } catch (IllegalArgumentException | org.json.JSONException ignored) {
            return false;
        }
    }

    public static boolean hasVerifiedDriverSignatureThisProcess() {
        return driverSignatureVerifiedThisProcess;
    }

    public static void markDriverSignatureRegistered(Context context) {
        driverSignatureVerifiedThisProcess = true;
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean("assinatura_fixa_cadastrada", true).apply();
    }

    public static void markDriverSignatureVerifiedOffline() {
        driverSignatureVerifiedThisProcess = true;
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
        context.getSharedPreferences("cinttia_chat", Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}
