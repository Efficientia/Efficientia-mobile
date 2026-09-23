package com.inter.efficientia_mobile.network;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    
    @POST("api/v1/auth/login")
    Call<JsonObject> login(@Body JsonObject request);
}
