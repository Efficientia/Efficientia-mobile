package com.inter.efficientia_mobile.models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class LoginResponseTest {
    @Test
    public void registeredDriverSignatureIsReadFromLoginUser() {
        LoginResponse response = new Gson().fromJson(
                "{\"token\":\"jwt\",\"usuario\":{\"id\":5,\"assinaturaFixaCadastrada\":true}}",
                LoginResponse.class);

        assertTrue(response.getUsuario().isAssinaturaFixaCadastrada());
    }

    @Test
    public void missingDriverSignatureDefaultsToFirstAccess() {
        LoginResponse response = new Gson().fromJson(
                "{\"token\":\"jwt\",\"usuario\":{\"id\":5}}",
                LoginResponse.class);

        assertFalse(response.getUsuario().isAssinaturaFixaCadastrada());
    }
}
