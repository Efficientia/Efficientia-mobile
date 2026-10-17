package com.inter.efficientia_mobile.network;

import com.inter.efficientia_mobile.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Separate service: the AI API is not hosted at the mobile app API base URL. */
public final class CinttiaClient {
    private static CinttiaService service;

    private CinttiaClient() { }

    public static boolean isConfigured() {
        String url = BuildConfig.CINTTIA_BASE_URL.trim();
        return url.startsWith("https://");
    }

    public static synchronized CinttiaService service() {
        if (service == null) {
            String url = BuildConfig.CINTTIA_BASE_URL.trim();
            if (!isConfigured()) throw new IllegalStateException("CinttIA URL not configured");
            service = new Retrofit.Builder()
                    .baseUrl(url.endsWith("/") ? url : url + "/")
                    .client(new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .callTimeout(75, TimeUnit.SECONDS)
                            .build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(CinttiaService.class);
        }
        return service;
    }
}
