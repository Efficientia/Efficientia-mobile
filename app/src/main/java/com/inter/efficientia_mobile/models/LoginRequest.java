package com.inter.efficientia_mobile.models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    private String cpf;
    private String email;
    private String senha;
    
    @SerializedName("codigoEmpresa")
    private String codigoEmpresa;

    public LoginRequest(String cpf, String email, String senha, String codigoEmpresa) {
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.codigoEmpresa = codigoEmpresa;
    }

    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getCodigoEmpresa() { return codigoEmpresa; }
}
