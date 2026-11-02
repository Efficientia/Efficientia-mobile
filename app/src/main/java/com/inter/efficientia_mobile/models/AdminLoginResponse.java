package com.inter.efficientia_mobile.models;

public class AdminLoginResponse {
    private String token;
    private String tokenType;
    private String proximoPasso;
    private String mensagem;
    private Admin admin;
    private Company empresa;

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public String getProximoPasso() { return proximoPasso; }
    public String getMensagem() { return mensagem; }
    public Admin getAdmin() { return admin; }
    public Company getEmpresa() { return empresa; }

    public static class Admin {
        private String nome;
        private String email;
        private String telefone;
        private String cargo;
        public String getNome() { return nome; }
        public String getEmail() { return email; }
        public String getTelefone() { return telefone; }
        public String getCargo() { return cargo; }
    }

    public static class Company {
        private String nomeEmpresa;
        public String getNomeEmpresa() { return nomeEmpresa; }
    }
}
