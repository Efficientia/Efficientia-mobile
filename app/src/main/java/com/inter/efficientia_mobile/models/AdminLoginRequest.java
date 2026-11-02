package com.inter.efficientia_mobile.models;

public class AdminLoginRequest {
    private final String cnpj;
    private final String senha;

    public AdminLoginRequest(String cnpj, String senha) {
        this.cnpj = cnpj;
        this.senha = senha;
    }
}
