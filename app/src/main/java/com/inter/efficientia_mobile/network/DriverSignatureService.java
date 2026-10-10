package com.inter.efficientia_mobile.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface DriverSignatureService {
    @Multipart
    @PUT("api/v1/usuarios/me/assinatura")
    Call<ResponseBody> save(@Header("Authorization") String authorization,
                            @Header("Idempotency-Key") String idempotencyKey,
                            @Part MultipartBody.Part arquivo,
                            @Part("metadados") RequestBody metadados);

    @GET("api/v1/usuarios/me/assinatura/conteudo")
    Call<ResponseBody> content(@Header("Authorization") String authorization);
}
