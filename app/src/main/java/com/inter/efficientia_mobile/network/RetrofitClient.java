package com.inter.efficientia_mobile.network;

import com.inter.efficientia_mobile.BuildConfig;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;

    private RetrofitClient() {
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            String baseUrl = BuildConfig.API_BASE_URL;
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
