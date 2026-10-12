package com.inter.efficientia_mobile.network;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface DriverPhotoService {
    @Multipart
    @PUT("api/v1/usuarios/me/foto")
    Call<Void> save(@Header("Authorization") String authorization,
                    @Part MultipartBody.Part arquivo);

    @GET("api/v1/usuarios/me/foto/conteudo")
    Call<ResponseBody> content(@Header("Authorization") String authorization);

    @DELETE("api/v1/usuarios/me/foto")
    Call<Void> remove(@Header("Authorization") String authorization);
}
