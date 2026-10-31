package com.inter.efficientia_mobile.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface CompanyService {
    @GET("api/v1/empresas/{id}")
    Call<CompanyResponse> getById(@Header("Authorization") String authorization,
                                  @Path("id") long id);

    final class CompanyResponse {
        private String nomeFantasia;
        private String nomeEmpresa;
        private String razaoSocial;

        public String displayName() {
            if (nomeFantasia != null && !nomeFantasia.trim().isEmpty()) return nomeFantasia.trim();
            if (nomeEmpresa != null && !nomeEmpresa.trim().isEmpty()) return nomeEmpresa.trim();
            return razaoSocial == null ? "" : razaoSocial.trim();
        }
    }
}
