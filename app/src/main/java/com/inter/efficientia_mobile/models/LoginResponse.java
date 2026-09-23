package com.inter.efficientia_mobile.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private String token;
    
    @SerializedName("tokenType")
    private String tokenType;
    
    private Usuario usuario;

    public String getToken() { return token; }
    public String getTokenType() { return tokenType != null ? tokenType : "Bearer"; }
    public Usuario getUsuario() { return usuario; }
}
