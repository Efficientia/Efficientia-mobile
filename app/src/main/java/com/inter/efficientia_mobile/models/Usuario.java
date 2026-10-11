package com.inter.efficientia_mobile.models;

import com.google.gson.annotations.SerializedName;

public class Usuario {
    private int id;
    private String nome;
    private String tipo;
    private String cpf;
    private String email;

    @SerializedName("codigoInterno")
    private String codigoInterno;

    private String telefone;
    private Boolean assinaturaFixaCadastrada;
    private String assinaturaFixaId;
    private String urlAssinaturaGeral;
    private String cnhNumero;
    private String categoriaCnh;

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public String getCodigoInterno() { return codigoInterno; }
    public String getTelefone() { return telefone; }
    public boolean isAssinaturaFixaCadastrada() {
        return Boolean.TRUE.equals(assinaturaFixaCadastrada);
    }
    public String getAssinaturaFixaId() { return assinaturaFixaId; }
    public String getUrlAssinaturaGeral() { return urlAssinaturaGeral; }
    public String getCnhNumero() { return cnhNumero; }
    public String getCategoriaCnh() { return categoriaCnh; }
}
