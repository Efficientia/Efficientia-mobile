package com.inter.efficientia_mobile.network;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface RouteDiaryService {
    @GET("api/v1/relatorios-viagem/me")
    Call<JsonObject> listMine(@Header("Authorization") String authorization,
                              @Query("pagina") int page, @Query("tamanho") int size);

    @GET("api/v1/relatorios-viagem/{id}")
    Call<JsonObject> get(@Header("Authorization") String authorization, @Path("id") int id);

    @Streaming
    @GET("api/v1/relatorios-viagem/{id}/assinaturas/{papel}/conteudo")
    Call<ResponseBody> signature(@Header("Authorization") String authorization,
                                 @Path("id") int id, @Path("papel") String role);

    @Streaming
    @GET("api/v1/usuarios/me/assinatura/conteudo")
    Call<ResponseBody> driverSignature(@Header("Authorization") String authorization);

    @POST("api/v1/relatorios-viagem")
    Call<JsonObject> create(@Header("Authorization") String authorization, @Body JsonObject body);

    @PUT("api/v1/relatorios-viagem/{id}")
    Call<JsonObject> update(@Header("Authorization") String authorization, @Path("id") int id,
                            @Header("Idempotency-Key") String idempotencyKey,
                            @Body JsonObject body);

    @Multipart
    @PUT("api/v1/relatorios-viagem/{id}/assinaturas/{papel}")
    Call<JsonObject> uploadSignature(@Header("Authorization") String authorization,
                                     @Path("id") int id, @Path("papel") String role,
                                     @Part MultipartBody.Part file);
}
