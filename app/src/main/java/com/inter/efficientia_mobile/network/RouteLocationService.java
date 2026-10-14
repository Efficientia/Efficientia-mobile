package com.inter.efficientia_mobile.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface RouteLocationService {
    @GET("api/v1/locais-diario/fazendas")
    Call<JsonObject> farms(@Header("Authorization") String authorization,
                           @Query("q") String query, @Query("pagina") int page,
                           @Query("tamanho") int size);

    @GET("api/v1/locais-diario/unidades-frigorificas")
    Call<JsonObject> facilities(@Header("Authorization") String authorization,
                                @Query("q") String query, @Query("pagina") int page,
                                @Query("tamanho") int size);
}
