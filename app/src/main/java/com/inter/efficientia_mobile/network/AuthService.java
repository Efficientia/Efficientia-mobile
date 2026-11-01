package com.inter.efficientia_mobile.network;

import com.inter.efficientia_mobile.models.LoginRequest;
import com.inter.efficientia_mobile.models.LoginResponse;
import com.inter.efficientia_mobile.models.AdminLoginRequest;
import com.inter.efficientia_mobile.models.AdminLoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {

    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/v1/auth/admin/login")
    Call<AdminLoginResponse> loginAdmin(@Body AdminLoginRequest request);
}
