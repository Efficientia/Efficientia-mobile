package com.inter.efficientia_mobile.network;

import android.os.Handler;
import android.os.Looper;

import com.inter.efficientia_mobile.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ApiClient {

    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 120_000;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private ApiClient() {
    }

    public interface LoginCallback {
        void onSuccess(JSONObject response);

        void onError(String message);
    }

    public static void login(
            String cpf,
            String email,
            String senha,
            String codigoEmpresa,
            LoginCallback callback
    ) {
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = endpoint("api/v1/auth/login");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);

                JSONObject request = new JSONObject()
                        .put("cpf", cpf)
                        .put("email", email)
                        .put("senha", senha)
                        .put("codigoEmpresa", codigoEmpresa);

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(request.toString().getBytes(StandardCharsets.UTF_8));
                }

                int status = connection.getResponseCode();
                String body = readBody(status >= 200 && status < 300
                        ? connection.getInputStream() : connection.getErrorStream());

                if (status >= 200 && status < 300) {
                    JSONObject response = new JSONObject(body);
                    if (response.optString("token").isBlank()) {
                        MAIN_HANDLER.post(() -> callback.onError(
                                "A API não retornou o token de autenticação."));
                    } else {
                        MAIN_HANDLER.post(() -> callback.onSuccess(response));
                    }
                } else {
                    String message = extractError(body, status);
                    MAIN_HANDLER.post(() -> callback.onError(message));
                }
            } catch (SocketTimeoutException e) {
                MAIN_HANDLER.post(() -> callback.onError(
                        "A API demorou para responder. Tente novamente em alguns instantes."));
            } catch (IOException e) {
                MAIN_HANDLER.post(() -> callback.onError(
                        "Não foi possível conectar à API em " + BuildConfig.API_BASE_URL));
            } catch (JSONException e) {
                MAIN_HANDLER.post(() -> callback.onError("A API retornou uma resposta inválida."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private static URL endpoint(String path) throws IOException {
        String baseUrl = BuildConfig.API_BASE_URL.endsWith("/")
                ? BuildConfig.API_BASE_URL
                : BuildConfig.API_BASE_URL + "/";
        return new URL(baseUrl + path);
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        return body.toString();
    }

    private static String extractError(String body, int status) {
        if (status == 502 || status == 503 || status == 504) {
            return "A API está iniciando ou temporariamente indisponível. Tente novamente.";
        }
        try {
            JSONObject problem = new JSONObject(body);
            String detail = problem.optString("detail");
            if (!detail.isBlank()) return detail;
            String title = problem.optString("title");
            if (!title.isBlank()) return title;
            String message = problem.optString("message");
            if (!message.isBlank()) return message;
        } catch (JSONException ignored) {
            // A resposta pode não ser JSON; nesse caso exibimos uma mensagem genérica.
        }
        return "Falha no login (HTTP " + status + ").";
    }
}
